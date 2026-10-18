package br.com.fateczs.seazs.repository;

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import br.com.fateczs.seazs.model.RefreshToken;
import jakarta.persistence.LockModeType;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

	/** Bloqueia a linha: dois refresh concorrentes do mesmo token nao podem ambos ter sucesso. */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	Optional<RefreshToken> findByTokenHash(String tokenHash);

	@Modifying
	@Query("update RefreshToken r set r.revogadoEm = :agora where r.familia = :familia and r.revogadoEm is null")
	int revogarFamilia(@Param("familia") String familia, @Param("agora") Instant agora);

	@Transactional
	@Modifying
	@Query("delete from RefreshToken r where r.expiraEm < :limite")
	int deleteByExpiraEmBefore(@Param("limite") Instant limite);

	void deleteByIdUsuario(Integer idUsuario);
}
