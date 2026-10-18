package br.com.fateczs.seazs.service.serviceImpl;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.fateczs.seazs.model.RefreshToken;
import br.com.fateczs.seazs.model.Usuario;
import br.com.fateczs.seazs.model.dto.LoginLegadoResponse;
import br.com.fateczs.seazs.model.dto.LoginRequest;
import br.com.fateczs.seazs.model.dto.TokenResponse;
import br.com.fateczs.seazs.model.dto.UsuarioResumoResponse;
import br.com.fateczs.seazs.repository.RefreshTokenRepository;
import br.com.fateczs.seazs.repository.UsuarioRepository;
import br.com.fateczs.seazs.security.Autoridades;
import br.com.fateczs.seazs.security.BloqueioLogin;
import br.com.fateczs.seazs.security.JwtProperties;
import br.com.fateczs.seazs.security.JwtTokenService;
import br.com.fateczs.seazs.security.RefreshTokens;
import br.com.fateczs.seazs.security.UsuarioAutenticado;
import br.com.fateczs.seazs.service.AuthService;
import br.com.fateczs.seazs.validation.MaxBytes;

@Service
public class AuthServiceImpl implements AuthService {

	@Autowired
	private AuthenticationManager authenticationManager;
	@Autowired
	private JwtTokenService tokenService;
	@Autowired
	private JwtProperties props;
	@Autowired
	private RefreshTokenRepository refreshTokens;
	@Autowired
	private UsuarioRepository usuarios;
	@Autowired
	private Clock clock;
	@Autowired
	private BloqueioLogin bloqueio;

	// Sem @Transactional em login/loginLegado: o loadUserByUsername (readOnly) marcaria a transacao externa como
	// rollback-only ao lancar UsernameNotFoundException (e-mail inexistente), e o commit viraria
	// UnexpectedRollbackException (500). Cada passo (leitura do usuario, rehash, refresh token) ja tem transacao propria.
	@Override
	public TokenResponse login(LoginRequest requisicao) {
		Usuario usuario = autenticarComBloqueio(requisicao.email(), requisicao.senha());
		return emitir(usuario, UUID.randomUUID().toString());
	}

	@Override
	@Transactional
	public Optional<TokenResponse> refresh(String refreshToken) {
		Instant agora = clock.instant();
		Optional<RefreshToken> encontrado = refreshTokens.findByTokenHash(RefreshTokens.hash(refreshToken));
		if (!encontrado.isPresent()) {
			return Optional.empty();
		}
		RefreshToken atual = encontrado.get();
		if (atual.estaRevogado()) {
			// Reuso de token ja rotacionado/revogado: possivel roubo. Derruba a cadeia inteira (sem lancar excecao, para
			// o commit da revogacao acontecer).
			refreshTokens.revogarFamilia(atual.getFamilia(), agora);
			return Optional.empty();
		}
		if (atual.estaExpirado(agora)) {
			return Optional.empty();
		}
		Optional<Usuario> usuario = usuarios.findById(atual.getIdUsuario());
		if (!usuario.isPresent()) {
			return Optional.empty();
		}
		atual.setRevogadoEm(agora);
		refreshTokens.save(atual);
		return Optional.of(emitir(usuario.get(), atual.getFamilia()));
	}

	@Override
	@Transactional
	public void logout(String refreshToken) {
		refreshTokens.findByTokenHash(RefreshTokens.hash(refreshToken))
				.ifPresent(t -> refreshTokens.revogarFamilia(t.getFamilia(), clock.instant()));
	}

	@Override
	public Optional<LoginLegadoResponse> loginLegado(String email, String senha) {
		if (email == null || senha == null || senha.trim().isEmpty() || senha.getBytes(StandardCharsets.UTF_8).length > MaxBytes.SENHA_BCRYPT) {
			return Optional.empty();
		}
		try {
			Usuario usuario = autenticarComBloqueio(email, senha);
			return Optional.of(new LoginLegadoResponse(usuario, tokenService.emitir(usuario).valor()));
		} catch (BadCredentialsException e) {
			return Optional.empty();
		}
	}

	/**
	 * Aplica o bloqueio temporario por e-mail ao redor da autenticacao: a tentativa e reservada de forma atomica antes
	 * de verificar a senha (bloqueado, nem verifica: 429); sem sucesso a reserva fica contada como falha; sucesso
	 * zera. O e-mail e contado como foi digitado (existindo ou nao), entao a resposta nao revela se ele existe.
	 */
	private Usuario autenticarComBloqueio(String email, String senha) {
		bloqueio.reservarTentativa(email);
		Usuario usuario = autenticar(email, senha);
		bloqueio.registrarSucesso(email);
		return usuario;
	}

	/** Valida e-mail/senha (rehash BCrypt de senha legada incluso) e devolve o usuario. */
	private Usuario autenticar(String email, String senha) {
		Authentication resultado = authenticationManager
				.authenticate(UsernamePasswordAuthenticationToken.unauthenticated(email, senha));
		return ((UsuarioAutenticado) resultado.getPrincipal()).getUsuario();
	}

	private TokenResponse emitir(Usuario usuario, String familia) {
		Instant agora = clock.instant();
		String opaco = RefreshTokens.gerar();
		refreshTokens.save(new RefreshToken(RefreshTokens.hash(opaco), familia, usuario.getId(), agora,
				agora.plus(props.refreshTtl())));
		JwtTokenService.AccessToken acesso = tokenService.emitir(usuario);
		UsuarioResumoResponse resumo = new UsuarioResumoResponse(usuario.getId(), usuario.getNome(),
				usuario.getTipoUsuario() == null ? null : usuario.getTipoUsuario().name(), Autoridades.de(usuario));
		return new TokenResponse(TokenResponse.BEARER, acesso.valor(), acesso.expiraEmSegundos(), opaco, resumo);
	}
}
