package br.com.fateczs.seazs.controller;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import br.com.fateczs.seazs.model.Usuario;
import br.com.fateczs.seazs.model.UsuarioDisciplina;
import br.com.fateczs.seazs.service.UsuarioDisciplinaService;

@RestController
@RequestMapping("/usuario/disciplina")
public class UsuarioDisciplinaController {
	
	@PreAuthorize("hasAnyRole('ADMIN', 'FUNCIONARIO')")
	@PostMapping("/inserir")
	@ResponseBody
	public void inserirUsuarioDisciplina(@Valid @RequestBody UsuarioDisciplina usuarioDisciplina) {
		usuarioDisciplinaServ.inserir(usuarioDisciplina);
	}
	
	@PreAuthorize("hasRole('ADMIN')")
	@PostMapping("/buscar")
	@ResponseBody
	public UsuarioDisciplina buscarUsuarioDisciplina(@Valid @RequestBody UsuarioDisciplina usuarioDisciplina) {
		return usuarioDisciplinaServ.buscar(usuarioDisciplina);
	}
	
	@PreAuthorize("hasAnyRole('ADMIN', 'FUNCIONARIO')")
	@PostMapping("/alterar")
	@ResponseBody
	public void alterarUsuarioDisciplina(@Valid @RequestBody UsuarioDisciplina usuarioDisciplina) {
		usuarioDisciplinaServ.alterar(usuarioDisciplina);
	}
	
	@PreAuthorize("hasAnyRole('ADMIN', 'FUNCIONARIO')")
	@PostMapping("/excluir")
	@ResponseBody
	public void excluirUsuarioDisciplina(@Valid @RequestBody UsuarioDisciplina usuarioDisciplina) {
		usuarioDisciplinaServ.excluir(usuarioDisciplina);
	}
	
	@PreAuthorize("hasRole('ADMIN')")
	@PostMapping("/listar")
	@ResponseBody
	public List<UsuarioDisciplina> listarUsuarioDisciplina() {
		return usuarioDisciplinaServ.listar();
	}
	
	@PreAuthorize("@autorizacao.ehDonoOuAdmin(#usuario.id)")
	@PostMapping("/DisciplinasUsuario")
	@ResponseBody
	public List<UsuarioDisciplina> listarDisciplinasUsuario(@RequestBody Usuario usuario){
		return usuarioDisciplinaServ.recuperarDisciplinasDoUsuario(usuario);
	}
	
	@Autowired
	private UsuarioDisciplinaService usuarioDisciplinaServ;


}
