package br.com.fateczs.seazs.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.fateczs.seazs.model.Usuario;

public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {
	
	public Optional<Usuario> findByEmail(String email);
	
	public List<Usuario> findByNome(String nome);
}
