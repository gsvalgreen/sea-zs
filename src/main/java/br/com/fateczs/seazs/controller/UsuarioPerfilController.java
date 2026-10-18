package br.com.fateczs.seazs.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.fateczs.seazs.model.Usuario;
import br.com.fateczs.seazs.model.dto.PerfilUsuarioRequest;
import br.com.fateczs.seazs.model.dto.UsuarioResumoResponse;
import br.com.fateczs.seazs.security.Autoridades;
import br.com.fateczs.seazs.service.UsuarioService;
import jakarta.validation.Valid;

/** Promocao/rebaixamento de usuarios por um administrador (o cadastro publico e o /usuario/alterar nunca mudam o perfil). */
@RestController
@RequestMapping("/api/v1/usuarios")
public class UsuarioPerfilController {

	@Autowired
	private UsuarioService usuarioService;

	/** O novo perfil entra no token no proximo login/refresh do usuario; o JWT ja emitido vale ate expirar. */
	@PutMapping("/{id}/perfil")
	@PreAuthorize("hasRole('ADMIN')")
	public UsuarioResumoResponse alterarPerfil(@PathVariable("id") Integer id,
			@Valid @RequestBody PerfilUsuarioRequest requisicao, Authentication autenticacao) {
		Usuario usuario = usuarioService.alterarPerfil(id, requisicao.tipoUsuario(), requisicao.adm(),
				Integer.valueOf(autenticacao.getName()));
		return new UsuarioResumoResponse(usuario.getId(), usuario.getNome(), usuario.getTipoUsuario().name(),
				Autoridades.de(usuario));
	}
}
