package br.com.fateczs.seazs.unit.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import br.com.fateczs.seazs.model.Usuario;
import br.com.fateczs.seazs.utils.TipoUsuario;
import br.com.fateczs.seazs.validation.DadosCadastrais;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.groups.Default;

/** Campos NOT NULL de Usuario informados pelo cliente sao validados no grupo DadosCadastrais (cadastro/alteracao). */
public class UsuarioDadosCadastraisTest {

	private static ValidatorFactory fabrica;
	private static Validator validador;

	@BeforeAll
	public static void iniciar() {
		fabrica = Validation.buildDefaultValidatorFactory();
		validador = fabrica.getValidator();
	}

	@AfterAll
	public static void encerrar() {
		fabrica.close();
	}

	private static Usuario completo() {
		Usuario u = new Usuario();
		u.setEmail("a@teste.com");
		u.setSenha("Segredo123");
		u.setNome("Ana");
		u.setEndereco("Rua A, 1");
		u.setCelular("11999999999");
		u.setDataNasc(new java.util.Date(0));
		u.setRg("123");
		u.setGenero("F");
		u.setAdm(false);
		u.setTipoUsuario(TipoUsuario.ALUNO);
		return u;
	}

	private static Set<String> campos(Set<ConstraintViolation<Usuario>> violacoes) {
		return violacoes.stream().map(v -> v.getPropertyPath().toString()).collect(Collectors.toSet());
	}

	@Test
	public void usuarioCompleto_ehValido() {
		assertTrue(validador.validate(completo(), Default.class, DadosCadastrais.class).isEmpty());
	}

	@Test
	public void cadastroSemOsCamposObrigatorios_apontaCadaUm() {
		Usuario u = new Usuario();
		u.setEmail("a@teste.com");
		u.setSenha("Segredo123");

		assertEquals(Set.of("nome", "endereco", "celular", "dataNasc", "rg", "genero"),
				campos(validador.validate(u, Default.class, DadosCadastrais.class)));
	}

	@Test
	public void camposEmBranco_sendoTextoSaoRecusados() {
		Usuario u = completo();
		u.setNome("   ");
		u.setRg("");

		assertEquals(Set.of("nome", "rg"), campos(validador.validate(u, Default.class, DadosCadastrais.class)));
	}

	@Test
	public void admETipoUsuarioNaoSaoExigidosDoCliente_poisOServidorDefine() {
		Usuario u = completo();
		u.setAdm(null);
		u.setTipoUsuario(null);

		assertTrue(validador.validate(u, Default.class, DadosCadastrais.class).isEmpty());
	}

	@Test
	public void foraDoGrupoCadastral_soEmailESenhaSaoExigidos() {
		Usuario u = new Usuario();
		u.setEmail("a@teste.com");
		u.setSenha("Segredo123");

		assertTrue(validador.validate(u).isEmpty());
	}
}
