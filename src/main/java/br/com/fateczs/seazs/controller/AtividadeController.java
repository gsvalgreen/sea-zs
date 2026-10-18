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

import br.com.fateczs.seazs.model.Agendamento;
import br.com.fateczs.seazs.model.Atividade;
import br.com.fateczs.seazs.model.Usuario;
import br.com.fateczs.seazs.service.AtividadeService;

@RestController
@RequestMapping("/atividade")
public class AtividadeController {
	
	@PreAuthorize("hasAnyRole('ADMIN', 'FUNCIONARIO') or (hasRole('PROFESSOR') and #atividade.id == null)")
	@PostMapping("/inserir")
	@ResponseBody
	public void inserirAtividade(@Valid @RequestBody Atividade atividade) {
		atividadeServ.inserir(atividade);
	}
	
	@PreAuthorize("isAuthenticated()")
	@PostMapping("/buscar")
	@ResponseBody
	public Atividade buscarAtividade(@Valid @RequestBody Atividade atividade) {
		return atividadeServ.buscar(atividade);
	}
	
	@PreAuthorize("hasAnyRole('ADMIN', 'FUNCIONARIO')")
	@PostMapping("/alterar")
	@ResponseBody
	public void alterarAtividade(@Valid @RequestBody Atividade atividade) {
		atividadeServ.alterar(atividade);
	}
	
	@PreAuthorize("hasAnyRole('ADMIN', 'FUNCIONARIO')")
	@PostMapping("/excluir")
	@ResponseBody
	public void excluirAtividade(@Valid @RequestBody Atividade atividade) {
		atividadeServ.excluir(atividade);
	}
	
	@PreAuthorize("isAuthenticated()")
	@PostMapping("/listar")
	@ResponseBody
	public List<Atividade> listarAtividade() {
		return atividadeServ.listar();
	}
	
	@PreAuthorize("isAuthenticated()")
	@PostMapping("/listarData")
	@ResponseBody
	public List<Atividade> listarAtividadePorDtInicio() {
		return atividadeServ.listarPorDataAsc();
	}
	
	@PreAuthorize("isAuthenticated()")
	@PostMapping("/listarAgendamento")
	@ResponseBody
	public List<Atividade> listarAtividadePorAgendamento(@RequestBody Agendamento agendamento) {
		return atividadeServ.listarPorAgendamento(agendamento);
	}
	
	@PreAuthorize("@autorizacao.ehDonoOuAdmin(#usuario.id)")
	@PostMapping("/listarDisponiveis")
	@ResponseBody
	public List<Atividade> listarAtividadesDisponiveis(@RequestBody Usuario usuario) {
		return atividadeServ.listarDisponiveisParaInscricao(usuario);
	}
	
	@PreAuthorize("isAuthenticated()")
	@PostMapping("/listarPassadas")
	@ResponseBody
	public List<Atividade> listarAtividadesPassadas(){
		return atividadeServ.listarPassadas();
	}
	
	@Autowired
	private AtividadeService atividadeServ;
}
