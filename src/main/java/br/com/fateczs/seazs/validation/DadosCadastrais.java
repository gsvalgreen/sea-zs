package br.com.fateczs.seazs.validation;

/**
 * Grupo de validacao dos campos obrigatorios do cadastro de {@code Usuario} (colunas NOT NULL que o cliente informa).
 * Fica fora do grupo padrao para nao exigir esses campos em operacoes que so usam o id (buscar, excluir...).
 * Usado no cadastro e na alteracao: sem isso a violacao de NOT NULL so apareceria no banco (500/409 enganoso).
 */
public interface DadosCadastrais {
}
