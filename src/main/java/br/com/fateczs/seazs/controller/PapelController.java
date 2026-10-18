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

import br.com.fateczs.seazs.model.Papel;
import br.com.fateczs.seazs.service.PapelService;

@RestController
@RequestMapping("/papel")
public class PapelController {
	
	@PreAuthorize("hasAnyRole('ADMIN', 'FUNCIONARIO')")
	@PostMapping("/inserir")
	@ResponseBody
	public void inserirPapel(@Valid @RequestBody Papel papel) {
		papelServ.inserir(papel);
	}
	
	@PreAuthorize("isAuthenticated()")
	@PostMapping("/buscar")
	@ResponseBody
	public Papel buscarPapel(@Valid @RequestBody Papel papel) {
		return papelServ.buscar(papel);
	}
	
	@PreAuthorize("hasAnyRole('ADMIN', 'FUNCIONARIO')")
	@PostMapping("/alterar")
	@ResponseBody
	public void alterarPapel(@Valid @RequestBody Papel papel) {
		papelServ.alterar(papel);
	}
	
	@PreAuthorize("hasAnyRole('ADMIN', 'FUNCIONARIO')")
	@PostMapping("/excluir")
	@ResponseBody
	public void excluirPapel(@Valid @RequestBody Papel papel) {
		papelServ.excluir(papel);
	}
	
	@PreAuthorize("isAuthenticated()")
	@PostMapping("/listar")
	@ResponseBody
	public List<Papel> listarPapel() {
		return papelServ.listar();
	}
	
	@Autowired
	private PapelService papelServ;
	
}
