package br.com.fateczs.seazs.unit.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import br.com.fateczs.seazs.model.Agendamento;
import br.com.fateczs.seazs.model.Inscricao;
import br.com.fateczs.seazs.model.StaffAgendamento;
import br.com.fateczs.seazs.model.Usuario;

public class UsuarioSenhaJsonTest {

	private static final String SEGREDO = "segredo-123";

	private final ObjectMapper mapper = new ObjectMapper();

	private Usuario usuarioComSenha() {
		Usuario u = new Usuario();
		u.setEmail("a@b.com");
		u.setSenha(SEGREDO);
		return u;
	}

	private void assertSemSenha(Object o) throws Exception {
		String json = mapper.writeValueAsString(o);
		assertFalse(json, json.contains("\"senha\""));
		assertFalse(json, json.contains(SEGREDO));
	}

	@Test
	public void usuarioNaoSerializaSenha() throws Exception {
		assertSemSenha(usuarioComSenha());
	}

	@Test
	public void inscricaoNaoExpoeSenhaDoUsuario() throws Exception {
		Inscricao i = new Inscricao();
		i.setUsuario(usuarioComSenha());
		assertSemSenha(i);
	}

	@Test
	public void staffAgendamentoNaoExpoeSenhaDoUsuario() throws Exception {
		StaffAgendamento s = new StaffAgendamento();
		s.setUsuario(usuarioComSenha());
		assertSemSenha(s);
	}

	@Test
	public void agendamentoNaoExpoeSenhaDoOrganizador() throws Exception {
		Agendamento a = new Agendamento();
		a.setUsuarioOrganizador(usuarioComSenha());
		assertSemSenha(a);
	}

	@Test
	public void usuarioAindaDesserializaSenhaNaEntrada() throws Exception {
		Usuario u = mapper.readValue("{\"email\":\"a@b.com\",\"senha\":\"" + SEGREDO + "\"}", Usuario.class);
		assertEquals(SEGREDO, u.getSenha());
	}
}
