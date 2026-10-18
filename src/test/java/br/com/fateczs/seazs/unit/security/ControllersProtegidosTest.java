package br.com.fateczs.seazs.unit.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Rede de seguranca da matriz de autorizacao: todo handler de controller precisa declarar {@code @PreAuthorize}
 * (mesmo que {@code permitAll()}). Um endpoint novo sem a anotacao fica autenticado pelo default-deny do filtro, mas
 * sem regra de perfil/posse; este teste obriga a decidir.
 */
public class ControllersProtegidosTest {

	private static List<Class<?>> controllers() throws ClassNotFoundException {
		ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
		scanner.addIncludeFilter(new AnnotationTypeFilter(RestController.class));
		List<Class<?>> classes = new ArrayList<>();
		for (BeanDefinition bd : scanner.findCandidateComponents("br.com.fateczs.seazs.controller")) {
			classes.add(Class.forName(bd.getBeanClassName()));
		}
		return classes;
	}

	private static boolean ehHandler(Method m) {
		return AnnotatedElementUtils.hasAnnotation(m, RequestMapping.class)
				|| m.isAnnotationPresent(GetMapping.class) || m.isAnnotationPresent(PostMapping.class)
				|| m.isAnnotationPresent(PutMapping.class) || m.isAnnotationPresent(DeleteMapping.class)
				|| m.isAnnotationPresent(PatchMapping.class);
	}

	@Test
	public void todoHandlerDeControllerDeclaraPreAuthorize() throws ClassNotFoundException {
		List<Class<?>> controllers = controllers();
		assertFalse(controllers.isEmpty());
		List<String> semRegra = new ArrayList<>();
		int handlers = 0;
		for (Class<?> c : controllers) {
			for (Method m : c.getDeclaredMethods()) {
				if (ehHandler(m)) {
					handlers++;
					if (!m.isAnnotationPresent(PreAuthorize.class) && !c.isAnnotationPresent(PreAuthorize.class)) {
						semRegra.add(c.getSimpleName() + "." + m.getName());
					}
				}
			}
		}
		assertTrue(handlers > 50, "esperava varrer os handlers dos controllers, achou " + handlers);
		assertTrue(semRegra.isEmpty(), "handlers sem @PreAuthorize: " + semRegra);
	}
}
