package br.com.fateczs.seazs.integration.support;

import java.security.Principal;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Endpoint /api/v1 de teste (ainda nao ha rotas de negocio em /api/v1) para exercitar a exigencia de autenticacao. */
@RestController
@RequestMapping("/api/v1/teste")
public class ProtegidoTesteController {

	@GetMapping("/quem-sou-eu")
	public String quemSouEu(Principal principal) {
		return principal.getName();
	}
}
