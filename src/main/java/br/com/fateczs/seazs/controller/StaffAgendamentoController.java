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
import br.com.fateczs.seazs.model.StaffAgendamento;
import br.com.fateczs.seazs.model.Usuario;
import br.com.fateczs.seazs.service.StaffAgendamentoService;

@RestController
@RequestMapping("/agendamento/staff")
public class StaffAgendamentoController {
	
	@PreAuthorize("hasRole('ADMIN')")
	@PostMapping("/inserir")
	@ResponseBody
	public void inserirInscricao(@Valid @RequestBody StaffAgendamento staff) {
		staffServ.inserir(staff);
	}
	
	@PreAuthorize("@autorizacao.ehDonoDoStaffOuAdmin(#staff.sequencialStaff)")
	@PostMapping("/buscar")
	@ResponseBody
	public StaffAgendamento buscarInscricao(@Valid @RequestBody StaffAgendamento staff) {
		return staffServ.buscar(staff);
	}
	
	@PreAuthorize("hasRole('ADMIN')")
	@PostMapping("/alterar")
	@ResponseBody
	public void alterarInscricao(@Valid @RequestBody StaffAgendamento staff) {
		staffServ.alterar(staff);
	}
	
	@PreAuthorize("hasRole('ADMIN')")
	@PostMapping("/excluir")
	@ResponseBody
	public void excluirInscricao(@Valid @RequestBody StaffAgendamento staff) {
		staffServ.excluir(staff);
	}
	
	@PreAuthorize("hasRole('ADMIN')")
	@PostMapping("/listar")
	@ResponseBody
	public List<StaffAgendamento> listarPapel() {
		return staffServ.listar();
	}
	
	@PreAuthorize("hasRole('ADMIN')")
	@PostMapping("/listarData")
	@ResponseBody
	public List<StaffAgendamento> listarAtividadePorDtInicio() {
		return staffServ.listarPorDataAsc();
	}
	
	@PreAuthorize("@autorizacao.ehDonoOuAdmin(#staff.usuario?.id)")
	@PostMapping("/ValidarPresenca")
	@ResponseBody
	public StaffAgendamento validarPresenca(@RequestBody StaffAgendamento staff) {
		staff.toString();
		return staffServ.validarPresencaStaff(staff);
	}
	
	@PreAuthorize("@autorizacao.ehDonoOuAdmin(#usuario.id)")
	@PostMapping("/AgendamentosUsuario")
	@ResponseBody
	public List<Agendamento> listarAgendamentosUsuario(@RequestBody Usuario usuario){
		return staffServ.listarAgendamentosUsuario(usuario);
	}
	
	@PreAuthorize("@autorizacao.ehDonoOuAdmin(#usuario.id)")
	@PostMapping("/AgendamentosUsuarioPassado")
	@ResponseBody
	public List<Agendamento> listarAgendamentosUsuarioPassado(@RequestBody Usuario usuario){
		return staffServ.listarAgendamentosUsuarioPassado(usuario);
	}
	
	@Autowired
	private StaffAgendamentoService staffServ;
	
}
