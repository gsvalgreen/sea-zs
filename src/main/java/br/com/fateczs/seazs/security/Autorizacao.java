package br.com.fateczs.seazs.security;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import br.com.fateczs.seazs.repository.InscricaoRepository;
import br.com.fateczs.seazs.repository.StaffAgendamentoRepository;

/**
 * Regras de posse usadas nos {@code @PreAuthorize} dos controllers ({@code @autorizacao.ehDonoOuAdmin(#usuario.id)}).
 * O usuario autenticado e o {@code sub} do JWT (id do usuario); o que vier no corpo da requisicao nunca define quem
 * esta chamando. Na duvida (sem autenticacao, sub invalido, id nulo, registro inexistente) a resposta e {@code false}:
 * negar e o padrao, e registro inexistente e indistinguivel de registro alheio (403 nos dois casos).
 *
 * Os perfis (ROLE_ADMIN, ROLE_ALUNO...) vem do token e valem ate ele expirar (veja o README).
 */
@Component("autorizacao")
public class Autorizacao {

	@Autowired
	private InscricaoRepository inscricoes;

	@Autowired
	private StaffAgendamentoRepository staffs;

	/** Id do usuario autenticado (claim {@code sub}), se houver autenticacao real (nao anonima) com id valido. */
	public Optional<Integer> idAutenticado() {
		Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
		if (autenticacao == null || !autenticacao.isAuthenticated() || autenticacao instanceof AnonymousAuthenticationToken) {
			return Optional.empty();
		}
		try {
			return Optional.of(Integer.valueOf(autenticacao.getName()));
		} catch (NumberFormatException e) {
			return Optional.empty();
		}
	}

	public boolean ehAdmin() {
		Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
		if (autenticacao == null || !autenticacao.isAuthenticated() || autenticacao instanceof AnonymousAuthenticationToken) {
			return false;
		}
		for (GrantedAuthority autoridade : autenticacao.getAuthorities()) {
			if (Autoridades.ADMIN.equals(autoridade.getAuthority())) {
				return true;
			}
		}
		return false;
	}

	/** O usuario autenticado e o dono do id informado. */
	public boolean ehDono(Integer idUsuario) {
		return idUsuario != null && idAutenticado().filter(idUsuario::equals).isPresent();
	}

	public boolean ehDonoOuAdmin(Integer idUsuario) {
		return ehAdmin() || ehDono(idUsuario);
	}

	/** Dono da inscricao gravada (o dono vem do banco, nao do corpo) ou administrador. */
	public boolean ehDonoDaInscricaoOuAdmin(Integer sequencialInscricao) {
		if (ehAdmin()) {
			return true;
		}
		return sequencialInscricao != null && idAutenticado().isPresent()
				&& inscricoes.findIdUsuarioBySequencial(sequencialInscricao).map(this::ehDono).orElse(false);
	}

	/** Dono do registro de staff gravado (o dono vem do banco, nao do corpo) ou administrador. */
	public boolean ehDonoDoStaffOuAdmin(Integer sequencialStaff) {
		if (ehAdmin()) {
			return true;
		}
		return sequencialStaff != null && idAutenticado().isPresent()
				&& staffs.findIdUsuarioBySequencial(sequencialStaff).map(this::ehDono).orElse(false);
	}
}
