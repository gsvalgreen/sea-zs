package br.com.fateczs.seazs.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsPasswordService;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.fateczs.seazs.model.Usuario;
import br.com.fateczs.seazs.repository.UsuarioRepository;

/** Carrega o usuario por e-mail e persiste o rehash BCrypt (upgrade de senhas legadas) apos login bem-sucedido. */
@Service
public class SeazsUserDetailsService implements UserDetailsService, UserDetailsPasswordService {

	@Autowired
	private UsuarioRepository repository;

	@Override
	@Transactional(readOnly = true)
	public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
		return repository.findByEmail(email).map(UsuarioAutenticado::new)
				.orElseThrow(() -> new UsernameNotFoundException("usuario nao encontrado"));
	}

	/** {@code novaSenha} ja vem codificada e com prefixo ({bcrypt}...), pronta para persistir. */
	@Override
	@Transactional
	public UserDetails updatePassword(UserDetails user, String novaSenha) {
		Usuario usuario = ((UsuarioAutenticado) user).getUsuario();
		usuario.setSenha(novaSenha);
		return new UsuarioAutenticado(repository.save(usuario));
	}
}
