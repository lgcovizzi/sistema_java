package com.sistema.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Entidade para gerenciar tokens de refresh com sessão persistente.
 * Permite que usuários mantenham sessões ativas por até 6 meses.
 */
@Entity
@Table(name = "refresh_tokens", indexes = {
    @Index(name = "idx_refresh_token", columnList = "token", unique = true),
    @Index(name = "idx_user_id", columnList = "user_id"),
    @Index(name = "idx_expires_at", columnList = "expires_at")
})
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "token", nullable = false, unique = true, length = 500)
    private String token;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "is_revoked", nullable = false)
    private Boolean isRevoked = false;

    @Column(name = "device_info", length = 255)
    private String deviceInfo;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Construtores
    public RefreshToken() {}

    public RefreshToken(String token, User user, LocalDateTime expiresAt) {
        this.token = token;
        this.user = user;
        this.expiresAt = expiresAt;
        this.isRevoked = false;
    }

    public RefreshToken(String token, User user, LocalDateTime expiresAt, String deviceInfo, String ipAddress, String userAgent) {
        this(token, user, expiresAt);
        this.deviceInfo = deviceInfo;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
    }

    // Métodos de conveniência
    
    /**
     * Verifica se o token está expirado.
     *
     * @return true se o token está expirado
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.expiresAt);
    }

    /**
     * Verifica se o token é válido (não expirado e não revogado).
     *
     * @return true se o token é válido
     */
    public boolean isValid() {
        return !isExpired() && !isRevoked;
    }

    /**
     * Revoga o token.
     */
    public void revoke() {
        this.isRevoked = true;
    }

    /**
     * Atualiza o último uso do token.
     */
    public void updateLastUsed() {
        this.lastUsedAt = LocalDateTime.now();
    }

    /**
     * Verifica se o token foi usado recentemente (últimas 24 horas).
     *
     * @return true se foi usado recentemente
     */
    public boolean isRecentlyUsed() {
        if (lastUsedAt == null) {
            return false;
        }
        return lastUsedAt.isAfter(LocalDateTime.now().minusDays(1));
    }

    /**
     * Calcula quantos dias restam até a expiração.
     *
     * @return número de dias até expiração
     */
    public long getDaysUntilExpiration() {
        if (isExpired()) {
            return 0;
        }
        return java.time.Duration.between(LocalDateTime.now(), expiresAt).toDays();
    }

    /**
     * Obtém informações resumidas do dispositivo.
     *
     * @return string com informações do dispositivo
     */
    public String getDeviceSummary() {
        if (deviceInfo != null && !deviceInfo.trim().isEmpty()) {
            return deviceInfo;
        }
        
        if (userAgent != null && !userAgent.trim().isEmpty()) {
            // Extrai informações básicas do User-Agent
            String ua = userAgent.toLowerCase();
            if (ua.contains("mobile")) {
                return "Mobile Device";
            } else if (ua.contains("tablet")) {
                return "Tablet";
            } else {
                return "Desktop";
            }
        }
        
        return "Unknown Device";
    }

    // Getters e Setters
    
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Boolean getIsRevoked() {
        return isRevoked;
    }

    public void setIsRevoked(Boolean isRevoked) {
        this.isRevoked = isRevoked;
    }

    public String getDeviceInfo() {
        return deviceInfo;
    }

    public void setDeviceInfo(String deviceInfo) {
        this.deviceInfo = deviceInfo;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public LocalDateTime getLastUsedAt() {
        return lastUsedAt;
    }

    public void setLastUsedAt(LocalDateTime lastUsedAt) {
        this.lastUsedAt = lastUsedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    // equals, hashCode e toString
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RefreshToken that = (RefreshToken) o;
        return Objects.equals(id, that.id) && Objects.equals(token, that.token);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, token);
    }

    @Override
    public String toString() {
        return "RefreshToken{" +
                "id=" + id +
                ", token='" + (token != null ? token.substring(0, Math.min(token.length(), 10)) + "..." : "null") + "'" +
                ", userId=" + (user != null ? user.getId() : "null") +
                ", expiresAt=" + expiresAt +
                ", isRevoked=" + isRevoked +
                ", deviceInfo='" + deviceInfo + "'" +
                ", createdAt=" + createdAt +
                '}';
    }
}