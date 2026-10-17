package br.com.fateczs.seazs.service.serviceImpl;

import java.time.Clock;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import br.com.fateczs.seazs.model.Agendamento;
import br.com.fateczs.seazs.model.StaffAgendamento;
import br.com.fateczs.seazs.model.Usuario;
import br.com.fateczs.seazs.repository.StaffAgendamentoRepository;
import br.com.fateczs.seazs.service.StaffAgendamentoService;
import br.com.fateczs.seazs.utils.DataUtils;

@Service
public class StaffAgendamentoServiceImpl implements StaffAgendamentoService {

	private static final Logger log = LoggerFactory.getLogger(StaffAgendamentoServiceImpl.class);
	
	private Sort sortByDtInscricaoAsc() {
		return Sort.by(Sort.Direction.ASC, "dtInscricao");
	}
	
	private Sort sortByDtInicioAgendamentoAsc() {
		return Sort.by(Sort.Direction.ASC, "agendamento.inicio");
	}
	
	@Autowired
	private Clock clock;

	private DataUtils operaData() {
		return new DataUtils(clock);
	}

	@Autowired
	private StaffAgendamentoRepository repository;

	@Override
	public StaffAgendamento buscar(StaffAgendamento staff) {
		return repository.getOne(staff.getSequencialStaff());
	}

	@Override
	public void inserir(StaffAgendamento staff) {
		repository.save(staff);
	}

	@Override
	public void alterar(StaffAgendamento staff) {
		repository.save(staff);
	}

	@Override
	public void excluir(StaffAgendamento staff) {
		repository.delete(staff);
	}

	@Override
	public List<StaffAgendamento> listar() {
		return repository.findAll();
	}

	@Override
	public List<StaffAgendamento> listarPorDataAsc() {
		return repository.findAll(sortByDtInscricaoAsc());
	}

	@Override
	public StaffAgendamento validarPresencaStaff(StaffAgendamento staff) {
		staff = repository.findByAgendamentoIdAndUsuarioId(staff.getAgendamento().getId(), staff.getUsuario().getId());
		
		DataUtils operaData = operaData();
		Date agora = new Date(clock.millis());
		Date inicioEntrada = operaData.somaMinutos(staff.getAgendamento().getInicio(), -10);
		Date fimSaida = operaData.somaMinutos(staff.getAgendamento().getFim(), 30);
		
		
		if(agora.before(fimSaida) && agora.after(inicioEntrada)) {
			log.debug("Dentro do horário de checagem");
			staff.setCheckIn(true);
			staff.setCheckOut(true);
			staff.setPontuacaoRecebida(staff.getAgendamento().getPontuacaoStaff());
			staff = repository.save(staff);
		}
		return staff;
	}
	
	@Override
	public List<Agendamento> listarAgendamentosUsuario(Usuario usuario) {
		// TODO Auto-generated method stub
		return repository.findAgendamentosInscrito(usuario.getId(), sortByDtInicioAgendamentoAsc());
	}
	
	@Override
	public List<Agendamento> listarAgendamentosUsuarioPassado(Usuario usuario) {
		return repository.findAgendamentosInscritoPassado(usuario.getId(), sortByDtInicioAgendamentoAsc());
	}

}
