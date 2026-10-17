package br.com.fateczs.seazs.integration.support;

import java.util.Calendar;
import java.util.Date;

import br.com.fateczs.seazs.model.Agendamento;
import br.com.fateczs.seazs.model.Atividade;
import br.com.fateczs.seazs.model.Curso;
import br.com.fateczs.seazs.model.Disciplina;
import br.com.fateczs.seazs.model.Evento;
import br.com.fateczs.seazs.model.Inscricao;
import br.com.fateczs.seazs.model.Papel;
import br.com.fateczs.seazs.model.StaffAgendamento;
import br.com.fateczs.seazs.model.Usuario;
import br.com.fateczs.seazs.utils.ModalidadeCurso;
import br.com.fateczs.seazs.utils.TipoUsuario;
import br.com.fateczs.seazs.utils.Turno;

/**
 * Fabrica de entidades (nao persistidas) com todos os campos obrigatorios preenchidos.
 * Datas sao fixas (sem relogio do sistema). Para as queries que comparam com CURRENT_TIMESTAMP
 * usa-se {@link #PASSADO} e {@link #FUTURO}, distantes o bastante para nao depender da hora de execucao.
 */
public final class DadosTeste {

	public static final Date PASSADO = data(2000, Calendar.JANUARY, 10, 10);
	public static final Date FUTURO = data(2999, Calendar.JANUARY, 10, 10);

	private DadosTeste() {
	}

	public static Date data(int ano, int mes, int dia, int hora) {
		Calendar c = Calendar.getInstance();
		c.clear();
		c.set(ano, mes, dia, hora, 0, 0);
		return c.getTime();
	}

	public static Usuario usuario(String email) {
		Usuario u = new Usuario();
		u.setEmail(email);
		u.setSenha("segredo123");
		u.setNome("Nome " + email);
		u.setEndereco("Rua A, 1");
		u.setCelular("11999999999");
		u.setDataNasc(data(1990, Calendar.MARCH, 5, 0));
		u.setSemestre(3);
		u.setTurno(Turno.NOITE);
		u.setAdm(false);
		u.setRg("123456789");
		u.setGenero("F");
		u.setMatricula("MAT-" + email);
		u.setTipoUsuario(TipoUsuario.ALUNO);
		return u;
	}

	public static Curso curso(String nome) {
		Curso c = new Curso();
		c.setNome(nome);
		c.setModalidade(ModalidadeCurso.PRESENCIAL);
		c.setQtdSemestres(6);
		return c;
	}

	public static Disciplina disciplina(String codigo, Curso curso) {
		Disciplina d = new Disciplina();
		d.setCodigoSiga(codigo);
		d.setNome("Disciplina " + codigo);
		d.setCurso(curso);
		return d;
	}

	public static Papel papel(String nome) {
		Papel p = new Papel();
		p.setNome(nome);
		p.setDescricao("Papel " + nome);
		return p;
	}

	public static Evento evento(String nome, Usuario criador) {
		Evento e = new Evento();
		e.setNome(nome);
		e.setDescricao("Evento " + nome);
		e.setInicio(PASSADO);
		e.setFim(FUTURO);
		e.setDataInclusao(data(2020, Calendar.JANUARY, 1, 0));
		e.setUsuarioCriador(criador);
		return e;
	}

	public static Agendamento agendamento(Evento evento, Usuario organizador, Date inicio, Date fim) {
		Agendamento a = new Agendamento();
		a.setEvento(evento);
		a.setUsuarioOrganizador(organizador);
		a.setInicio(inicio);
		a.setFim(fim);
		a.setPontuacaoStaff(20);
		return a;
	}

	public static Atividade atividade(String nome, Agendamento agendamento, Date inicio, Date fim) {
		Atividade a = new Atividade();
		a.setAgendamento(agendamento);
		a.setNome(nome);
		a.setCategoria("Palestra");
		a.setLocalAtividade("Sala 1");
		a.setInicioAtividade(inicio);
		a.setFimAtividade(fim);
		a.setCargaHoraria(2);
		a.setQtdVagas(50);
		a.setPontuacaoParticipante(10);
		a.setFlagContinua(false);
		a.setFlagCumulativa(false);
		return a;
	}

	public static Inscricao inscricao(Atividade atividade, Usuario usuario, Papel papel, Date data,
			boolean checkIn, boolean checkOut, int pontos) {
		Inscricao i = new Inscricao();
		i.setAtividade(atividade);
		i.setUsuario(usuario);
		i.setPapel(papel);
		i.setDtInscricao(data);
		i.setCheckIn(checkIn);
		i.setCheckOut(checkOut);
		i.setPontuacaoRecebida(pontos);
		return i;
	}

	public static StaffAgendamento staff(Agendamento agendamento, Usuario usuario, Date data,
			boolean checkIn, boolean checkOut, int pontos) {
		StaffAgendamento s = new StaffAgendamento();
		s.setAgendamento(agendamento);
		s.setUsuario(usuario);
		s.setDtInscricao(data);
		s.setCheckIn(checkIn);
		s.setCheckOut(checkOut);
		s.setPontuacaoRecebida(pontos);
		return s;
	}
}
