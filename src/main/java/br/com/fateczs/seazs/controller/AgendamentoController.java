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
import br.com.fateczs.seazs.model.Evento;
import br.com.fateczs.seazs.service.AgendamentoService;

/**
 * Controlador de Requisições aos Serviços do sistema. (HTTP e Local)
 * @author Gustavo Santos Valverde
 *
 */
@RestController
@RequestMapping("/agendamento")
public class AgendamentoController {
	
	/**
	 * Acessa o serviço de inserção de Agendamento
	 * @param agendamento Objeto Agendamento totalmente Preenchido* para Inserts. 
	 * Atributos de Relacionamento requerem apenas o ID preenchido (exemplo: Id do usuárioOrganizador).
	 */
	@PreAuthorize("hasAnyRole('ADMIN', 'FUNCIONARIO') or (hasRole('PROFESSOR') and #agendamento.id == null)")
	@PostMapping("/inserir")
	@ResponseBody
	public void inserirAgendamento(@Valid @RequestBody Agendamento agendamento) {
		agendamentoServ.inserir(agendamento);
	}
	
	/**
	 * Acessa o serviço que busca um Agendamento específico
	 * @param agendamento Objeto preenchido apenas com o atributo Id
	 * @return Objeto Agendamento preenchido com as informações preenchidas no BD
	 */
	@PreAuthorize("isAuthenticated()")
	@PostMapping("/buscar")
	@ResponseBody
	public Agendamento buscarAgendamento(@Valid @RequestBody Agendamento agendamento) {
		return agendamentoServ.buscar(agendamento);
	}
	
	/**
	 * Acessa o serviço que altera um Agendamento
	 * @param agendamento Objeto totalmente preenchido para o Update no BD*
	 * Atributos de Relacionamento requerem apenas o ID. 
	 * Qualquer atributo da Entidade não preenchido será salvo como null no BD.
	 */
	@PreAuthorize("hasAnyRole('ADMIN', 'FUNCIONARIO')")
	@PostMapping("/alterar")
	@ResponseBody
	public void alterarAgendamento(@Valid @RequestBody Agendamento agendamento) {
		agendamentoServ.alterar(agendamento);
	}
	
	/**
	 * Acessa o Serviço que exclui o Agendamento informado
	 * @param agendamento Objeto preenchido apenas com o ID.
	 */
	@PreAuthorize("hasAnyRole('ADMIN', 'FUNCIONARIO')")
	@PostMapping("/excluir")
	@ResponseBody
	public void excluirAgendamento(@Valid @RequestBody Agendamento agendamento) {
		agendamentoServ.excluir(agendamento);
	}
	
	/**
	 * Acessa o Serviço que Lista todos os Agendamentos
	 * @return Lista de Objetos agendamento persistidos no BD.
	 */
	@PreAuthorize("isAuthenticated()")
	@PostMapping("/listar")
	@ResponseBody
	public List<Agendamento> listarAgendamento(){
		return agendamentoServ.listar();
	}
	/**
	 * Acessa o Serviço que Lista todos os Agendamentos ordenados pela Data de Inicio (Asc)
	 * @return Lista de Objetos agendamento persistidos no BD em ordem da Data de Inicio (Asc).
	 */
	@PreAuthorize("isAuthenticated()")
	@PostMapping("/listarData")
	@ResponseBody
	public List<Agendamento> lsitarAgendamentoPorDtinicio() {
		return agendamentoServ.listarPorDataAsc();
	}
	/**
	 * Acessa o Serviço que lista os Agendamentos de um Evento
	 * @param evento Evento desejado
	 * @return Lista de Agendamentos relacionados ao Evento informado.ç
	 */
	@PreAuthorize("isAuthenticated()")
	@PostMapping("/listarEvento")
	@ResponseBody
	public List<Agendamento> listarAgendamentoDoEvento(@RequestBody Evento evento){
		return agendamentoServ.listarPorEvento(evento);
	}
	
    /**
     * Instância da Classe de Serviços da Entidade Agendamento
     */
	@Autowired
	private AgendamentoService agendamentoServ;
	
}
