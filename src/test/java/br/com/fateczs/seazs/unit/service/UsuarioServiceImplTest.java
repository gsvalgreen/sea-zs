package br.com.fateczs.seazs.unit.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.fateczs.seazs.model.Usuario;
import org.springframework.security.crypto.password.PasswordEncoder;

import br.com.fateczs.seazs.repository.RefreshTokenRepository;
import br.com.fateczs.seazs.repository.UsuarioRepository;
import br.com.fateczs.seazs.service.serviceImpl.UsuarioServiceImpl;
import br.com.fateczs.seazs.utils.TipoUsuario;

@ExtendWith(MockitoExtension.class)
public class UsuarioServiceImplTest {

	@Mock
	private UsuarioRepository repository;
	@Mock
	private RefreshTokenRepository refreshTokenRepository;
	@Mock
	private PasswordEncoder passwordEncoder;

	@InjectMocks
	private UsuarioServiceImpl service;

	@Test
	public void buscarDelegaAoRepositorioPeloId() {
		Usuario entidade = new Usuario();
		entidade.setId(7);
		Usuario esperado = new Usuario();
		when(repository.getReferenceById(7)).thenReturn(esperado);

		assertEquals(esperado, service.buscar(entidade));
	}

	@Test
	public void inserirGuardaSenhaCodificadaENuncaOTextoPuro() {
		Usuario entidade = new Usuario();
		entidade.setSenha("Segredo123");
		when(passwordEncoder.encode("Segredo123")).thenReturn("{bcrypt}HASH");

		service.inserir(entidade);

		ArgumentCaptor<Usuario> salvo = ArgumentCaptor.forClass(Usuario.class);
		verify(repository).save(salvo.capture());
		assertEquals("{bcrypt}HASH", salvo.getValue().getSenha());
	}

	@Test
	public void inserirComSenhaAcimaDoLimiteDoBcryptPropagaAExcecaoDoEncoderSemPersistir() {
		Usuario entidade = new Usuario();
		entidade.setSenha("x");
		when(passwordEncoder.encode("x")).thenThrow(new IllegalArgumentException("password cannot be more than 72 bytes"));

		assertThrows(IllegalArgumentException.class, () -> service.inserir(entidade));

		verify(repository, never()).save(any());
	}

	@Test
	public void inserirIgnoraAdmTipoUsuarioEIdEnviadosPeloCliente() {
		Usuario entidade = new Usuario();
		entidade.setId(1);
		entidade.setSenha("Segredo123");
		entidade.setAdm(true);
		entidade.setTipoUsuario(TipoUsuario.PROFESSOR);
		when(passwordEncoder.encode("Segredo123")).thenReturn("{bcrypt}HASH");

		service.inserir(entidade);

		ArgumentCaptor<Usuario> salvo = ArgumentCaptor.forClass(Usuario.class);
		verify(repository).save(salvo.capture());
		assertFalse(salvo.getValue().getAdm());
		assertEquals(TipoUsuario.VISITANTE, salvo.getValue().getTipoUsuario());
		assertNull(salvo.getValue().getId());
	}

	@Test
	public void alterarPreservaAdmETipoUsuarioGravados() {
		Usuario gravado = new Usuario();
		gravado.setAdm(true);
		gravado.setTipoUsuario(TipoUsuario.FUNCIONARIO);
		when(repository.findById(5)).thenReturn(Optional.of(gravado));
		Usuario entidade = new Usuario();
		entidade.setId(5);
		entidade.setSenha("nova");
		entidade.setAdm(false);
		entidade.setTipoUsuario(TipoUsuario.ALUNO);

		service.alterar(entidade);

		verify(repository).save(entidade);
		assertEquals(Boolean.TRUE, entidade.getAdm());
		assertEquals(TipoUsuario.FUNCIONARIO, entidade.getTipoUsuario());
	}

	@Test
	public void alterarSemRegistroGravadoNaoPermiteEscalarPrivilegio() {
		when(repository.findById(8)).thenReturn(Optional.empty());
		Usuario entidade = new Usuario();
		entidade.setId(8);
		entidade.setSenha("nova");
		entidade.setAdm(true);
		entidade.setTipoUsuario(TipoUsuario.PROFESSOR);

		service.alterar(entidade);

		assertFalse(entidade.getAdm());
		assertEquals(TipoUsuario.VISITANTE, entidade.getTipoUsuario());
	}

