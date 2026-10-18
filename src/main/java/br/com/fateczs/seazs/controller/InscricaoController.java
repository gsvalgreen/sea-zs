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

import br.com.fateczs.seazs.model.Atividade;
import br.com.fateczs.seazs.model.Inscricao;
import br.com.fateczs.seazs.model.Usuario;
import br.com.fateczs.seazs.service.InscricaoService;


@RestController
@RequestMapping("/atividade/inscricao")
public class InscricaoController {
	
	@PreAuthorize("@autorizacao.ehDonoOuAdmin(#inscricao.usuario?.id)")
	@PostMapping("/inserir")
	@ResponseBody
	public void inserirInscricao(@Valid @RequestBody Inscricao inscricao) {
		inscricaoServ.inserir(inscricao);
	}
	
	@PreAuthorize("@autorizacao.ehDonoDaInscricaoOuAdmin(#inscricao.sequencialInscricao)")
	@PostMapping("/buscar")
	@ResponseBody
	public Inscricao buscarInscricao(@Valid @RequestBody Inscricao inscricao) {
		return inscricaoServ.buscar(inscricao);
	}
	
	@PreAuthorize("hasRole('ADMIN')")
	@PostMapping("/alterar")
	@ResponseBody
	public void alterarInscricao(@Valid @RequestBody Inscricao inscricao) {
		inscricaoServ.alterar(inscricao);
	}
	
	@PreAuthorize("@autorizacao.ehDonoDaInscricaoOuAdmin(#inscricao.sequencialInscricao)")
	@PostMapping("/excluir")
	@ResponseBody
	public void excluirInscricao(@Valid @RequestBody Inscricao inscricao) {
		inscricaoServ.excluir(inscricao);
	}
	
	@PreAuthorize("hasRole('ADMIN')")
	@PostMapping("/listar")
	@ResponseBody
	public List<Inscricao> listarInscricao() {
		return inscricaoServ.listar();
	}
	
	@PreAuthorize("hasRole('ADMIN')")
	@PostMapping("/listarData")
	@ResponseBody
	public List<Inscricao> listarAtividadePorDtInicio() {
		return inscricaoServ.listarPorDataAsc();
	}
	
	@PreAuthorize("@autorizacao.ehDonoOuAdmin(#usuario.id)")
	@PostMapping("/listarAtividadesInscrito")
	@ResponseBody
	public List<Atividade> listarAtividadesInscrito(@RequestBody Usuario usuario) {
		return inscricaoServ.listarAtividadesInscrito(usuario);
	}
	
	@PreAuthorize("@autorizacao.ehDonoOuAdmin(#usuario.id)")
	@PostMapping("/listarAtividadesParticipou")
	@ResponseBody
	public List<Atividade> listarAtividadesParticipou(@RequestBody Usuario usuario){
		return inscricaoServ.listarAtividadesInscritoPassado(usuario);
	}
	
	@PreAuthorize("@autorizacao.ehDonoDaInscricaoOuAdmin(#inscricao.sequencialInscricao)")
	@PostMapping("/validaPresenca")
	@ResponseBody
	public Inscricao validaPresenca(@Valid @RequestBody Inscricao inscricao) {
		return inscricaoServ.validaPresenca(inscricao);
	}
	
	@PreAuthorize("hasRole('ADMIN')")
	@PostMapping("/listarAtividade")
	@ResponseBody
	public List<Inscricao> listarPorAtividade(@RequestBody Atividade atividade){
		return inscricaoServ.listarPorAtividade(atividade);
	}
	
	@PreAuthorize("@autorizacao.ehDonoOuAdmin(#usuario.id)")
	@PostMapping("/listarUsuario")
	@ResponseBody
	public List<Inscricao> listarPorUsuario(@RequestBody Usuario usuario){
		return inscricaoServ.listarPorUsuario(usuario);
	}
	
	@PreAuthorize("@autorizacao.ehDonoOuAdmin(#usuario.id)")
	@PostMapping("/somarPontos")
	@ResponseBody
	public Integer somarTotalDePontos(@RequestBody Usuario usuario){
		return inscricaoServ.somaTotalDePontos(usuario);
	}
	
	@PreAuthorize("@autorizacao.ehDonoOuAdmin(#usuario.id)")
	@PostMapping("/somarPontosSemestre")
	@ResponseBody
	public Integer somarTotalDePontosSemestre(@RequestBody Usuario usuario) {
		return inscricaoServ.somaTotalDePontosNoSemestre(usuario);
	}
	
	
	@Autowired
	private InscricaoService inscricaoServ;
}
