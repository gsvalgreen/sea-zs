package br.com.fateczs.seazs.controller;

import java.time.Clock;
import java.util.Date;
import java.util.List;

import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import br.com.fateczs.seazs.model.Evento;
import br.com.fateczs.seazs.service.EventoService;

@RestController
@RequestMapping("/evento")
public class EventoController {
	
	@PreAuthorize("hasAnyRole('ADMIN', 'FUNCIONARIO') or (hasRole('PROFESSOR') and #evento.id == null)")
	@PostMapping("/inserir")
	@ResponseBody
	public void inserirEvento(@Valid @RequestBody Evento evento) {
		evento.setDataInclusao(new Date(clock.millis()));
		eventoServ.inserir(evento);
	}
	
	@PreAuthorize("isAuthenticated()")
	@PostMapping("/buscar")
	@ResponseBody
	public Evento buscarEvento(@Valid @RequestBody Evento evento) {
		return eventoServ.buscar(evento);
	}
	
	@PreAuthorize("hasAnyRole('ADMIN', 'FUNCIONARIO')")
	@PostMapping("/alterar")
	@ResponseBody
	public void alterarEvento(@Valid @RequestBody Evento evento) {
		eventoServ.alterar(evento);
	}
	
	@PreAuthorize("hasAnyRole('ADMIN', 'FUNCIONARIO')")
	@PostMapping("/excluir")
	@ResponseBody
	public void excluirEvento(@Valid @RequestBody Evento evento) {
		eventoServ.excluir(evento);
	}
	
	@PreAuthorize("isAuthenticated()")
	@PostMapping("/listar")
	@ResponseBody
	public List<Evento> listarEvento() {
		return eventoServ.listar();
	}
	
	@PreAuthorize("isAuthenticated()")
	@PostMapping("/listarData")
	@ResponseBody
	public List<Evento> listarEventoPorDtInicio() {
		return eventoServ.listarPorDataAsc();
	}
	
	@PreAuthorize("isAuthenticated()")
	@PostMapping("/listarDisponivelData")
	@ResponseBody
	public List<Evento> listarEventoDisponivelPorDtInicio() {
		return eventoServ.listarDisponivelPorDataAsc();
	}
	
	@Autowired
	private Clock clock;

	@Autowired
	private EventoService eventoServ;

}
