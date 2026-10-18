package br.com.fateczs.seazs.unit.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.json.JsonMapper;

import br.com.fateczs.seazs.model.Agendamento;
import br.com.fateczs.seazs.model.Inscricao;
import br.com.fateczs.seazs.model.StaffAgendamento;
import br.com.fateczs.seazs.model.Usuario;

public class UsuarioSenhaJsonTest {

	private static final String SEGREDO = "segredo-123";

	private final JsonMapper mapper = JsonMapper.builder().build();

	private Usuario usuarioComSenha() {
		Usuario u = new Usuario();
		u.setEmail("a@b.com");
		u.setSenha(SEGREDO);
		return u;
	}

	private void assertSemSenha(Object o) throws Exception {
		String json = mapper.writeValueAsString(o);
		assertFalse(json.contains("\"senha\""), json);
		assertFalse(json.contains(SEGREDO), json);
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
