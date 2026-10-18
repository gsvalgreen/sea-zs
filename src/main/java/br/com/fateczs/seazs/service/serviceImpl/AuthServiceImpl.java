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

	@Override
	@Transactional
	public TokenResponse login(LoginRequest requisicao) {
		Usuario usuario = autenticar(requisicao.email(), requisicao.senha());
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
	@Transactional
	public Optional<LoginLegadoResponse> loginLegado(String email, String senha) {
		if (email == null || senha == null || senha.getBytes(StandardCharsets.UTF_8).length > MaxBytes.SENHA_BCRYPT) {
			return Optional.empty();
		}
		try {
			Usuario usuario = autenticar(email, senha);
			return Optional.of(new LoginLegadoResponse(usuario, tokenService.emitir(usuario).valor()));
		} catch (BadCredentialsException e) {
			return Optional.empty();
		}
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
