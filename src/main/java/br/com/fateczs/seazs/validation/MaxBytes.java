package br.com.fateczs.seazs.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * Limita o tamanho de uma String em BYTES UTF-8 (nao em caracteres). Necessario para senhas: o BCrypt
 * so considera os 72 primeiros bytes e o Spring Security 7 rejeita entradas maiores.
 */
@Documented
@Constraint(validatedBy = MaxBytesValidator.class)
@Target({ ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT })
@Retention(RetentionPolicy.RUNTIME)
public @interface MaxBytes {

	/** Limite do BCrypt. */
	int SENHA_BCRYPT = 72;

	int value();

	String message() default "deve ter no máximo {value} bytes (caracteres acentuados ocupam mais de 1 byte)";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};
}
