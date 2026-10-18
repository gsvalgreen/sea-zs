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

import br.com.fateczs.seazs.model.Disciplina;
import br.com.fateczs.seazs.service.DisciplinaService;

@RestController
@RequestMapping("/disciplina")
public class DisciplinaController {
	
	@PreAuthorize("hasAnyRole('ADMIN', 'FUNCIONARIO')")
	@PostMapping("/inserir")
	@ResponseBody
	public void inserirDisciplina(@Valid @RequestBody Disciplina disciplina) {
		disciplinaServ.inserir(disciplina);
	}
	
	@PreAuthorize("hasAnyRole('ADMIN', 'FUNCIONARIO')")
	@PostMapping("/inserirLote")
	@ResponseBody
	public void inserirLoteDisciplina(@Valid @RequestBody List<Disciplina> disciplinas) {
		disciplinaServ.inserirLote(disciplinas);
	}
	
	@PreAuthorize("isAuthenticated()")
	@PostMapping("/buscar")
	@ResponseBody
	public Disciplina buscarDisciplina(@Valid @RequestBody Disciplina disciplina) {
		return disciplinaServ.buscar(disciplina);
	}
	
	@PreAuthorize("hasAnyRole('ADMIN', 'FUNCIONARIO')")
	@PostMapping("/alterar")
	@ResponseBody
	public void alterarDisciplina(@Valid @RequestBody Disciplina disciplina) {
		disciplinaServ.alterar(disciplina);
	}
	
	@PreAuthorize("hasAnyRole('ADMIN', 'FUNCIONARIO')")
	@PostMapping("/excluir")
	@ResponseBody
	public void excluirDisciplina(@Valid @RequestBody Disciplina disciplina) {
		disciplinaServ.excluir(disciplina);
	}
	
	@PreAuthorize("isAuthenticated()")
	@PostMapping("/listar")
	@ResponseBody
	public List<Disciplina> listarDisciplina() {
		return disciplinaServ.listar();
	}
	
	@Autowired
	private DisciplinaService disciplinaServ;
}
