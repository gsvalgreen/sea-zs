package br.com.fateczs.seazs.validation;

import java.nio.charset.StandardCharsets;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class MaxBytesValidator implements ConstraintValidator<MaxBytes, CharSequence> {

	private int max;

	@Override
	public void initialize(MaxBytes anotacao) {
		this.max = anotacao.value();
	}

	/** Nulo e valido (use @NotNull/@NotEmpty para exigir valor). */
	@Override
	public boolean isValid(CharSequence valor, ConstraintValidatorContext contexto) {
		return valor == null || valor.toString().getBytes(StandardCharsets.UTF_8).length <= max;
	}
}
