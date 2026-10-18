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

import br.com.fateczs.seazs.model.Curso;
import br.com.fateczs.seazs.service.CursoService;

@RestController
@RequestMapping("/curso")
public class CursoController {

	@PreAuthorize("hasAnyRole('ADMIN', 'FUNCIONARIO')")
	@PostMapping("/inserir")
	@ResponseBody
	public void inserirCurso(@Valid @RequestBody Curso curso) {
		cursoServ.inserir(curso);
	}
	
	@PreAuthorize("isAuthenticated()")
	@PostMapping("/buscar")
	@ResponseBody
	public Curso buscarCurso(@Valid @RequestBody Curso curso) {
		return cursoServ.buscar(curso);
	}
	
	@PreAuthorize("hasAnyRole('ADMIN', 'FUNCIONARIO')")
	@PostMapping("/alterar")
	@ResponseBody
	public void alterarCurso(@Valid @RequestBody Curso curso) {
		cursoServ.alterar(curso);
	}
	
	@PreAuthorize("hasAnyRole('ADMIN', 'FUNCIONARIO')")
	@PostMapping("/excluir")
	@ResponseBody
	public void excluirCurso(@Valid @RequestBody Curso curso) {
		cursoServ.excluir(curso);
	}
	
	@PreAuthorize("isAuthenticated()")
	@PostMapping("/listar")
	@ResponseBody
	public List<Curso> listarCurso() {
		return cursoServ.listar();
	}
	
	@Autowired
	private CursoService cursoServ;
}
