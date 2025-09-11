package com.sistema.java.repository;

import com.sistema.java.model.entity.Usuario;
import com.sistema.java.model.enums.PapelUsuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository para gerenciamento de usuários
 * 
 * @author Sistema Java
 * @version 1.0
 */
@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    /**
     * Busca usuário por email
     * 
     * @param email Email do usuário
     * @return Optional com o usuário encontrado
     */
    Optional<Usuario> findByEmail(String email);

    /**
     * Busca usuário por email ignorando case
     * 
     * @param email Email do usuário
     * @return Optional com o usuário encontrado
     */
    Optional<Usuario> findByEmailIgnoreCase(String email);

    /**
     * Busca usuário por CPF
     * 
     * @param cpf CPF do usuário
     * @return Optional com o usuário encontrado
     */
    Optional<Usuario> findByCpf(String cpf);

    /**
     * Busca usuários por status ativo ordenados por nome
     * 
     * @param ativo Status ativo
     * @return Lista de usuários ordenados por nome
     */
    List<Usuario> findByAtivoOrderByNome(boolean ativo);

    /**
     * Busca usuário por token de verificação
     * 
     * @param token Token de verificação
     * @return Optional com o usuário encontrado
     */
    Optional<Usuario> findByTokenVerificacao(String token);

    /**
     * Busca usuário por token de reset de senha
     * 
     * @param token Token de reset de senha
     * @return Optional com o usuário encontrado
     */
    Optional<Usuario> findByTokenResetSenha(String token);

    /**
     * Verifica se existe usuário com o email informado
     * 
     * @param email Email a ser verificado
     * @return true se existir, false caso contrário
     */
    boolean existsByEmail(String email);

    /**
     * Verifica se existe usuário com o CPF informado
     * 
     * @param cpf CPF a ser verificado
     * @return true se existir, false caso contrário
     */
    boolean existsByCpf(String cpf);

    /**
     * Verifica se existe usuário com email diferente do ID informado
     * 
     * @param email Email a ser verificado
     * @param id ID do usuário a ser excluído da verificação
     * @return true se existir, false caso contrário
     */
    boolean existsByEmailAndIdNot(String email, Long id);

    /**
     * Verifica se existe usuário com CPF diferente do ID informado
     * 
     * @param cpf CPF a ser verificado
     * @param id ID do usuário a ser excluído da verificação
     * @return true se existir, false caso contrário
     */
    boolean existsByCpfAndIdNot(String cpf, Long id);

    /**
     * Busca usuários ativos
     * 
     * @param ativo Status ativo
     * @param pageable Configuração de paginação
     * @return Página de usuários
     */
    Page<Usuario> findByAtivo(boolean ativo, Pageable pageable);

    /**
     * Busca usuários por nome (case insensitive)
     * 
     * @param nome Nome ou parte do nome
     * @param pageable Configuração de paginação
     * @return Página de usuários
     */
    Page<Usuario> findByNomeContainingIgnoreCase(String nome, Pageable pageable);

    /**
     * Busca usuários por papel
     * 
     * @param papel Papel do usuário
     * @param pageable Configuração de paginação
     * @return Página de usuários
     */
    Page<Usuario> findByPapel(PapelUsuario papel, Pageable pageable);

    /**
     * Busca usuários ativos por papel
     * 
     * @param papel Papel do usuário
     * @param ativo Status ativo
     * @param pageable Configuração de paginação
     * @return Página de usuários
     */
    Page<Usuario> findByPapelAndAtivo(PapelUsuario papel, boolean ativo, Pageable pageable);

    /**
     * Busca usuários ativos por nome
     * 
     * @param nome Nome ou parte do nome
     * @param ativo Status ativo
     * @param pageable Configuração de paginação
     * @return Página de usuários
     */
    Page<Usuario> findByNomeContainingIgnoreCaseAndAtivo(String nome, boolean ativo, Pageable pageable);

    /**
     * Busca usuários criados em um período
     * 
     * @param dataInicio Data de início
     * @param dataFim Data de fim
     * @return Lista de usuários
     */
    List<Usuario> findByDataCriacaoBetween(LocalDateTime dataInicio, LocalDateTime dataFim);

    /**
     * Conta usuários ativos
     * 
     * @return Número de usuários ativos
     */
    long countByAtivo(boolean ativo);

    /**
     * Conta usuários por papel
     * 
     * @param papel Papel do usuário
     * @return Número de usuários com o papel
     */
    long countByPapel(PapelUsuario papel);

    /**
     * Conta usuários verificados
     * 
     * @param emailVerificado Status de verificação
     * @return Número de usuários verificados
     */
    long countByEmailVerificado(boolean emailVerificado);

    /**
     * Busca usuários com notícias publicadas
     * 
     * @return Lista de usuários que têm notícias publicadas
     */
    @Query("SELECT DISTINCT u FROM Usuario u JOIN u.noticias n WHERE n.publicada = true")
    List<Usuario> findUsuariosComNoticiasPublicadas();

    /**
     * Busca usuários por email ou nome
     * 
     * @param termo Termo de busca
     * @param pageable Configuração de paginação
     * @return Página de usuários
     */
    @Query("SELECT u FROM Usuario u WHERE " +
           "LOWER(u.nome) LIKE LOWER(CONCAT('%', :termo, '%')) OR " +
           "LOWER(u.sobrenome) LIKE LOWER(CONCAT('%', :termo, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :termo, '%')) OR " +
           "u.cpf LIKE CONCAT('%', :termo, '%')")
    Page<Usuario> buscarPorEmailOuNome(@Param("termo") String termo, Pageable pageable);

    /**
     * Busca usuários não verificados há mais de X dias
     * 
     * @param dataLimite Data limite para verificação
     * @return Lista de usuários não verificados
     */
    @Query("SELECT u FROM Usuario u WHERE u.emailVerificado = false AND u.dataCriacao < :dataLimite")
    List<Usuario> findUsuariosNaoVerificados(@Param("dataLimite") LocalDateTime dataLimite);

    /**
     * Busca usuários com tokens expirados
     * 
     * @param agora Data/hora atual
     * @return Lista de usuários com tokens expirados
     */
    // @Query("SELECT u FROM Usuario u WHERE (u.tokenVerificacao IS NOT NULL OR u.tokenResetSenha IS NOT NULL) " +
    //        "AND u.dataExpiracaoToken < :agora")
    // List<Usuario> findUsuariosComTokensExpirados(@Param("agora") LocalDateTime agora);

    /**
     * Busca usuários mais ativos (com mais notícias)
     * 
     * @param limite Número máximo de usuários
     * @return Lista de usuários ordenados por número de notícias
     */
    @Query("SELECT u FROM Usuario u LEFT JOIN u.noticias n " +
           "WHERE u.ativo = true " +
           "GROUP BY u " +
           "ORDER BY COUNT(n) DESC")
    List<Usuario> findUsuariosMaisAtivos(Pageable pageable);

    /**
     * Atualiza status ativo do usuário
     * 
     * @param id ID do usuário
     * @param ativo Novo status
     * @return Número de registros atualizados
     */
    @Modifying
    @Transactional
    @Query("UPDATE Usuario u SET u.ativo = :ativo, u.dataAtualizacao = CURRENT_TIMESTAMP " +
           "WHERE u.id = :id")
    int updateAtivoById(@Param("id") Long id, @Param("ativo") boolean ativo);

    /**
     * Atualiza status de verificação de email
     * 
     * @param id ID do usuário
     * @param emailVerificado Status de verificação
     * @return Número de registros atualizados
     */
    @Modifying
    @Transactional
    @Query("UPDATE Usuario u SET u.emailVerificado = :emailVerificado, u.tokenVerificacao = null, " +
           "u.dataExpiracaoToken = null, u.dataAtualizacao = CURRENT_TIMESTAMP WHERE u.id = :id")
    int updateEmailVerificadoById(@Param("id") Long id, @Param("emailVerificado") boolean emailVerificado);

    /**
     * Atualiza último acesso do usuário
     * 
     * @param id ID do usuário
     * @param ultimoAcesso Data/hora do último acesso
     * @return Número de registros atualizados
     */
    @Modifying
    @Transactional
    @Query("UPDATE Usuario u SET u.ultimoAcesso = :ultimoAcesso WHERE u.id = :id")
    int updateUltimoAcessoById(@Param("id") Long id, @Param("ultimoAcesso") LocalDateTime ultimoAcesso);

    /**
     * Limpa tokens expirados
     * 
     * @param agora Data/hora atual
     * @return Número de registros atualizados
     */
    @Modifying
    @Transactional
    @Query("UPDATE Usuario u SET u.tokenVerificacao = null, u.tokenResetSenha = null, " +
           "u.dataExpiracaoToken = null WHERE u.dataExpiracaoToken < :agora")
    int limparTokensExpirados(@Param("agora") LocalDateTime agora);

    /**
     * Busca todos os avatars em uso pelos usuários
     * 
     * @return Lista com os caminhos dos avatars em uso
     */
    @Query("SELECT u.avatar FROM Usuario u WHERE u.avatar IS NOT NULL")
    List<String> findAllAvatarsEmUso();
}