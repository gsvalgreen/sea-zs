package br.com.fateczs.seazs.service.serviceImpl;

import java.util.List;

import br.com.fateczs.seazs.model.CrachaUsuario;
import br.com.fateczs.seazs.model.mapper.CrachaUsuarioMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.hibernate.exception.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.fateczs.seazs.model.Usuario;
import br.com.fateczs.seazs.repository.RefreshTokenRepository;
import br.com.fateczs.seazs.repository.UsuarioRepository;
import br.com.fateczs.seazs.service.EmailJaCadastradoException;
import br.com.fateczs.seazs.service.PerfilInvalidoException;
import br.com.fateczs.seazs.service.UsuarioNaoEncontradoException;
import br.com.fateczs.seazs.service.UsuarioService;
import br.com.fateczs.seazs.utils.TipoUsuario;

@Service
public class UsuarioServiceImpl implements UsuarioService {

	private static final Logger log = LoggerFactory.getLogger(UsuarioServiceImpl.class);

	@Autowired
	private UsuarioRepository repository;

	@Autowired
	private RefreshTokenRepository refreshTokenRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Override
	public Usuario buscar(Usuario usuario) {
		// TODO Auto-generated method stub
		return repository.getReferenceById(usuario.getId());
	}

	@Override
	public void inserir(Usuario usuario) {
		// Cadastro publico: o cliente nao define privilegios nem o id (id preenchido faria o save sobrescrever outro
		// usuario). Perfil padrao = menor privilegio (VISITANTE); administradores so por promocao/SQL (ver README).
		usuario.setId(null);
		usuario.setAdm(false);
		usuario.setTipoUsuario(TipoUsuario.VISITANTE);
		usuario.setSenha(passwordEncoder.encode(usuario.getSenha()));
		try {
			repository.save(usuario);
		} catch (DataIntegrityViolationException e) {
			// Os campos NOT NULL sao validados na borda (400); a unica restricao UNIQUE da tabela alem da PK
			// (identity) e a do e-mail. Qualquer outra violacao e inesperada e segue como erro (nao vira 409).
			if (violouChaveUnica(e)) {
				throw new EmailJaCadastradoException(e);
			}
			throw e;
		}
	}

	/** Procura na cadeia de causas uma violacao de restricao UNIQUE reportada pelo Hibernate. */
	private static boolean violouChaveUnica(Throwable erro) {
		for (Throwable t = erro; t != null; t = t.getCause() == t ? null : t.getCause()) {
			if (t instanceof ConstraintViolationException
					&& ((ConstraintViolationException) t).getKind() == ConstraintViolationException.ConstraintKind.UNIQUE) {
				return true;
			}
		}
		return false;
	}

	@Override
	@Transactional
	public void alterar(Usuario usuario) {
		// A senha recebida e sempre texto puro (o cliente nunca ve o hash); guarda BCrypt.
		String senha = passwordEncoder.encode(usuario.getSenha());
		Usuario gravado = usuario.getId() == null ? null : repository.findById(usuario.getId()).orElse(null);
		Integer id = usuario.getId();
		if (gravado == null) {
			// Sem registro, vale o padrao do cadastro (o cliente nao define privilegios).
			usuario.setAdm(false);
			usuario.setTipoUsuario(TipoUsuario.VISITANTE);
			usuario.setSenha(senha);
			repository.save(usuario);
		} else {
			// Atualiza a entidade gravada so com os dados cadastrais: adm e tipoUsuario nunca saem do cliente, e como
			// a entidade e @DynamicUpdate o UPDATE nao regrava o perfil (sem lost update com alterarPerfil).
			copiarDadosCadastrais(usuario, gravado);
			gravado.setSenha(senha);
			repository.save(gravado);
		}
		// A senha foi trocada: sessoes (refresh tokens) abertas com a senha anterior deixam de renovar.
		if (id != null) {
			refreshTokenRepository.deleteByIdUsuario(id);
		}
	}

	private static void copiarDadosCadastrais(Usuario origem, Usuario destino) {
		destino.setEmail(origem.getEmail());
		destino.setNome(origem.getNome());
		destino.setEndereco(origem.getEndereco());
		destino.setCelular(origem.getCelular());
		destino.setDataNasc(origem.getDataNasc());
		destino.setSemestre(origem.getSemestre());
		destino.setTurno(origem.getTurno());
		destino.setRg(origem.getRg());
		destino.setGenero(origem.getGenero());
		destino.setMatricula(origem.getMatricula());
		destino.setCurso(origem.getCurso());
	}

	@Override
	@Transactional
	public void excluir(Usuario usuario) {
		exigirOutroAdministrador(repository.findAdministradoresParaAtualizacao(), usuario.getId(),
				"nao e possivel excluir o ultimo administrador");
		refreshTokenRepository.deleteByIdUsuario(usuario.getId());
		repository.delete(usuario);
	}

	@Override
	public List<Usuario> listar() {
		// TODO Auto-generated method stub
		return repository.findAll();
	}

	@Override
	@Transactional
	public Usuario alterarPerfil(Integer id, TipoUsuario tipoUsuario, boolean adm, Integer idAutor) {
		// Lock dos administradores antes de qualquer decisao: rebaixamentos/exclusoes concorrentes sao serializados.
		List<Usuario> administradores = repository.findAdministradoresParaAtualizacao();
		Usuario usuario = repository.findById(id).orElseThrow(UsuarioNaoEncontradoException::new);
		if (!adm && Boolean.TRUE.equals(usuario.getAdm())) {
			if (id.equals(idAutor)) {
				throw new PerfilInvalidoException("o administrador nao pode retirar o proprio acesso");
			}
			exigirOutroAdministrador(administradores, id, "nao e possivel retirar o acesso do ultimo administrador");
		}
		usuario.setTipoUsuario(tipoUsuario);
		usuario.setAdm(adm);
		Usuario salvo = repository.save(usuario);
		// Perfil mudou: as sessoes abertas nao renovam mais com o perfil antigo (o JWT ja emitido vale ate expirar).
		refreshTokenRepository.deleteByIdUsuario(id);
		// Auditoria simples: so ids e o novo perfil (sem nome, e-mail ou outros dados pessoais).
		log.info("perfil alterado: usuario={} tipoUsuario={} adm={} por={}", id, tipoUsuario, adm, idAutor);
		return salvo;
	}

	/** Se o usuario e administrador, precisa existir outro administrador alem dele; senao recusa (ficaria zero). */
	private static void exigirOutroAdministrador(List<Usuario> administradores, Integer id, String mensagem) {
		boolean ehAdmin = id != null && administradores.stream().anyMatch(a -> id.equals(a.getId()));
		if (ehAdmin && administradores.size() <= 1) {
			throw new PerfilInvalidoException(mensagem);
		}
	}

	@Override
	public CrachaUsuario emitirCracha(Usuario usuario) {
		usuario = repository.getReferenceById(usuario.getId());
		CrachaUsuarioMapper crachaUsuarioMapper = new CrachaUsuarioMapper();
		return crachaUsuarioMapper.createFrom(usuario);
	}

}
