package br.com.fateczs.seazs.model.dto;

import java.util.List;

/** Dados minimos do usuario autenticado devolvidos junto com os tokens (sem e-mail, senha ou dados pessoais). */
public record UsuarioResumoResponse(Integer id, String nome, String tipoUsuario, List<String> authorities) {
}
