package br.com.fateczs.seazs.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import br.com.fateczs.seazs.model.Usuario;
import jakarta.persistence.LockModeType;

public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {
	
	public Optional<Usuario> findByEmail(String email);
	
	public List<Usuario> findByNome(String nome);

	/**
	 * Administradores com lock de escrita: serializa operacoes que podem remover o ultimo ADMIN (rebaixar, excluir),
	 * evitando que duas transacoes concorrentes deixem o sistema sem administrador.
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select u from Usuario u where u.adm = true")
	public List<Usuario> findAdministradoresParaAtualizacao();
}
