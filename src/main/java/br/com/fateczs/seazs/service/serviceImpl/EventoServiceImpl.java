package br.com.fateczs.seazs.service.serviceImpl;

import java.util.List;

import jakarta.persistence.EntityNotFoundException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import br.com.fateczs.seazs.model.Evento;
import br.com.fateczs.seazs.repository.EventoRepository;
import br.com.fateczs.seazs.service.EventoService;

@Service
public class EventoServiceImpl implements EventoService {
	
	@Autowired
	EventoRepository repository;
	
	@Override
	public Evento buscar(Evento evento) {
		// TODO Auto-generated method stub
		return repository.getReferenceById(evento.getId());
	}

	@Override
	public void inserir(Evento evento) {
		// TODO Auto-generated method stub
		repository.save(evento);
	}

	@Override
	public void alterar(Evento evento) {
		// A autoria nao muda na alteracao: vale o criador gravado, nunca o do corpo.
		Evento gravado = evento.getId() == null ? null : repository.findById(evento.getId()).orElse(null);
		if (gravado == null) {
			throw new EntityNotFoundException("Evento inexistente");
		}
		evento.setUsuarioCriador(gravado.getUsuarioCriador());
		evento.setDataInclusao(gravado.getDataInclusao());
		repository.save(evento);
	}

	@Override
	public void excluir(Evento evento) {
		// TODO Auto-generated method stub
		repository.delete(evento);
	}

	@Override
	public List<Evento> listar() {
		// TODO Auto-generated method stub
		return repository.findAll();
	}
	
	@Override
	public List<Evento> listarPorDataAsc() {
		return repository.findAll(sortByInicioAsc());
	}
	
	@Override
	public List<Evento> listarDisponivelPorDataAsc() {
		// TODO Auto-generated method stub
		return repository.findByFimAfterToday(sortByInicioAsc());
	}
	
	private Sort sortByInicioAsc() {
		return Sort.by(Sort.Direction.ASC, "inicio");
	}

}
