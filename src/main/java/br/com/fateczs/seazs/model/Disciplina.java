package br.com.fateczs.seazs.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Entity(name = "Disciplina")
@Table(name = "tb_Disciplina")
public class Disciplina {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_disciplina")
	private Integer id;
	
	@NotNull
	@Column(name = "codigo_siga", nullable = false)
	private String codigoSiga;
	
	@NotNull
	@Column(name = "nome_disciplina", nullable = false)
	private String nome;
	
	@ManyToOne(optional = false)
	@JoinColumn(name = "id_curso")
	private Curso curso;
	
	public Disciplina() {}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getCodigoSiga() {
		return codigoSiga;
	}

	public void setCodigoSiga(String codigoSiga) {
		this.codigoSiga = codigoSiga;
	}

	public String getNome() {
		return nome;
	}

	public void setNome(String nome) {
		this.nome = nome;
	}

	public Curso getCurso() {
		return curso;
	}

	public void setCurso(Curso curso) {
		this.curso = curso;
	}
	
	
	
}
