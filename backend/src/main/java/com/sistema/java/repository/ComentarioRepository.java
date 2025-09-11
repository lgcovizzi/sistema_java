package com.sistema.java.repository;

import com.sistema.java.model.entity.Comentario;
import com.sistema.java.model.entity.Noticia;
import com.sistema.java.model.entity.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository para gerenciamento de comentários
 * 
 * @author Sistema Java
 * @version 1.0
 */
@Repository
public interface ComentarioRepository extends JpaRepository<Comentario, Long> {

    /**
     * Busca comentários por notícia
     * 
     * @param noticia Notícia dos comentários
     * @param pageable Configuração de paginação
     * @return Página de comentários da notícia
     */
    Page<Comentario> findByNoticia(Noticia noticia, Pageable pageable);

    /**
     * Busca comentários aprovados por notícia
     * 
     * @param noticia Notícia dos comentários
     * @param aprovado Status de aprovação
     * @param pageable Configuração de paginação
     * @return Página de comentários aprovados
     */
    Page<Comentario> findByNoticiaAndAprovado(Noticia noticia, boolean aprovado, Pageable pageable);

    /**
     * Busca comentários por autor
     * 
     * @param autor Autor dos comentários
     * @param pageable Configuração de paginação
     * @return Página de comentários do autor
     */
    Page<Comentario> findByAutor(Usuario autor, Pageable pageable);

    /**
     * Busca comentários por status de aprovação
     * 
     * @param aprovado Status de aprovação
     * @param pageable Configuração de paginação
     * @return Página de comentários
     */
    Page<Comentario> findByAprovado(boolean aprovado, Pageable pageable);

    /**
     * Busca comentários pendentes de aprovação ordenados por data
     * 
     * @param pageable Configuração de paginação
     * @return Página de comentários pendentes
     */
    Page<Comentario> findByAprovadoFalseOrderByDataCriacaoAsc(Pageable pageable);

    /**
     * Busca comentários aprovados por notícia ordenados por data
     * 
     * @param noticia Notícia dos comentários
     * @return Lista de comentários aprovados
     */
    List<Comentario> findByNoticiaAndAprovadoTrueOrderByDataCriacaoAsc(Noticia noticia);

    /**
     * Busca comentários por período
     * 
     * @param dataInicio Data de início
     * @param dataFim Data de fim
     * @param pageable Configuração de paginação
     * @return Página de comentários do período
     */
    Page<Comentario> findByDataCriacaoBetween(LocalDateTime dataInicio, LocalDateTime dataFim, Pageable pageable);

    /**
     * Conta comentários por notícia
     * 
     * @param noticia Notícia dos comentários
     * @return Número de comentários da notícia
     */
    long countByNoticia(Noticia noticia);

    /**
     * Conta comentários aprovados por notícia
     * 
     * @param noticia Notícia dos comentários
     * @param aprovado Status de aprovação
     * @return Número de comentários aprovados
     */
    long countByNoticiaAndAprovado(Noticia noticia, boolean aprovado);

    /**
     * Conta comentários por autor
     * 
     * @param autor Autor dos comentários
     * @return Número de comentários do autor
     */
    long countByAutor(Usuario autor);

    /**
     * Conta comentários por status de aprovação
     * 
     * @param aprovado Status de aprovação
     * @return Número de comentários
     */
    long countByAprovado(boolean aprovado);

    /**
     * Busca comentários por termo no conteúdo
     * 
     * @param termo Termo de busca
     * @param aprovado Status de aprovação
     * @param pageable Configuração de paginação
     * @return Página de comentários encontrados
     */
    @Query("SELECT c FROM Comentario c WHERE c.aprovado = :aprovado AND " +
           "LOWER(c.conteudo) LIKE LOWER(CONCAT('%', :termo, '%'))")
    Page<Comentario> buscarPorTermo(@Param("termo") String termo, 
                                   @Param("aprovado") boolean aprovado, 
                                   Pageable pageable);

    /**
     * Busca comentários recentes por notícia
     * 
     * @param noticiaId ID da notícia
     * @param limite Número máximo de comentários
     * @return Lista de comentários recentes
     */
    @Query("SELECT c FROM Comentario c WHERE c.noticia.id = :noticiaId " +
           "AND c.aprovado = true " +
           "ORDER BY c.dataCriacao DESC")
    List<Comentario> findComentariosRecentesByNoticia(@Param("noticiaId") Long noticiaId, 
                                                     Pageable pageable);

    /**
     * Busca usuários mais ativos em comentários
     * 
     * @param aprovado Status de aprovação
     * @param pageable Configuração de paginação
     * @return Lista de usuários ordenados por número de comentários
     */
    @Query("SELECT c.autor FROM Comentario c WHERE c.aprovado = :aprovado " +
           "GROUP BY c.autor " +
           "ORDER BY COUNT(c) DESC")
    List<Usuario> findUsuariosMaisAtivosComentarios(@Param("aprovado") boolean aprovado, 
                                                   Pageable pageable);

    /**
     * Busca estatísticas de comentários por notícia
     * 
     * @return Lista com ID da notícia, título e número de comentários
     */
    @Query("SELECT n.id, n.titulo, COUNT(c) FROM Noticia n LEFT JOIN n.comentarios c " +
           "WHERE c.aprovado = true OR c IS NULL " +
           "GROUP BY n.id, n.titulo " +
           "ORDER BY COUNT(c) DESC")
    List<Object[]> findEstatisticasComentariosPorNoticia();

    /**
     * Busca comentários pendentes com informações da notícia
     * 
     * @param pageable Configuração de paginação
     * @return Página de comentários pendentes com dados da notícia
     */
    @Query("SELECT c FROM Comentario c JOIN FETCH c.noticia JOIN FETCH c.autor " +
           "WHERE c.aprovado = false " +
           "ORDER BY c.dataCriacao ASC")
    Page<Comentario> findComentariosPendentesComDetalhes(Pageable pageable);

    /**
     * Atualiza status de aprovação do comentário
     * 
     * @param id ID do comentário
     * @param aprovado Novo status
     * @return Número de registros atualizados
     */
    @Query("UPDATE Comentario c SET c.aprovado = :aprovado " +
           "WHERE c.id = :id")
    int updateAprovadoById(@Param("id") Long id, @Param("aprovado") boolean aprovado);

    /**
     * Remove comentários antigos não aprovados
     * 
     * @param dataLimite Data limite para remoção
     * @return Número de comentários removidos
     */
    @Query("DELETE FROM Comentario c WHERE c.aprovado = false " +
           "AND c.dataCriacao < :dataLimite")
    int removeComentariosAntigosNaoAprovados(@Param("dataLimite") LocalDateTime dataLimite);

    /**
     * Busca comentários por notícia com paginação otimizada
     * 
     * @param noticiaId ID da notícia
     * @param aprovado Status de aprovação
     * @param pageable Configuração de paginação
     * @return Página de comentários
     */
    @Query("SELECT c FROM Comentario c WHERE c.noticia.id = :noticiaId " +
           "AND c.aprovado = :aprovado " +
           "ORDER BY c.dataCriacao DESC")
    Page<Comentario> findByNoticiaIdAndAprovado(@Param("noticiaId") Long noticiaId, 
                                               @Param("aprovado") boolean aprovado, 
                                               Pageable pageable);
}