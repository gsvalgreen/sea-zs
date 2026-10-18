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

	/** Forca um 500 com mensagem parecida com a de um erro de SQL, para conferir que nada disso vaza na resposta. */
	@GetMapping("/erro-interno")
	public String erroInterno() {
		throw new IllegalStateException(
				"could not execute statement [select rt1_0.id from tb_refresh_token rt1_0 for update of rt1_0] segredo-interno");
	}
}
