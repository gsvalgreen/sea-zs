package br.com.fateczs.seazs.controller;

import br.com.fateczs.seazs.model.CertificadoAtividade;
import br.com.fateczs.seazs.model.CertificadoStaff;
import br.com.fateczs.seazs.model.Inscricao;
import br.com.fateczs.seazs.model.StaffAgendamento;
import br.com.fateczs.seazs.service.CertificadoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;


@RestController
@RequestMapping("/certificado")
public class CertificadoController {
	
	@PreAuthorize("@autorizacao.ehDonoOuAdmin(#inscricao.usuario?.id)")
	@PostMapping("/atividade/emitir")
	@ResponseBody
	public CertificadoAtividade emitirCertificadoAtividade(@Valid @RequestBody Inscricao inscricao) {
		return certificadoService.emiteCertificadoAtividade(inscricao);
	}
	@PreAuthorize("@autorizacao.ehDonoOuAdmin(#inscricaoStaff.usuario?.id)")
	@PostMapping("/agendamento/emitir")
	@ResponseBody
	public CertificadoStaff emitirCertificadoAgendamento(@Valid @RequestBody StaffAgendamento inscricaoStaff) {
		return certificadoService.emiteCertificadoAgendamento(inscricaoStaff);
	}
	
	@Autowired
	private CertificadoService certificadoService;
}
