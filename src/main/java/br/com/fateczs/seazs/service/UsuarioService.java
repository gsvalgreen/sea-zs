package br.com.fateczs.seazs.service;

import java.util.List;

import br.com.fateczs.seazs.model.CrachaUsuario;
import br.com.fateczs.seazs.model.Usuario;
import br.com.fateczs.seazs.utils.TipoUsuario;

public interface UsuarioService {

	Usuario buscar(Usuario usuario);
	
	void inserir(Usuario usuario);
	
	void alterar(Usuario usuario);
	
	void excluir(Usuario usuario);
	
	List<Usuario> listar();
	
	CrachaUsuario emitirCracha(Usuario usuario);

	/**
	 * Define o perfil ({@code tipoUsuario}) e o flag {@code adm} de um usuario. Unico caminho que altera esses
	 * campos; o chamador ja foi autorizado como ADMIN. Registra em log quem mudou o perfil de quem (so ids).
	 *
	 * @throws UsuarioNaoEncontradoException se o id nao existe
	 * @throws PerfilInvalidoException       se o proprio administrador tentar retirar o seu {@code adm}
	 */
	Usuario alterarPerfil(Integer id, TipoUsuario tipoUsuario, boolean adm, Integer idAutor);
	
}
