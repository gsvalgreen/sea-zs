package br.com.fateczs.seazs.unit.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

import br.com.fateczs.seazs.model.Agendamento;
import br.com.fateczs.seazs.model.Atividade;
import br.com.fateczs.seazs.model.Usuario;
import br.com.fateczs.seazs.repository.AtividadeRepository;
import br.com.fateczs.seazs.service.serviceImpl.AtividadeServiceImpl;

@ExtendWith(MockitoExtension.class)
public class AtividadeServiceImplTest {

	@Mock
	private AtividadeRepository repository;

	@InjectMocks
	private AtividadeServiceImpl service;

	@Test
	public void buscarDelegaAoRepositorioPeloId() {
		Atividade entrada = new Atividade();
		entrada.setId(5);
		Atividade esperado = new Atividade();
		when(repository.getOne(5)).thenReturn(esperado);
		assertEquals(esperado, service.buscar(entrada));
	}

	@Test
	public void inserirAlterarExcluirListar() {
		Atividade a = new Atividade();
		List<Atividade> todas = Arrays.asList(a);
		when(repository.findAll()).thenReturn(todas);

		service.inserir(a);
		service.alterar(a);
		service.excluir(a);

		verify(repository, times(2)).save(a);
		verify(repository).delete(a);
		assertEquals(todas, service.listar());
	}

	@Test
	public void listarPorDataAscOrdenaPorInicioAtividade() {
		List<Atividade> todas = Arrays.asList(new Atividade());
		when(repository.findAll(any(Sort.class))).thenReturn(todas);

		assertEquals(todas, service.listarPorDataAsc());

		ArgumentCaptor<Sort> sort = ArgumentCaptor.forClass(Sort.class);
		verify(repository).findAll(sort.capture());
		assertEquals(Sort.Direction.ASC, sort.getValue().getOrderFor("inicioAtividade").getDirection());
	}

	@Test
	public void listarPorAgendamentoUsaIdDoAgendamento() {
		Agendamento agendamento = new Agendamento();
		agendamento.setId(8);
		List<Atividade> doAgendamento = Arrays.asList(new Atividade());
		when(repository.findByAgendamentoId(8)).thenReturn(doAgendamento);
		assertEquals(doAgendamento, service.listarPorAgendamento(agendamento));
	}

	@Test
	public void listarDisponiveisParaInscricaoUsaIdDoUsuario() {
		Usuario usuario = new Usuario();
		usuario.setId(3);
		List<Atividade> disponiveis = Arrays.asList(new Atividade());
		when(repository.findAtividadeDisponivelParaInscricao(eq(3), any(Sort.class))).thenReturn(disponiveis);
		assertEquals(disponiveis, service.listarDisponiveisParaInscricao(usuario));
	}

	@Test
	public void listarPassadasConsultaAtividadesPassadas() {
		List<Atividade> passadas = Arrays.asList(new Atividade());
		when(repository.findAtividadesPassadas(any(Sort.class))).thenReturn(passadas);
		assertEquals(passadas, service.listarPassadas());
	}
}
