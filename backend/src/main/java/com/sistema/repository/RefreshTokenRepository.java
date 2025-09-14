package com.sistema.repository;

import com.sistema.entity.RefreshToken;
import com.sistema.entity.User;
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
 * Repository para gerenciar tokens de refresh.
 * Fornece operações CRUD e consultas específicas para tokens de refresh.
 */
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    /**
     * Busca um token de refresh pelo valor do token.
     *
     * @param token O valor do token
     * @return Optional contendo o RefreshToken se encontrado
     */
    Optional<RefreshToken> findByToken(String token);

    /**
     * Busca um token de refresh válido pelo valor do token.
     *
     * @param token O valor do token
     * @return Optional contendo o RefreshToken se encontrado e válido
     */
    @Query("SELECT rt FROM RefreshToken rt WHERE rt.token = :token AND rt.isRevoked = false AND rt.expiresAt > :now")
    Optional<RefreshToken> findValidByToken(@Param("token") String token, @Param("now") LocalDateTime now);

    /**
     * Busca todos os tokens de refresh de um usuário.
     *
     * @param user O usuário
     * @return Lista de tokens do usuário
     */
    List<RefreshToken> findByUser(User user);

    /**
     * Busca todos os tokens de refresh válidos de um usuário.
     *
     * @param user O usuário
     * @return Lista de tokens válidos do usuário
     */
    @Query("SELECT rt FROM RefreshToken rt WHERE rt.user = :user AND rt.isRevoked = false AND rt.expiresAt > :now ORDER BY rt.lastUsedAt DESC")
    List<RefreshToken> findValidByUser(@Param("user") User user, @Param("now") LocalDateTime now);

    /**
     * Busca tokens de refresh por usuário e informações do dispositivo.
     *
     * @param user O usuário
     * @param deviceInfo Informações do dispositivo
     * @return Lista de tokens do dispositivo
     */
    @Query("SELECT rt FROM RefreshToken rt WHERE rt.user = :user AND rt.deviceInfo = :deviceInfo AND rt.isRevoked = false ORDER BY rt.createdAt DESC")
    List<RefreshToken> findByUserAndDeviceInfo(@Param("user") User user, @Param("deviceInfo") String deviceInfo);

    /**
     * Busca tokens de refresh por usuário e endereço IP.
     *
     * @param user O usuário
     * @param ipAddress Endereço IP
     * @return Lista de tokens do IP
     */
    @Query("SELECT rt FROM RefreshToken rt WHERE rt.user = :user AND rt.ipAddress = :ipAddress AND rt.isRevoked = false ORDER BY rt.createdAt DESC")
    List<RefreshToken> findByUserAndIpAddress(@Param("user") User user, @Param("ipAddress") String ipAddress);

    /**
     * Revoga todos os tokens de refresh de um usuário.
     *
     * @param user O usuário
     * @return Número de tokens revogados
     */
    @Modifying
    @Transactional
    @Query("UPDATE RefreshToken rt SET rt.isRevoked = true WHERE rt.user = :user AND rt.isRevoked = false")
    int revokeAllByUser(@Param("user") User user);

    /**
     * Revoga todos os tokens de refresh de um usuário exceto o token especificado.
     *
     * @param user O usuário
     * @param excludeToken Token a ser excluído da revogação
     * @return Número de tokens revogados
     */
    @Modifying
    @Transactional
    @Query("UPDATE RefreshToken rt SET rt.isRevoked = true WHERE rt.user = :user AND rt.token != :excludeToken AND rt.isRevoked = false")
    int revokeAllByUserExcept(@Param("user") User user, @Param("excludeToken") String excludeToken);

    /**
     * Remove tokens expirados do banco de dados.
     *
     * @param now Data/hora atual
     * @return Número de tokens removidos
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM RefreshToken rt WHERE rt.expiresAt < :now")
    int deleteExpiredTokens(@Param("now") LocalDateTime now);

    /**
     * Remove tokens revogados antigos (mais de 30 dias).
     *
     * @param cutoffDate Data limite para remoção
     * @return Número de tokens removidos
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM RefreshToken rt WHERE rt.isRevoked = true AND rt.updatedAt < :cutoffDate")
    int deleteOldRevokedTokens(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * Conta tokens válidos de um usuário.
     *
     * @param user O usuário
     * @return Número de tokens válidos
     */
    @Query("SELECT COUNT(rt) FROM RefreshToken rt WHERE rt.user = :user AND rt.isRevoked = false AND rt.expiresAt > :now")
    long countValidByUser(@Param("user") User user, @Param("now") LocalDateTime now);

    /**
     * Busca tokens que expiram em breve (próximos 7 dias).
     *
     * @param now Data/hora atual
     * @param soonDate Data limite (7 dias)
     * @return Lista de tokens que expiram em breve
     */
    @Query("SELECT rt FROM RefreshToken rt WHERE rt.isRevoked = false AND rt.expiresAt > :now AND rt.expiresAt <= :soonDate")
    List<RefreshToken> findTokensExpiringSoon(@Param("now") LocalDateTime now, @Param("soonDate") LocalDateTime soonDate);

    /**
     * Busca tokens não utilizados há muito tempo (mais de 30 dias).
     *
     * @param cutoffDate Data limite para último uso
     * @return Lista de tokens inativos
     */
    @Query("SELECT rt FROM RefreshToken rt WHERE rt.isRevoked = false AND (rt.lastUsedAt IS NULL OR rt.lastUsedAt < :cutoffDate)")
    List<RefreshToken> findInactiveTokens(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * Atualiza o último uso de um token.
     *
     * @param token O valor do token
     * @param lastUsedAt Nova data de último uso
     * @return Número de registros atualizados
     */
    @Modifying
    @Transactional
    @Query("UPDATE RefreshToken rt SET rt.lastUsedAt = :lastUsedAt WHERE rt.token = :token")
    int updateLastUsed(@Param("token") String token, @Param("lastUsedAt") LocalDateTime lastUsedAt);

    /**
     * Busca estatísticas de tokens por usuário.
     *
     * @param user O usuário
     * @return Array com [total, válidos, expirados, revogados]
     */
    @Query("SELECT COUNT(rt), " +
           "SUM(CASE WHEN rt.isRevoked = false AND rt.expiresAt > :now THEN 1 ELSE 0 END), " +
           "SUM(CASE WHEN rt.expiresAt <= :now THEN 1 ELSE 0 END), " +
           "SUM(CASE WHEN rt.isRevoked = true THEN 1 ELSE 0 END) " +
           "FROM RefreshToken rt WHERE rt.user = :user")
    Object[] getTokenStatsByUser(@Param("user") User user, @Param("now") LocalDateTime now);

    /**
     * Busca tokens por faixa de datas.
     *
     * @param startDate Data inicial
     * @param endDate Data final
     * @return Lista de tokens criados no período
     */
    @Query("SELECT rt FROM RefreshToken rt WHERE rt.createdAt BETWEEN :startDate AND :endDate ORDER BY rt.createdAt DESC")
    List<RefreshToken> findByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    /**
     * Verifica se existe um token válido para o usuário e dispositivo.
     *
     * @param user O usuário
     * @param deviceInfo Informações do dispositivo
     * @return true se existe token válido
     */
    @Query("SELECT COUNT(rt) > 0 FROM RefreshToken rt WHERE rt.user = :user AND rt.deviceInfo = :deviceInfo AND rt.isRevoked = false AND rt.expiresAt > :now")
    boolean existsValidTokenForUserAndDevice(@Param("user") User user, @Param("deviceInfo") String deviceInfo, @Param("now") LocalDateTime now);

    /**
     * Busca o token mais recente de um usuário.
     *
     * @param user O usuário
     * @return Optional contendo o token mais recente
     */
    @Query("SELECT rt FROM RefreshToken rt WHERE rt.user = :user ORDER BY rt.createdAt DESC LIMIT 1")
    Optional<RefreshToken> findLatestByUser(@Param("user") User user);
}