	@Test
	public void alterarGuardaSenhaCodificada() {
		Usuario entidade = new Usuario();
		entidade.setSenha("nova");
		when(passwordEncoder.encode("nova")).thenReturn("{bcrypt}NOVO");

		service.alterar(entidade);

		verify(repository).save(entidade);
		assertEquals("{bcrypt}NOVO", entidade.getSenha());
	}

	@Test
	public void excluirRemoveOsRefreshTokensEOUsuario() {
		Usuario entidade = new Usuario();
		entidade.setId(9);
		service.excluir(entidade);
		verify(refreshTokenRepository).deleteByIdUsuario(9);
		verify(repository).delete(entidade);
	}

	@Test
	public void listarRetornaTodos() {
		List<Usuario> todos = Arrays.asList(new Usuario(), new Usuario());
		when(repository.findAll()).thenReturn(todos);

		assertEquals(todos, service.listar());
	}

	@Test
	public void emitirCrachaMontaCrachaDoUsuarioPersistido() {
		Usuario entrada = new Usuario();
		entrada.setId(3);
		Usuario persistido = new Usuario();
		persistido.setNome("Ana");
		persistido.setTipoUsuario(br.com.fateczs.seazs.utils.TipoUsuario.ALUNO);
		when(repository.getReferenceById(3)).thenReturn(persistido);

		br.com.fateczs.seazs.model.CrachaUsuario cracha = service.emitirCracha(entrada);

		assertEquals("Ana", cracha.getNomeDoUsuario());
		assertEquals("ALUNO", cracha.getTipoDoUsuario());
	}

	private static org.springframework.dao.DataIntegrityViolationException violacao(
			org.hibernate.exception.ConstraintViolationException.ConstraintKind tipo) {
		return new org.springframework.dao.DataIntegrityViolationException("violacao",
				new org.hibernate.exception.ConstraintViolationException("detalhe interno", new java.sql.SQLException("x"), tipo,
						"constraint"));
	}

	@Test
	public void inserirComEmailDuplicadoLancaEmailJaCadastradoSemVazarOEmail() {
		Usuario entidade = new Usuario();
		entidade.setEmail("ana@teste.com");
		entidade.setSenha("Segredo123");
		when(passwordEncoder.encode("Segredo123")).thenReturn("{bcrypt}HASH");
		when(repository.save(any(Usuario.class)))
				.thenThrow(violacao(org.hibernate.exception.ConstraintViolationException.ConstraintKind.UNIQUE));

		br.com.fateczs.seazs.service.EmailJaCadastradoException ex = assertThrows(
				br.com.fateczs.seazs.service.EmailJaCadastradoException.class, () -> service.inserir(entidade));

		assertFalse(ex.getMessage().contains("ana@teste.com"));
	}

	@Test
	public void inserirComOutraViolacaoDeIntegridadeNaoViraConflitoDeEmail() {
		Usuario entidade = new Usuario();
		entidade.setSenha("Segredo123");
		when(passwordEncoder.encode("Segredo123")).thenReturn("{bcrypt}HASH");
		org.springframework.dao.DataIntegrityViolationException notNull = violacao(
				org.hibernate.exception.ConstraintViolationException.ConstraintKind.NOT_NULL);
		when(repository.save(any(Usuario.class))).thenThrow(notNull);

		org.springframework.dao.DataIntegrityViolationException lancada = assertThrows(
				org.springframework.dao.DataIntegrityViolationException.class, () -> service.inserir(entidade));

		assertSame(notNull, lancada);
	}

	@Test
	public void inserirComViolacaoSemCausaDoHibernateNaoViraConflitoDeEmail() {
		Usuario entidade = new Usuario();
		entidade.setSenha("Segredo123");
		when(passwordEncoder.encode("Segredo123")).thenReturn("{bcrypt}HASH");
		when(repository.save(any(Usuario.class)))
				.thenThrow(new org.springframework.dao.DataIntegrityViolationException("Duplicate entry"));

		assertThrows(org.springframework.dao.DataIntegrityViolationException.class, () -> service.inserir(entidade));
	}
}
