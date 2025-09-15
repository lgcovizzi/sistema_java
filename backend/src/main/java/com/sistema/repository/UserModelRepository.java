package com.sistema.repository;

import com.sistema.model.UserModel;
import com.sistema.model.UserRole;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserModelRepository extends JpaRepository<UserModel, Long> {

    /**
     * Encontra um usuário pelo email
     * @param email o email do usuário
     * @return Optional contendo o usuário se encontrado
     */
    Optional<UserModel> findByEmail(String email);

    /**
     * Verifica se existe um usuário com o email especificado
     * @param email o email a ser verificado
     * @return true se o email já existe, false caso contrário
     */
    boolean existsByEmail(String email);

    /**
     * Encontra usuários por role
     * @param role o role dos usuários
     * @return lista de usuários com o role especificado
     */
    List<UserModel> findByRole(UserRole role);

    /**
     * Encontra usuários por status de verificação de email
     * @param emailVerificado true para usuários verificados, false para não verificados
     * @return lista de usuários com o status especificado
     */
    List<UserModel> findByEmailVerificado(boolean emailVerificado);

    /**
     * Encontra usuários não verificados criados antes de uma data específica
     * @param dataCriacao a data limite
     * @return lista de usuários não verificados criados antes da data
     */
    List<UserModel> findByEmailVerificadoFalseAndDataCriacaoBefore(LocalDateTime dataCriacao);

    /**
     * Conta usuários por role
     * @param role o role a ser contado
     * @return número de usuários com o role especificado
     */
    long countByRole(UserRole role);

    /**
     * Encontra usuários por nome contendo texto (case insensitive)
     * @param nome o texto a ser procurado no nome
     * @return lista de usuários cujo nome contém o texto
     */
    List<UserModel> findByNomeContainingIgnoreCase(String nome);

    /**
     * Encontra usuários por email contendo texto (case insensitive)
     * @param email o texto a ser procurado no email
     * @return lista de usuários cujo email contém o texto
     */
    List<UserModel> findByEmailContainingIgnoreCase(String email);

    /**
     * Deleta usuários não verificados criados antes de uma data específica
     * @param dataCriacao a data limite
     * @return número de usuários deletados
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM UserModel u WHERE u.emailVerificado = false AND u.dataCriacao < :dataCriacao")
    int deleteByEmailVerificadoFalseAndDataCriacaoBefore(@Param("dataCriacao") LocalDateTime dataCriacao);

    /**
     * Encontra usuários por nome completo (nome + sobrenome)
     * @param nomeCompleto o nome completo a ser procurado
     * @return lista de usuários
     */
    @Query("SELECT u FROM UserModel u WHERE CONCAT(u.nome, ' ', u.sobrenome) LIKE %:nomeCompleto%")
    List<UserModel> findByNomeCompleto(@Param("nomeCompleto") String nomeCompleto);

    /**
     * Encontra usuários criados em um período específico
     * @param dataInicio data de início do período
     * @param dataFim data de fim do período
     * @return lista de usuários criados no período
     */
    List<UserModel> findByDataCriacaoBetween(LocalDateTime dataInicio, LocalDateTime dataFim);

    /**
     * Encontra usuários verificados em um período específico
     * @param dataInicio data de início do período
     * @param dataFim data de fim do período
     * @return lista de usuários verificados no período
     */
    List<UserModel> findByEmailVerificadoTrueAndDataVerificacaoEmailBetween(LocalDateTime dataInicio, LocalDateTime dataFim);

    /**
     * Conta usuários não verificados
     * @return número de usuários não verificados
     */
    long countByEmailVerificadoFalse();

    /**
     * Conta usuários verificados
     * @return número de usuários verificados
     */
    long countByEmailVerificadoTrue();

    /**
     * Encontra os usuários mais recentes
     * @param limit número máximo de usuários a retornar
     * @return lista dos usuários mais recentes
     */
    @Query("SELECT u FROM UserModel u ORDER BY u.dataCriacao DESC")
    List<UserModel> findRecentUsers(@Param("limit") int limit);
}