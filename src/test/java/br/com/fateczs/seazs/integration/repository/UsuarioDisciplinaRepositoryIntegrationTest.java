package br.com.fateczs.seazs.integration.repository;

import static br.com.fateczs.seazs.integration.support.DadosTeste.curso;
import static br.com.fateczs.seazs.integration.support.DadosTeste.data;
import static br.com.fateczs.seazs.integration.support.DadosTeste.disciplina;
import static br.com.fateczs.seazs.integration.support.DadosTeste.usuario;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Calendar;
import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import br.com.fateczs.seazs.integration.support.RepositorioIntegracaoBase;
import br.com.fateczs.seazs.model.Disciplina;
import br.com.fateczs.seazs.model.Usuario;
import br.com.fateczs.seazs.model.UsuarioDisciplina;
import br.com.fateczs.seazs.model.UsuarioDisciplinaID;
import br.com.fateczs.seazs.repository.UsuarioDisciplinaRepository;

/** Chave composta (usuario, disciplina, dtInicio) com @ManyToOne dentro do @Embeddable. */
public class UsuarioDisciplinaRepositoryIntegrationTest extends RepositorioIntegracaoBase {

	private static final Date INICIO = data(2024, Calendar.FEBRUARY, 1, 0);

	@Autowired
	private TestEntityManager em;
	@Autowired
	private UsuarioDisciplinaRepository repository;

	private Usuario ana;
	private Usuario bia;
	private Disciplina java;
	private Disciplina banco;

	@BeforeEach
	public void prepararCenarioBase() {
		ana = em.persist(usuario("ana@teste.com"));
		bia = em.persist(usuario("bia@teste.com"));
		br.com.fateczs.seazs.model.Curso ads = em.persist(curso("ADS"));
		java = em.persist(disciplina("JAVA", ads));
		banco = em.persist(disciplina("BD", ads));
	}

	private static UsuarioDisciplina vinculo(Usuario u, Disciplina d, Date inicio) {
		UsuarioDisciplinaID id = new UsuarioDisciplinaID();
		id.setUsuario(u);
		id.setDisciplina(d);
		id.setDtInicio(inicio);
		UsuarioDisciplina ud = new UsuarioDisciplina();
		ud.setIdUsuarioDisciplina(id);
		return ud;
	}

	private void sincronizar() {
		em.flush();
		em.clear();
	}

	@Test
	public void save_persisteEFindByIdRecuperaPelaChaveComposta() {
		UsuarioDisciplina ud = vinculo(ana, java, INICIO);
		ud.setDtFim(data(2024, Calendar.JULY, 1, 0));
		repository.save(ud);
		sincronizar();

		UsuarioDisciplina lido = repository.findById(vinculo(ana, java, INICIO).getIdUsuarioDisciplina()).get();

		assertEquals(data(2024, Calendar.JULY, 1, 0), lido.getDtFim());
	}

	@Test
	public void save_aceitaDtFimNula() {
		repository.save(vinculo(ana, java, INICIO));
		sincronizar();

		assertNull(repository.findById(vinculo(ana, java, INICIO).getIdUsuarioDisciplina()).get().getDtFim());
	}

	/**
	 * Caracterizacao: save() com chave ja existente faz merge (atualiza dtFim), nao falha nem duplica.
	 */
	@Test
	public void save_comMesmaChaveAtualizaOVinculoEmVezDeDuplicar() {
		repository.save(vinculo(ana, java, INICIO));
		sincronizar();
		UsuarioDisciplina atualizado = vinculo(ana, java, INICIO);
		atualizado.setDtFim(data(2024, Calendar.JULY, 1, 0));

		repository.save(atualizado);
		sincronizar();

		assertEquals(1, repository.count());
	}

	@Test
	public void save_mesmoUsuarioEDisciplinaComDtInicioDiferenteGeraOutroVinculo() {
		repository.save(vinculo(ana, java, INICIO));
		repository.save(vinculo(ana, java, data(2025, Calendar.FEBRUARY, 1, 0)));
		sincronizar();

		assertEquals(2, repository.count());
	}

	@Test
	public void findByIdUsuarioDisciplinaUsuarioId_retornaSomenteVinculosDoUsuario() {
		repository.save(vinculo(ana, java, INICIO));
		repository.save(vinculo(ana, banco, INICIO));
		repository.save(vinculo(bia, java, INICIO));
		sincronizar();

		assertEquals(2, repository.findByIdUsuarioDisciplinaUsuarioId(ana.getId()).size());
	}

	@Test
	public void findByIdUsuarioDisciplinaUsuarioId_devolveListaVaziaParaUsuarioSemVinculos() {
		sincronizar();

		assertTrue(repository.findByIdUsuarioDisciplinaUsuarioId(ana.getId()).isEmpty());
	}

	@Test
	public void delete_removeSomenteOVinculoIndicado() {
		repository.save(vinculo(ana, java, INICIO));
		repository.save(vinculo(ana, banco, INICIO));
		sincronizar();

		repository.deleteById(vinculo(ana, java, INICIO).getIdUsuarioDisciplina());
		sincronizar();

		assertFalse(repository.findById(vinculo(ana, java, INICIO).getIdUsuarioDisciplina()).isPresent());
	}
}
