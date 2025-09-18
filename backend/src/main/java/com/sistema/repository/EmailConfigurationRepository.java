package com.sistema.repository;

import com.sistema.entity.EmailConfiguration;
import com.sistema.enums.EmailProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositório para operações de banco de dados da entidade EmailConfiguration.
 */
@Repository
public interface EmailConfigurationRepository extends JpaRepository<EmailConfiguration, Long> {

    /**
     * Busca a configuração padrão ativa.
     * 
     * @return Optional com a configuração padrão
     */
    @Query("SELECT ec FROM EmailConfiguration ec WHERE ec.isDefault = true AND ec.enabled = true")
    Optional<EmailConfiguration> findDefaultConfiguration();

    /**
     * Busca configurações por provedor.
     * 
     * @param provider o provedor de email
     * @return lista de configurações do provedor
     */
    List<EmailConfiguration> findByProvider(EmailProvider provider);

    /**
     * Busca configurações ativas por provedor.
     * 
     * @param provider o provedor de email
     * @return lista de configurações ativas do provedor
     */
    List<EmailConfiguration> findByProviderAndEnabledTrue(EmailProvider provider);

    /**
     * Busca todas as configurações ativas.
     * 
     * @return lista de configurações ativas
     */
    List<EmailConfiguration> findByEnabledTrue();

    /**
     * Verifica se existe uma configuração padrão ativa.
     * 
     * @return true se existe configuração padrão ativa
     */
    @Query("SELECT COUNT(ec) > 0 FROM EmailConfiguration ec WHERE ec.isDefault = true AND ec.enabled = true")
    boolean existsDefaultConfiguration();

    /**
     * Remove o status de padrão de todas as configurações.
     */
    @Modifying
    @Query("UPDATE EmailConfiguration ec SET ec.isDefault = false")
    void clearAllDefaultFlags();

    /**
     * Define uma configuração como padrão.
     * 
     * @param id o ID da configuração
     */
    @Modifying
    @Query("UPDATE EmailConfiguration ec SET ec.isDefault = true WHERE ec.id = :id")
    void setAsDefault(@Param("id") Long id);

    /**
     * Busca configurações por username.
     * 
     * @param username o username
     * @return lista de configurações com o username
     */
    List<EmailConfiguration> findByUsername(String username);

    /**
     * Busca configuração por provedor e username.
     * 
     * @param provider o provedor
     * @param username o username
     * @return Optional com a configuração
     */
    Optional<EmailConfiguration> findByProviderAndUsername(EmailProvider provider, String username);

    /**
     * Conta configurações ativas por provedor.
     * 
     * @param provider o provedor
     * @return número de configurações ativas
     */
    long countByProviderAndEnabledTrue(EmailProvider provider);

    /**
     * Busca configurações ordenadas por data de criação.
     * 
     * @return lista ordenada de configurações
     */
    @Query("SELECT ec FROM EmailConfiguration ec ORDER BY ec.createdAt DESC")
    List<EmailConfiguration> findAllOrderByCreatedAtDesc();

    /**
     * Busca configurações ativas ordenadas por padrão primeiro.
     * 
     * @return lista ordenada de configurações ativas
     */
    @Query("SELECT ec FROM EmailConfiguration ec WHERE ec.enabled = true ORDER BY ec.isDefault DESC, ec.createdAt DESC")
    List<EmailConfiguration> findEnabledOrderByDefaultFirst();
}