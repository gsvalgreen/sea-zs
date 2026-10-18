package br.com.fateczs.seazs.model;

import java.util.Date;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.validation.constraints.NotNull;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Entity
@Table(name = "tb_Evento")
public class Evento {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_evento")
	private Integer id;
	
	@NotNull
	@Column(name = "nome_evento", nullable = false)
	private String nome;
	
	@Column(name = "descricao_evento", nullable = true)
	private String descricao;
	
	@ManyToOne(optional = true)
	@JoinColumn(name = "id_curso")
	private Curso cursoEvento;
	
	@NotNull
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "data_inicio", nullable = false)
	private Date inicio;
	
	@NotNull
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "data_fim", nullable = false)
	private Date fim;
	
//	@NotNull
	@Temporal(TemporalType.DATE)
	@Column(name = "data_inclusao", nullable = false)
	private Date dataInclusao;
	
	@ManyToOne(optional = false)
	// Leitura aberta a qualquer autenticado: nao devolve dados pessoais do usuario (so id, nome, turno, semestre, tipo e curso).
	@JsonIgnoreProperties(value = { "email", "endereco", "celular", "dataNasc", "rg", "genero", "matricula", "adm", "hibernateLazyInitializer", "handler" }, allowSetters = true)
	@JoinColumn(name = "id_usuario_criador")
	private Usuario usuarioCriador;
	
	public Evento () {	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getNome() {
		return nome;
	}

	public void setNome(String nome) {
		this.nome = nome;
	}

	public String getDescricao() {
		return descricao;
	}

	public void setDescricao(String descricao) {
		this.descricao = descricao;
	}

	public Curso getCursoEvento() {
		return cursoEvento;
	}

	public void setCursoEvento(Curso cursoEvento) {
		this.cursoEvento = cursoEvento;
	}

	public Date getInicio() {
		return inicio;
	}

	public void setInicio(Date inicio) {
		this.inicio = inicio;
	}

	public Date getFim() {
		return fim;
	}

	public void setFim(Date fim) {
		this.fim = fim;
	}

	public Date getDataInclusao() {
		return dataInclusao;
	}

	public void setDataInclusao(Date dataInclusao) {
		this.dataInclusao = dataInclusao;
	}

	public Usuario getUsuarioCriador() {
		return usuarioCriador;
	}

	public void setUsuarioCriador(Usuario usuarioCriador) {
		this.usuarioCriador = usuarioCriador;
	}
	
	
}
