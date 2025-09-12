package com.sistema.java.repository;

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
import java.util.Optional;

/**
 * Repository para gerenciamento de notícias
 * 
 * @author Sistema Java
 * @version 1.0
 */
@Repository
public interface NoticiaRepository extends JpaRepository<Noticia, Long> {

    /**
     * Busca notícias publicadas ordenadas por data de publicação
     * 
     * @param pageable Configuração de paginação
     * @return Página de notícias publicadas
     */
    Page<Noticia> findByPublicadaTrueOrderByDataPublicacaoDesc(Pageable pageable);

    /**
     * Busca notícias por status de publicação
     * 
     * @param publicada Status de publicação
     * @param pageable Configuração de paginação
     * @return Página de notícias
     */
    Page<Noticia> findByPublicada(boolean publicada, Pageable pageable);

    /**
     * Busca notícias por autor
     * 
     * @param autor Autor das notícias
     * @param pageable Configuração de paginação
     * @return Página de notícias do autor
     */
    Page<Noticia> findByAutor(Usuario autor, Pageable pageable);

    /**
     * Busca notícias por autor e status de publicação
     * 
     * @param autor Autor das notícias
     * @param publicada Status de publicação
     * @param pageable Configuração de paginação
     * @return Página de notícias
     */
    Page<Noticia> findByAutorAndPublicada(Usuario autor, boolean publicada, Pageable pageable);

    /**
     * Busca notícias por título (case insensitive)
     * 
     * @param titulo Título ou parte do título
     * @param pageable Configuração de paginação
     * @return Página de notícias
     */
    Page<Noticia> findByTituloContainingIgnoreCase(String titulo, Pageable pageable);

    /**
     * Busca notícias publicadas por título
     * 
     * @param titulo Título ou parte do título
     * @param pageable Configuração de paginação
     * @return Página de notícias publicadas
     */
    Page<Noticia> findByTituloContainingIgnoreCaseAndPublicadaTrue(String titulo, Pageable pageable);

    /**
     * Busca notícias por categoria
     * 
     * @param categoriaId ID da categoria
     * @param publicada Status de publicação
     * @param pageable Configuração de paginação
     * @return Página de notícias da categoria
     */
    @Query("SELECT n FROM Noticia n JOIN n.categorias c " +
           "WHERE c.id = :categoriaId AND n.publicada = :publicada " +
           "ORDER BY n.dataPublicacao DESC")
    Page<Noticia> findByCategoriaAndPublicada(@Param("categoriaId") Long categoriaId, 
                                             @Param("publicada") boolean publicada, 
                                             Pageable pageable);

    /**
     * Busca notícias publicadas em um período
     * 
     * @param dataInicio Data de início
     * @param dataFim Data de fim
     * @param pageable Configuração de paginação
     * @return Página de notícias do período
     */
    Page<Noticia> findByPublicadaTrueAndDataPublicacaoBetween(
        LocalDateTime dataInicio, LocalDateTime dataFim, Pageable pageable);

    /**
     * Busca notícias mais recentes publicadas
     * 
     * @param limite Número máximo de notícias
     * @return Lista de notícias mais recentes
     */
    List<Noticia> findTop10ByPublicadaTrueOrderByDataPublicacaoDesc();

    /**
     * Conta notícias por status de publicação
     * 
     * @param publicada Status de publicação
     * @return Número de notícias
     */
    long countByPublicada(boolean publicada);

    /**
     * Conta notícias por autor
     * 
     * @param autor Autor das notícias
     * @return Número de notícias do autor
     */
    long countByAutor(Usuario autor);

    /**
     * Busca notícias por termo no título ou conteúdo
     * 
     * @param termo Termo de busca
     * @param publicada Status de publicação
     * @param pageable Configuração de paginação
     * @return Página de notícias encontradas
     */
    @Query("SELECT n FROM Noticia n WHERE n.publicada = :publicada AND " +
           "(LOWER(n.titulo) LIKE LOWER(CONCAT('%', :termo, '%')) OR " +
           "LOWER(n.conteudo) LIKE LOWER(CONCAT('%', :termo, '%')) OR " +
           "LOWER(n.resumo) LIKE LOWER(CONCAT('%', :termo, '%')))")
    Page<Noticia> buscarPorTermo(@Param("termo") String termo, 
                                @Param("publicada") boolean publicada, 
                                Pageable pageable);

    /**
     * Busca notícias com mais comentários
     * 
     * @param publicada Status de publicação
     * @param pageable Configuração de paginação
     * @return Página de notícias ordenadas por número de comentários
     */
    @Query("SELECT n FROM Noticia n LEFT JOIN n.comentarios c " +
           "WHERE n.publicada = :publicada " +
           "GROUP BY n " +
           "ORDER BY COUNT(c) DESC")
    Page<Noticia> findNoticiasComMaisComentarios(@Param("publicada") boolean publicada, 
                                                Pageable pageable);

    /**
     * Busca notícias por múltiplas categorias
     * 
     * @param categoriaIds Lista de IDs das categorias
     * @param publicada Status de publicação
     * @param pageable Configuração de paginação
     * @return Página de notícias das categorias
     */
    @Query("SELECT DISTINCT n FROM Noticia n JOIN n.categorias c " +
           "WHERE c.id IN :categoriaIds AND n.publicada = :publicada " +
           "ORDER BY n.dataPublicacao DESC")
    Page<Noticia> findByCategoriasIn(@Param("categoriaIds") List<Long> categoriaIds, 
                                    @Param("publicada") boolean publicada, 
                                    Pageable pageable);

    /**
     * Atualiza status de publicação da notícia
     * 
     * @param id ID da notícia
     * @param publicada Novo status
     * @param dataPublicacao Data de publicação (null para despublicar)
     * @return Número de registros atualizados
     */
    @Query("UPDATE Noticia n SET n.publicada = :publicada, " +
           "n.dataPublicacao = :dataPublicacao, " +
           "n.dataAtualizacao = CURRENT_TIMESTAMP " +
           "WHERE n.id = :id")
    int updatePublicacaoById(@Param("id") Long id, 
                           @Param("publicada") boolean publicada, 
                           @Param("dataPublicacao") LocalDateTime dataPublicacao);

    /**
     * Conta notícias por título contendo texto e status de publicação
     * 
     * @param titulo Texto a ser buscado no título
     * @param publicada Status de publicação
     * @return Número de notícias encontradas
     */
    long countByTituloContainingIgnoreCaseAndPublicada(String titulo, boolean publicada);

    /**
     * Conta notícias por categoria e status de publicação
     * 
     * @param categoriaId ID da categoria
     * @param publicada Status de publicação
     * @return Número de notícias encontradas
     */
    @Query("SELECT COUNT(n) FROM Noticia n JOIN n.categorias c " +
           "WHERE c.id = :categoriaId AND n.publicada = :publicada")
    long countByCategoriasIdAndPublicada(@Param("categoriaId") Long categoriaId, 
                                        @Param("publicada") boolean publicada);
}