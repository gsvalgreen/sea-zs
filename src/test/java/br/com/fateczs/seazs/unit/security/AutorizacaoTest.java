package br.com.fateczs.seazs.unit.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.util.ReflectionTestUtils;

import br.com.fateczs.seazs.repository.InscricaoRepository;
import br.com.fateczs.seazs.repository.StaffAgendamentoRepository;
import br.com.fateczs.seazs.security.Autorizacao;

@ExtendWith(MockitoExtension.class)
public class AutorizacaoTest {

	@Mock
	private InscricaoRepository inscricoes;
	@Mock
	private StaffAgendamentoRepository staffs;

	private Autorizacao autorizacao() {
		Autorizacao a = new Autorizacao();
		ReflectionTestUtils.setField(a, "inscricoes", inscricoes);
		ReflectionTestUtils.setField(a, "staffs", staffs);
		return a;
	}

	@AfterEach
	public void limpar() {
		SecurityContextHolder.clearContext();
	}

	private static void autenticarComo(String sub, String... papeis) {
		Jwt jwt = Jwt.withTokenValue("t").header("alg", "HS256").subject(sub).issuedAt(Instant.now())
				.expiresAt(Instant.now().plusSeconds(60)).build();
		SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt,
				java.util.Arrays.stream(papeis).map(SimpleGrantedAuthority::new).collect(java.util.stream.Collectors.toList())));
	}

	@Test
	public void semAutenticacao_tudoEhNegado() {
		Autorizacao a = autorizacao();

		assertFalse(a.idAutenticado().isPresent());
		assertFalse(a.ehAdmin());
		assertFalse(a.ehDono(1));
		assertFalse(a.ehDonoOuAdmin(1));
		assertFalse(a.ehDonoDaInscricaoOuAdmin(1));
		assertFalse(a.ehDonoDoStaffOuAdmin(1));
		verifyNoInteractions(inscricoes, staffs);
	}

	@Test
	public void autenticacaoAnonimaNaoContaComoUsuario() {
		SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken("k", "anonymousUser",
				AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));

		assertFalse(autorizacao().idAutenticado().isPresent());
		assertFalse(autorizacao().ehAdmin());
	}

	@Test
	public void subNaoNumericoNaoIdentificaNinguem() {
		autenticarComo("abc", "ROLE_ALUNO");

		assertFalse(autorizacao().idAutenticado().isPresent());
		assertFalse(autorizacao().ehDono(1));
	}

	@Test
	public void donoEhQuemTemOSubIgualAoId() {
		autenticarComo("7", "ROLE_ALUNO");
		Autorizacao a = autorizacao();

		assertEquals(Optional.of(7), a.idAutenticado());
		assertTrue(a.ehDono(7));
		assertTrue(a.ehDonoOuAdmin(7));
		assertFalse(a.ehDono(8));
		assertFalse(a.ehDonoOuAdmin(8));
	}

	@Test
	public void idNuloNaoEhDeNinguemMasAdminPassa() {
		autenticarComo("7", "ROLE_ALUNO");
		assertFalse(autorizacao().ehDonoOuAdmin(null));

		autenticarComo("1", "ROLE_FUNCIONARIO", "ROLE_ADMIN");
		assertTrue(autorizacao().ehDonoOuAdmin(null));
	}

	@Test
	public void adminPassaEmTodasAsChecagensSemConsultarOBanco() {
		autenticarComo("1", "ROLE_ALUNO", "ROLE_ADMIN");
		Autorizacao a = autorizacao();

		assertTrue(a.ehAdmin());
		assertTrue(a.ehDonoOuAdmin(99));
		assertTrue(a.ehDonoDaInscricaoOuAdmin(5));
		assertTrue(a.ehDonoDoStaffOuAdmin(5));
		verifyNoInteractions(inscricoes, staffs);
	}

	@Test
	public void tipoFuncionarioOuProfessorSozinhoNaoEhAdmin() {
		autenticarComo("1", "ROLE_FUNCIONARIO", "ROLE_PROFESSOR");

		assertFalse(autorizacao().ehAdmin());
	}

	@Test
	public void inscricaoEhDoDonoGravadoNoBancoENaoDoQueVemNoCorpo() {
		autenticarComo("7", "ROLE_ALUNO");
		when(inscricoes.findIdUsuarioBySequencial(10)).thenReturn(Optional.of(7));
		when(inscricoes.findIdUsuarioBySequencial(11)).thenReturn(Optional.of(8));
		when(inscricoes.findIdUsuarioBySequencial(12)).thenReturn(Optional.empty());
		Autorizacao a = autorizacao();

		assertTrue(a.ehDonoDaInscricaoOuAdmin(10));
		assertFalse(a.ehDonoDaInscricaoOuAdmin(11));
		assertFalse(a.ehDonoDaInscricaoOuAdmin(12)); // inexistente: mesma negativa de alheio
		assertFalse(a.ehDonoDaInscricaoOuAdmin(null));
	}

	@Test
	public void staffEhDoDonoGravadoNoBanco() {
		autenticarComo("7", "ROLE_ALUNO");
		when(staffs.findIdUsuarioBySequencial(1)).thenReturn(Optional.of(7));
		when(staffs.findIdUsuarioBySequencial(2)).thenReturn(Optional.of(9));
		Autorizacao a = autorizacao();

		assertTrue(a.ehDonoDoStaffOuAdmin(1));
		assertFalse(a.ehDonoDoStaffOuAdmin(2));
		assertFalse(a.ehDonoDoStaffOuAdmin(null));
	}

	@Test
	public void autenticacaoNaoAutenticadaEhIgnorada() {
		TestingAuthenticationToken t = new TestingAuthenticationToken("7", "x", List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
		t.setAuthenticated(false);
		SecurityContextHolder.getContext().setAuthentication(t);

		assertFalse(autorizacao().ehAdmin());
		assertFalse(autorizacao().idAutenticado().isPresent());
	}
}
