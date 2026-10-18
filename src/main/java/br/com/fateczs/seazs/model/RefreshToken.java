package br.com.fateczs.seazs.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Refresh token opaco. Apenas o hash SHA-256 do token e armazenado (um vazamento do banco nao permite usar os
 * tokens). Uso unico: a cada refresh o token e revogado e outro da mesma {@code familia} e emitido (rotacao);
 * reapresentar um token revogado indica roubo e revoga a familia inteira.
 */
@Entity(name = "RefreshToken")
@Table(name = "tb_refresh_token")
public class RefreshToken {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private Long id;

	@Column(name = "token_hash", nullable = false, unique = true, length = 64)
	private String tokenHash;

	@Column(name = "familia", nullable = false, length = 36)
	private String familia;

	@Column(name = "id_usuario", nullable = false)
	private Integer idUsuario;

	@Column(name = "criado_em", nullable = false)
	private Instant criadoEm;

	@Column(name = "expira_em", nullable = false)
	private Instant expiraEm;

	@Column(name = "revogado_em")
	private Instant revogadoEm;

	public RefreshToken() {
	}

	public RefreshToken(String tokenHash, String familia, Integer idUsuario, Instant criadoEm, Instant expiraEm) {
		this.tokenHash = tokenHash;
		this.familia = familia;
		this.idUsuario = idUsuario;
		this.criadoEm = criadoEm;
		this.expiraEm = expiraEm;
	}

	public Long getId() {
		return id;
	}

	public String getTokenHash() {
		return tokenHash;
	}

	public String getFamilia() {
		return familia;
	}

	public Integer getIdUsuario() {
		return idUsuario;
	}

	public Instant getCriadoEm() {
		return criadoEm;
	}

	public Instant getExpiraEm() {
		return expiraEm;
	}

	public Instant getRevogadoEm() {
		return revogadoEm;
	}

	public void setRevogadoEm(Instant revogadoEm) {
		this.revogadoEm = revogadoEm;
	}

	public boolean estaRevogado() {
		return revogadoEm != null;
	}

	public boolean estaExpirado(Instant agora) {
		return !expiraEm.isAfter(agora);
	}
}
