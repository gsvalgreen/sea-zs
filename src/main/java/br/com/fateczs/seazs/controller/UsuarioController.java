package br.com.fateczs.seazs.controller;

import java.util.List;

import jakarta.validation.Valid;

import br.com.fateczs.seazs.model.CrachaUsuario;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import br.com.fateczs.seazs.model.Usuario;
import br.com.fateczs.seazs.model.dto.LoginLegadoResponse;
import br.com.fateczs.seazs.service.AuthService;
import br.com.fateczs.seazs.service.UsuarioService;

@RestController
@RequestMapping("/usuario")
public class UsuarioController {
	
	@PostMapping("/inserir")
	@ResponseBody
	public void inserirUsuario(@Valid @RequestBody Usuario usuario) {
		usuServ.inserir(usuario);
	}
	
	@PostMapping("/buscar")
	@ResponseBody
	public Usuario buscarUsuario(@Valid @RequestBody Usuario usuario) {
		return usuServ.buscar(usuario);
	}
	
	@PostMapping("/alterar")
	@ResponseBody
	public void alterarUsuario(@Valid @RequestBody Usuario usuario) {
		usuServ.alterar(usuario);
	}
	
	@PostMapping("/excluir")
	@ResponseBody
	public void excluirUsuario(@Valid @RequestBody Usuario usuario) {
		usuServ.excluir(usuario);
	}
	
	@PostMapping("/listar")
	@ResponseBody
	public List<Usuario> listarUsuario() {
		return usuServ.listar();
	}
	
	/**
	 * Login legado, mantido temporariamente para o front atual: mesmo JSON de antes (sem a senha) mais o campo
	 * {@code token} (access token). Credenciais invalidas seguem respondendo 200 com corpo vazio.
	 *
	 * @deprecated use {@code POST /api/v1/auth/login} (401 padronizado, refresh token e DTOs proprios).
	 */
	@Deprecated
	@PostMapping("/login")
	@ResponseBody
	public LoginLegadoResponse loginUsuario(@RequestBody Usuario usuario){
		return authService.loginLegado(usuario.getEmail(), usuario.getSenha()).orElse(null);
	}

	@PostMapping("/cracha")
	@ResponseBody
	public CrachaUsuario emitirCracha(@RequestBody Usuario usuario) {return usuServ.emitirCracha(usuario);}

	@Autowired
	private UsuarioService usuServ;

	@Autowired
	private AuthService authService;
}
