package br.com.fateczs.seazs.service.serviceImpl;

import java.util.List;

import br.com.fateczs.seazs.model.CrachaUsuario;
import br.com.fateczs.seazs.model.mapper.CrachaUsuarioMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.fateczs.seazs.model.Usuario;
import br.com.fateczs.seazs.repository.RefreshTokenRepository;
import br.com.fateczs.seazs.repository.UsuarioRepository;
import br.com.fateczs.seazs.service.UsuarioService;

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
		usuario.setSenha(passwordEncoder.encode(usuario.getSenha()));
		repository.save(usuario);
	}

	@Override
	public void alterar(Usuario usuario) {
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
