package br.com.fateczs.seazs.unit.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import br.com.fateczs.seazs.model.Evento;
import br.com.fateczs.seazs.model.Usuario;
import br.com.fateczs.seazs.security.Autorizacao;
import br.com.fateczs.seazs.service.EventoService;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.security.access.AccessDeniedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import br.com.fateczs.seazs.controller.EventoController;

@ExtendWith(MockitoExtension.class)
public class EventoControllerTest {

	@Mock
	private EventoService eventoServ;

	@Mock
	private Autorizacao autorizacao;

	@InjectMocks
	private EventoController controller;

	private static final Instant AGORA = Instant.parse("2019-03-15T15:00:00Z");

	@BeforeEach
	public void fixaRelogio() {
		ReflectionTestUtils.setField(controller, "clock", Clock.fixed(AGORA, ZoneId.of("America/Sao_Paulo")));
	}

	@Test
	public void inserirEventoDelegaParaOService() {
		when(autorizacao.idAutenticado()).thenReturn(Optional.of(5));
		Evento arg = new Evento();
		controller.inserirEvento(arg);
		assertEquals(AGORA.toEpochMilli(), arg.getDataInclusao().getTime());
		verify(eventoServ).inserir(arg);
	}

	@Test
	public void buscarEventoDelegaParaOService() {
		Evento arg = new Evento();
		Evento esperado = new Evento();
		when(eventoServ.buscar(arg)).thenReturn(esperado);
		assertSame(esperado, controller.buscarEvento(arg));
		verify(eventoServ).buscar(arg);
	}

	@Test
	public void alterarEventoDelegaParaOService() {
		Evento arg = new Evento();
		controller.alterarEvento(arg);
		verify(eventoServ).alterar(arg);
	}

	@Test
	public void excluirEventoDelegaParaOService() {
		Evento arg = new Evento();
		controller.excluirEvento(arg);
		verify(eventoServ).excluir(arg);
	}

	@Test
	public void listarEventoDelegaParaOService() {
		List<Evento> esperado = new ArrayList<>();
		when(eventoServ.listar()).thenReturn(esperado);
		assertSame(esperado, controller.listarEvento());
		verify(eventoServ).listar();
	}

	@Test
	public void listarEventoPorDtInicioDelegaParaOService() {
		List<Evento> esperado = new ArrayList<>();
		when(eventoServ.listarPorDataAsc()).thenReturn(esperado);
		assertSame(esperado, controller.listarEventoPorDtInicio());
		verify(eventoServ).listarPorDataAsc();
	}

	@Test
	public void listarEventoDisponivelPorDtInicioDelegaParaOService() {
		List<Evento> esperado = new ArrayList<>();
		when(eventoServ.listarDisponivelPorDataAsc()).thenReturn(esperado);
		assertSame(esperado, controller.listarEventoDisponivelPorDtInicio());
		verify(eventoServ).listarDisponivelPorDataAsc();
	}

	@Test
	public void inserirPreencheDataInclusaoAntesDeDelegar() {
		when(autorizacao.idAutenticado()).thenReturn(Optional.of(5));
		Evento evento = new Evento();
		controller.inserirEvento(evento);
		assertNotNull(evento.getDataInclusao());
		verify(eventoServ).inserir(evento);
	}

	@Test
	public void inserirSobrescreveOCriadorDoCorpoComOUsuarioAutenticado() {
		when(autorizacao.idAutenticado()).thenReturn(Optional.of(5));
		Usuario falso = new Usuario();
		falso.setId(99);
		Evento evento = new Evento();
		evento.setUsuarioCriador(falso);

		controller.inserirEvento(evento);

		assertEquals(Integer.valueOf(5), evento.getUsuarioCriador().getId());
		verify(eventoServ).inserir(evento);
	}

	@Test
	public void inserirSemUsuarioAutenticadoNegaSemChamarOService() {
		when(autorizacao.idAutenticado()).thenReturn(Optional.empty());

		assertThrows(AccessDeniedException.class, () -> controller.inserirEvento(new Evento()));

		verifyNoInteractions(eventoServ);
	}
}
