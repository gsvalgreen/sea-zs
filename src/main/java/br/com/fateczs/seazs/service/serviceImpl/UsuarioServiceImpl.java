package br.com.fateczs.seazs.service.serviceImpl;

import java.util.List;

import br.com.fateczs.seazs.model.CrachaUsuario;
import br.com.fateczs.seazs.model.mapper.CrachaUsuarioMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.fateczs.seazs.model.Usuario;
import br.com.fateczs.seazs.repository.RefreshTokenRepository;
import br.com.fateczs.seazs.repository.UsuarioRepository;
import br.com.fateczs.seazs.service.EmailJaCadastradoException;
import br.com.fateczs.seazs.service.UsuarioService;
import br.com.fateczs.seazs.utils.TipoUsuario;

@Service
public class UsuarioServiceImpl implements UsuarioService {

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
	public void alterar(Usuario usuario) {
		// adm e tipoUsuario nao mudam por aqui: preserva o que esta gravado (sem registro, vale o padrao do cadastro).
		Usuario gravado = usuario.getId() == null ? null : repository.findById(usuario.getId()).orElse(null);
		if (gravado == null) {
			usuario.setAdm(false);
			usuario.setTipoUsuario(TipoUsuario.VISITANTE);
		} else {
			usuario.setAdm(gravado.getAdm());
			usuario.setTipoUsuario(gravado.getTipoUsuario());
		}
		// A senha recebida e sempre texto puro (o cliente nunca ve o hash); guarda BCrypt.
		usuario.setSenha(passwordEncoder.encode(usuario.getSenha()));
		repository.save(usuario);
	}

	@Override
	@Transactional
	public void excluir(Usuario usuario) {
		refreshTokenRepository.deleteByIdUsuario(usuario.getId());
		repository.delete(usuario);
	}

	@Override
	public List<Usuario> listar() {
		// TODO Auto-generated method stub
		return repository.findAll();
	}

	@Override
	public CrachaUsuario emitirCracha(Usuario usuario) {
		usuario = repository.getReferenceById(usuario.getId());
		CrachaUsuarioMapper crachaUsuarioMapper = new CrachaUsuarioMapper();
		return crachaUsuarioMapper.createFrom(usuario);
	}

}
