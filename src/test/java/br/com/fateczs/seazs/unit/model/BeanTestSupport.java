package br.com.fateczs.seazs.unit.model;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Date;

/**
 * Apoio para testes de getters/setters de classes de modelo: invoca cada setter
 * publico com um valor de amostra e confere se o getter correspondente o devolve.
 */
final class BeanTestSupport {

	private BeanTestSupport() {
	}

	static Object amostra(Class<?> tipo) throws Exception {
		if (tipo == Integer.class || tipo == int.class) return 7;
		if (tipo == Long.class || tipo == long.class) return 7L;
		if (tipo == String.class) return "x";
		if (tipo == Date.class) return new Date(0);
		if (tipo == Boolean.class || tipo == boolean.class) return Boolean.TRUE;
		if (tipo == Character.class || tipo == char.class) return 'A';
		if (tipo == java.util.List.class) return Collections.emptyList();
		if (tipo.isEnum()) return tipo.getEnumConstants()[0];
		return instanciar(tipo);
	}

	static <T> T instanciar(Class<T> classe) throws Exception {
		try {
			return classe.getDeclaredConstructor().newInstance();
		} catch (NoSuchMethodException e) {
			Constructor<?> c = classe.getConstructors()[0];
			Object[] args = new Object[c.getParameterTypes().length];
			for (int i = 0; i < args.length; i++) args[i] = amostra(c.getParameterTypes()[i]);
			return classe.cast(c.newInstance(args));
		}
	}

	/** @return quantidade de pares setter/getter verificados */
	static int verificaGettersESetters(Class<?> classe) throws Exception {
		Object bean = instanciar(classe);
		int verificados = 0;
		for (Method set : classe.getMethods()) {
			if (!set.getName().startsWith("set") || set.getParameterTypes().length != 1) continue;
			String sufixo = set.getName().substring(3);
			Method get = null;
			try {
				get = classe.getMethod("get" + sufixo);
			} catch (NoSuchMethodException e) {
				try {
					get = classe.getMethod("is" + sufixo);
				} catch (NoSuchMethodException e2) {
					continue;
				}
			}
			Object valor = amostra(set.getParameterTypes()[0]);
			set.invoke(bean, valor);
			assertEquals(classe.getSimpleName() + "." + set.getName(), valor, get.invoke(bean));
			verificados++;
		}
		return verificados;
	}
}
