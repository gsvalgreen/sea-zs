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
import com.fasterxml.jackson.annotation.JsonProperty;

import br.com.fateczs.seazs.utils.TipoUsuario;
import br.com.fateczs.seazs.utils.Turno;
import br.com.fateczs.seazs.validation.MaxBytes;

@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Entity(name = "Usuario")
@Table(name = "tb_Usuario")
public class Usuario {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_usuario")
	private Integer id;

	@NotNull
	@Column(name = "email_usuario", nullable = false, unique = true)
	private String email;


	@NotNull
	@MaxBytes(MaxBytes.SENHA_BCRYPT)
	@JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
	@Column(name = "senha", nullable = false)
	private String senha;

	
	@Column(name = "nome_usuario", nullable = false)
	private String nome;

	
	@Column(name = "endereco_usuario", nullable = false)
	private String endereco;

	
	@Column(name = "celular_usuario", nullable = false)
	private String celular;

	
	@Temporal(TemporalType.DATE)
	@Column(name = "data_nascimento", nullable = false)
	private Date dataNasc;

	@Column(name = "semestre", nullable = true)
	private Integer semestre;

	@Column(name = "turno")
	private Character turno;

	
	@Column(name = "adm", nullable = false)
	private Boolean adm;

	
	@Column(name = "rg_usuario", nullable = false)
	private String rg;

	
	@Column(name = "genero_usuario", nullable = false)
	private String genero;

	@Column(name = "matricula_usuario")
	private String matricula;

	
	@Column(name = "tipo_usuario", nullable = false)
	private Integer tipoUsuario;

	@ManyToOne(optional = true)
	@JoinColumn(name = "id_curso")
	private Curso curso;

	public Usuario() {	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getSenha() {
		return senha;
	}

	public void setSenha(String senha) {
		this.senha = senha;
	}

	public String getNome() {
		return nome;
	}

	public void setNome(String nome) {
		this.nome = nome;
	}

	public String getEndereco() {
		return endereco;
	}

	public void setEndereco(String endereco) {
		this.endereco = endereco;
	}

	public String getCelular() {
		return celular;
	}

	public void setCelular(String celular) {
		this.celular = celular;
	}

	public Date getDataNasc() {
		return dataNasc;
	}

	public void setDataNasc(Date dataNasc) {
		this.dataNasc = dataNasc;
	}

	public Integer getSemestre() {
		return semestre;
	}

	public void setSemestre(Integer semestre) {
		this.semestre = semestre;
	}

	public Turno getTurno() {
		return Turno.getTurno(turno);
	}

	public void setTurno(Turno turno) {
		if (turno == null) {
			this.turno = null;
		}
		else {
			this.turno = turno.getIdTurno();
		};
	}

	public Boolean getAdm() {
		return adm;
	}

	public void setAdm(Boolean adm) {
		this.adm = adm;
	}

	public String getRg() {
		return rg;
	}

	public void setRg(String rg) {
		this.rg = rg;
	}

	public String getGenero() {
		return genero;
	}

	public void setGenero(String genero) {
		this.genero = genero;
	}

	public String getMatricula() {
		return matricula;
	}

	public void setMatricula(String matricula) {
		this.matricula = matricula;
	}

	public TipoUsuario getTipoUsuario() {
		return TipoUsuario.getTipoUsuario(this.tipoUsuario);
	}

	public void setTipoUsuario(TipoUsuario tipoUsuario) {
		if (tipoUsuario == null) {
			this.tipoUsuario = null;
		}
		else {
			this.tipoUsuario = tipoUsuario.getIdTipoUsuario();
		}
	}

	public Curso getCurso() {
		return curso;
	}

	public void setCurso(Curso curso) {
		this.curso = curso;
	}

	/**
	 * Igualdade por {@code id} (identidade do registro), a abordagem segura para entidades JPA: entidade transitoria
	 * (sem id) so e igual a si mesma, e o hashCode constante nao muda quando o id e gerado no persist. Usa getters
	 * e instanceof para funcionar com proxies do Hibernate.
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!(obj instanceof Usuario))
			return false;
		Integer idOutro = ((Usuario) obj).getId();
		return getId() != null && getId().equals(idOutro);
	}

	@Override
	public int hashCode() {
		return Usuario.class.hashCode();
	}

}
