package com.sistema.java.repository;

import com.sistema.java.model.entity.Categoria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository para gerenciamento de categorias
 * 
 * @author Sistema Java
 * @version 1.0
 */
@Repository
public interface CategoriaRepository extends JpaRepository<Categoria, Long> {

    /**
     * Busca categoria por nome
     * 
     * @param nome Nome da categoria
     * @return Optional com a categoria encontrada
     */
    Optional<Categoria> findByNome(String nome);

    /**
     * Verifica se existe categoria com o nome informado
     * 
     * @param nome Nome a ser verificado
     * @return true se existir, false caso contrário
     */
    boolean existsByNome(String nome);

    /**
     * Busca categorias ativas
     * 
     * @param ativa Status ativo
     * @param pageable Configuração de paginação
     * @return Página de categorias
     */
    Page<Categoria> findByAtiva(boolean ativa, Pageable pageable);

    /**
     * Busca todas as categorias ativas ordenadas por nome
     * 
     * @return Lista de categorias ativas
     */
    List<Categoria> findByAtivaOrderByNome(boolean ativa);

    /**
     * Busca categorias por nome (case insensitive)
     * 
     * @param nome Nome ou parte do nome
     * @param pageable Configuração de paginação
     * @return Página de categorias
     */
    Page<Categoria> findByNomeContainingIgnoreCase(String nome, Pageable pageable);

    /**
     * Busca categorias ativas por nome
     * 
     * @param nome Nome ou parte do nome
     * @param ativa Status ativo
     * @param pageable Configuração de paginação
     * @return Página de categorias
     */
    Page<Categoria> findByNomeContainingIgnoreCaseAndAtiva(String nome, boolean ativa, Pageable pageable);

    /**
     * Conta categorias ativas
     * 
     * @param ativa Status ativo
     * @return Quantidade de categorias ativas
     */
    long countByAtiva(boolean ativa);

    /**
     * Conta categorias por nome (case insensitive)
     * 
     * @param nome Nome a ser pesquisado
     * @return Quantidade de categorias encontradas
     */
    long countByNomeContainingIgnoreCase(String nome);

    /**
     * Conta categorias por nome e status ativo (case insensitive)
     * 
     * @param nome Nome a ser pesquisado
     * @param ativa Status ativo
     * @return Quantidade de categorias encontradas
     */
    long countByNomeContainingIgnoreCaseAndAtiva(String nome, boolean ativa);

    /**
     * Busca categorias com notícias publicadas
     * 
     * @return Lista de categorias que têm notícias publicadas
     */
    @Query("SELECT DISTINCT c FROM Categoria c JOIN c.noticias n " +
           "WHERE c.ativa = true AND n.publicada = true " +
           "ORDER BY c.nome")
    List<Categoria> findCategoriasComNoticiasPublicadas();

    /**
     * Busca categorias mais utilizadas (com mais notícias)
     * 
     * @param limite Número máximo de categorias
     * @return Lista de categorias ordenadas por número de notícias
     */
    @Query("SELECT c FROM Categoria c LEFT JOIN c.noticias n " +
           "WHERE c.ativa = true " +
           "GROUP BY c " +
           "ORDER BY COUNT(n) DESC")
    List<Categoria> findCategoriasMaisUtilizadas(Pageable pageable);

    /**
     * Conta notícias por categoria
     * 
     * @param categoriaId ID da categoria
     * @param publicada Status de publicação das notícias
     * @return Número de notícias da categoria
     */
    @Query("SELECT COUNT(n) FROM Categoria c JOIN c.noticias n " +
           "WHERE c.id = :categoriaId AND n.publicada = :publicada")
    long countNoticiasByCategoria(@Param("categoriaId") Long categoriaId, 
                                 @Param("publicada") boolean publicada);

    /**
     * Busca categorias por termo no nome ou descrição
     * 
     * @param termo Termo de busca
     * @param ativa Status ativo
     * @param pageable Configuração de paginação
     * @return Página de categorias encontradas
     */
    @Query("SELECT c FROM Categoria c WHERE c.ativa = :ativa AND " +
           "(LOWER(c.nome) LIKE LOWER(CONCAT('%', :termo, '%')) OR " +
           "LOWER(c.descricao) LIKE LOWER(CONCAT('%', :termo, '%')))")
    Page<Categoria> buscarPorTermo(@Param("termo") String termo, 
                                  @Param("ativa") boolean ativa, 
                                  Pageable pageable);

    /**
     * Busca estatísticas das categorias
     * 
     * @return Lista com nome da categoria e número de notícias
     */
    @Query("SELECT c.nome, COUNT(n) FROM Categoria c LEFT JOIN c.noticias n " +
           "WHERE c.ativa = true " +
           "GROUP BY c.id, c.nome " +
           "ORDER BY COUNT(n) DESC, c.nome")
    List<Object[]> findEstatisticasCategorias();

    /**
     * Verifica se categoria pode ser excluída (não tem notícias)
     * 
     * @param categoriaId ID da categoria
     * @return true se pode ser excluída, false caso contrário
     */
    @Query("SELECT CASE WHEN COUNT(n) = 0 THEN true ELSE false END " +
           "FROM Categoria c LEFT JOIN c.noticias n " +
           "WHERE c.id = :categoriaId")
    boolean podeSerExcluida(@Param("categoriaId") Long categoriaId);

    /**
     * Atualiza status ativo da categoria
     * 
     * @param id ID da categoria
     * @param ativa Novo status
     * @return Número de registros atualizados
     */
    @Query("UPDATE Categoria c SET c.ativa = :ativa " +
           "WHERE c.id = :id")
    int updateAtivaById(@Param("id") Long id, @Param("ativa") boolean ativa);

    /**
     * Busca categorias ordenadas por nome para seleção
     * 
     * @return Lista de categorias ativas ordenadas
     */
    @Query("SELECT c FROM Categoria c WHERE c.ativa = true ORDER BY c.nome")
    List<Categoria> findAllAtivasOrdenadas();
}