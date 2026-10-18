package br.com.fateczs.seazs.featureTest;

import org.junit.jupiter.api.Test;

import br.com.fateczs.seazs.featureTest.support.FeatureTestBase;

/**
 * Substitui o antigo SeazsApplicationTests.contextLoads, que exigia um MySQL manual em localhost:3306.
 * Agora o MySQL vem do Testcontainers (e o teste e pulado sem Docker).
 */
public class ContextLoadsFeatureTest extends FeatureTestBase {

	@Test
	public void contextLoads() {
	}
}
