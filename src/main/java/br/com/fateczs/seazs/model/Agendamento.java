package br.com.fateczs.seazs.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.validation.constraints.NotNull;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Entity
@Table(name = "tb_Agendamento")
public class Agendamento implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_Agendamento")
	private Integer id;
	
	@NotNull
	@ManyToOne
	@JoinColumn(name = "id_evento")
	private Evento evento;
	
	// Opcional no corpo: o organizador e definido no servidor (sub do JWT no inserir; preservado no alterar).
	@ManyToOne(optional = false)
	// Leitura aberta a qualquer autenticado: nao devolve dados pessoais do usuario (so id, nome, turno, semestre, tipo e curso).
	@JsonIgnoreProperties(value = { "email", "endereco", "celular", "dataNasc", "rg", "genero", "matricula", "adm", "hibernateLazyInitializer", "handler" }, allowSetters = true)
	@JoinColumn(name = "id_usuario_organizador")
	private Usuario usuarioOrganizador;
	
	@NotNull
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "data_inicio", nullable = false)
	private Date inicio;
	
	@NotNull
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "data_fim", nullable = false)
	private Date fim;
	
	@NotNull
	@Column(name = "pontuacaoStaff", nullable = false)
	private Integer pontuacaoStaff;
	
	
	@OneToMany(fetch = FetchType.LAZY, mappedBy = "agendamento")
	//@JsonManagedReference
	@JsonIgnoreProperties("agendamento")
	private List<Atividade> atividades = new ArrayList<>();
	
	public Agendamento () { }

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public Evento getEvento() {
		return evento;
	}

	public void setEvento(Evento evento) {
		this.evento = evento;
	}

	public Usuario getUsuarioOrganizador() {
		return usuarioOrganizador;
	}

	public void setUsuarioOrganizador(Usuario usuarioOrganizador) {
		this.usuarioOrganizador = usuarioOrganizador;
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

	public Integer getPontuacaoStaff() {
		return pontuacaoStaff;
	}

	public void setPontuacaoStaff(Integer pontuacaoStaff) {
		this.pontuacaoStaff = pontuacaoStaff;
	}

	public List<Atividade> getAtividades() {
		return atividades;
	}

	public void setAtividades(List<Atividade> atividades) {
		this.atividades = atividades;
	}
	
	
}
