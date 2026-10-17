package br.com.fateczs.seazs.service.serviceImpl;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import br.com.fateczs.seazs.model.Atividade;
import br.com.fateczs.seazs.model.Inscricao;
import br.com.fateczs.seazs.model.Usuario;
import br.com.fateczs.seazs.repository.InscricaoRepository;
import br.com.fateczs.seazs.service.InscricaoService;
import br.com.fateczs.seazs.utils.DataUtils;

@Service
public class InscricaoServiceImpl implements InscricaoService {

	private static final Logger log = LoggerFactory.getLogger(InscricaoServiceImpl.class);
	
	private DataUtils operaData = new DataUtils();
	
	@Override
	public Inscricao buscar(Inscricao inscricao) {
		return repository.getOne(inscricao.getSequencialInscricao());
	}

	@Override
	public void inserir(Inscricao inscricao) {
		inscricao.setDtInscricao(operaData.getDataAtual().getTime());;
		inscricao.setCheckIn(false);
		inscricao.setCheckOut(false);
		inscricao.setPontuacaoRecebida(0);
		// TODO Auto-generated method stub
		repository.save(inscricao);
	}

	@Override
	public void alterar(Inscricao inscricao) {
		// TODO Auto-generated method stub
		repository.save(inscricao);
	}

	@Override
	public void excluir(Inscricao inscricao) {
		// TODO Auto-generated method stub
		repository.delete(inscricao);
	}

	@Override
	public List<Inscricao> listar() {
		// TODO Auto-generated method stub
		return repository.findAll();
	}

	@Override
	public List<Inscricao> listarPorDataAsc() {
		// TODO Auto-generated method stub
		return repository.findAll(sortByDtInscricaoAsc());
	}
	
	@Override
	public List<Atividade> listarAtividadesInscrito(Usuario usuario){
		return repository.findAtividadesInscrito(usuario.getId(), sortByDtInicioAtividadeAsc());
	}
	
	@Override
	public List<Atividade> listarAtividadesInscritoPassado(Usuario usuario){
		return repository.findAtividadesInscritoPassado(usuario.getId(), sortByDtInicioAtividadeAsc());
	}
	
	private Sort sortByDtInscricaoAsc() {
		return new Sort(Sort.Direction.ASC, "dtInscricao");
	}
	
	private Sort sortByDtInicioAtividadeAsc() {
		return new Sort(Sort.Direction.ASC, "atividade.inicioAtividade");
	}
	

	@Override
	public Inscricao validaPresenca(Inscricao inscricao) {
		inscricao = buscar(inscricao);
		Date agora = new Date();
		Date inicioEntrada = operaData.somaMinutos(inscricao.getAtividade().getInicioAtividade(), -10);
		Date fimEntrada = operaData.somaMinutos(inscricao.getAtividade().getInicioAtividade(), 30);
		Date inicioSaida = operaData.somaMinutos(inscricao.getAtividade().getFimAtividade(), -10);
		Date fimSaida = operaData.somaMinutos(inscricao.getAtividade().getFimAtividade(), 30);
		
		
		// Verifica se a Atividade não é Continua para realizar checkIn ou CheckOut
		if(!inscricao.getAtividade().getFlagContinua()) {
			//Verifica se está dentro do horário de checkIn
			if(agora.before(fimEntrada)) {
				if(agora.after(inicioEntrada)) {
					log.debug("Dentro do horário de entrada");
					if (!inscricao.getCheckIn()) {
						log.debug("Fez Check In");
						inscricao.setCheckIn(true);
						repository.save(inscricao);
					}
				}	
			}
			//Verifica se está dentro do horário de checkOut
			else if (agora.before(fimSaida)) {
				if(agora.after(inicioSaida)) {
					log.debug("Dentro do horário de saida");
					if (!inscricao.getCheckOut()) {
						log.debug("Fez Check Out");
						inscricao.setCheckOut(true);
						repository.save(inscricao);
					}
				}
			}
			if (inscricao.getCheckIn() && inscricao.getCheckOut()) {
						log.debug("Presença validada, contabilizando pontos");
						inscricao.setPontuacaoRecebida(inscricao.getAtividade().getPontuacaoParticipante());
						repository.save(inscricao);
			}
		}else if(inscricao.getAtividade().getFlagContinua()) {
			if((agora.equals(inicioEntrada) || agora.after(inicioEntrada)) && agora.before(fimEntrada)) {
				inscricao.setCheckIn(true);
				inscricao.setCheckOut(true);
				if(inscricao.getAtividade().getFlagCumulativa()) {
					log.debug("[Contínua/Cumulativa] Presença Validada, contabilizando pontos");
					if((inscricao.getPontuacaoRecebida()/inscricao.getAtividade().getPontuacaoParticipante()) < inscricao.getAtividade().getQtdCheckIn()) {
						log.debug("mais um");
						inscricao.setPontuacaoRecebida(inscricao.getPontuacaoRecebida() + inscricao.getAtividade().getPontuacaoParticipante());
						inscricao = repository.save(inscricao);
					}else log.debug("Número máximo de checkIns Realizados");
				}
				else {
					log.debug("[Contínua] Presença validada, contabilizando pontos");
					inscricao.setPontuacaoRecebida(inscricao.getAtividade().getPontuacaoParticipante());
					inscricao = repository.save(inscricao);
				}
			}
		}
		return inscricao;
	}
	
	
	@Override
	public List<Inscricao> listarPorAtividade(Atividade atividade) {
		// TODO Auto-generated method stub
		return repository.findByAtividadeId(atividade.getId());
	}
	
	@Override
	public List<Inscricao> listarPorUsuario(Usuario usuario){
		return repository.findByUsuarioId(usuario.getId());
	}
	
	@Override
	public Integer somaTotalDePontos(Usuario usuario) {
		// TODO Auto-generated method stub
		return repository.somaPontuacaoRecebida(usuario.getId());
	}
	
	@Override
	public Integer somaTotalDePontosNoSemestre(Usuario usuario) {
		Integer pontuacao = null;
		Calendar dataIni = Calendar.getInstance();
		Calendar dataFim = Calendar.getInstance();
		if (operaData.verificaSemestre(operaData.getDataAtual()) == 1){
			dataIni.set(dataIni.get(Calendar.YEAR), 0, 1, 0 , 0 , 1);
			dataFim.set(dataFim.get(Calendar.YEAR), 6, 31, 23, 59, 59);
			pontuacao = repository.somaPontuacaoRecebidaNoSemestre(usuario.getId(), dataIni, dataFim);
		}
		else if (operaData.verificaSemestre(operaData.getDataAtual()) == 2) {
			dataIni.set(dataIni.get(Calendar.YEAR), 7, 1, 0 , 0 , 1);
			dataFim.set(dataFim.get(Calendar.YEAR), 11, 31, 23, 59, 59);
			pontuacao = repository.somaPontuacaoRecebidaNoSemestre(usuario.getId(), dataIni, dataFim);
		}
		return pontuacao;
	}
	
	
	
	@Autowired
	private InscricaoRepository repository;
}
