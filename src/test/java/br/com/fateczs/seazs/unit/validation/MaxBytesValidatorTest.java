package br.com.fateczs.seazs.unit.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.Test;

import br.com.fateczs.seazs.model.dto.LoginRequest;
import br.com.fateczs.seazs.model.Usuario;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

/** Limite de 72 bytes do BCrypt: conta BYTES UTF-8, nao caracteres. */
public class MaxBytesValidatorTest {

	private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

	private Set<ConstraintViolation<LoginRequest>> validar(String senha) {
		return validator.validate(new LoginRequest("a@b.com", senha));
	}

	@Test
	public void aceitaAte72BytesAscii() {
		assertTrue(validar("a".repeat(72)).isEmpty());
	}

	@Test
	public void rejeita73BytesComMensagemClara() {
		Set<ConstraintViolation<LoginRequest>> v = validar("a".repeat(73));
		assertEquals(1, v.size());
		assertTrue(v.iterator().next().getMessage().contains("72 bytes"));
	}

	@Test
	public void contaBytesUtf8EntaoAcentoValeDois() {
		assertTrue(validar("é".repeat(36)).isEmpty()); // 72 bytes
		assertFalse(validar("é".repeat(37)).isEmpty()); // 74 bytes, apenas 37 caracteres
	}

	@Test
	public void senhaVaziaOuEmailEmBrancoSaoInvalidos() {
		assertFalse(validar("").isEmpty());
		assertFalse(validar(null).isEmpty());
		assertFalse(validator.validate(new LoginRequest(" ", "x")).isEmpty());
	}

	@Test
	public void loginRequestNaoImprimeASenha() {
		assertFalse(new LoginRequest("a@b.com", "Segredo123").toString().contains("Segredo123"));
	}

	@Test
	public void cadastroDeUsuarioTambemLimitaASenha() {
		Usuario u = new Usuario();
		u.setEmail("a@b.com");
		u.setSenha("a".repeat(73));
		assertFalse(validator.validateProperty(u, "senha").isEmpty());
		u.setSenha("a".repeat(72));
		assertTrue(validator.validateProperty(u, "senha").isEmpty());
	}
}
