package com.sistema.repository;

import com.sistema.entity.User;
import com.sistema.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository para operações de banco de dados da entidade User.
 * Fornece métodos para autenticação e gerenciamento de usuários.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Busca usuário por username.
     * Usado para autenticação.
     * 
     * @param username o nome de usuário
     * @return Optional contendo o usuário se encontrado
     */
    Optional<User> findByUsername(String username);

    /**
     * Busca usuário por email.
     * Usado para recuperação de senha e validações.
     * 
     * @param email o email do usuário
     * @return Optional contendo o usuário se encontrado
     */
    Optional<User> findByEmail(String email);

    /**
     * Busca usuário por CPF.
     * Usado para recuperação de senha e validações.
     * 
     * @param cpf o CPF do usuário
     * @return Optional contendo o usuário se encontrado
     */
    Optional<User> findByCpf(String cpf);

    /**
     * Busca usuário por username ou email.
     * Útil para login flexível.
     * 
     * @param username o nome de usuário
     * @param email o email
     * @return Optional contendo o usuário se encontrado
     */
    Optional<User> findByUsernameOrEmail(String username, String email);

    /**
     * Verifica se existe usuário com o username.
     * 
     * @param username o nome de usuário
     * @return true se existir
     */
    boolean existsByUsername(String username);

    /**
     * Verifica se existe usuário com o email.
     * 
     * @param email o email
     * @return true se existir
     */
    boolean existsByEmail(String email);

    /**
     * Verifica se existe usuário com o CPF.
     * 
     * @param cpf o CPF
     * @return true se existir
     */
    boolean existsByCpf(String cpf);

    /**
     * Busca usuários por role específico.
     * 
     * @param role o papel do usuário
     * @return lista de usuários com o papel especificado
     */
    List<User> findByRole(UserRole role);

    /**
     * Busca usuários ativos (enabled = true).
     * 
     * @return lista de usuários ativos
     */
    List<User> findByEnabledTrue();

    /**
     * Busca usuários inativos (enabled = false).
     * 
     * @return lista de usuários inativos
     */
    List<User> findByEnabledFalse();

    /**
     * Busca usuários criados após uma data específica.
     * 
     * @param date a data de referência
     * @return lista de usuários criados após a data
     */
    List<User> findByCreatedAtAfter(LocalDateTime date);

    /**
     * Busca usuários que fizeram login após uma data específica.
     * 
     * @param date a data de referência
     * @return lista de usuários com login recente
     */
    List<User> findByLastLoginAfter(LocalDateTime date);

    /**
     * Atualiza o último login do usuário.
     * 
     * @param userId o ID do usuário
     * @param lastLogin a data/hora do último login
     */
    @Modifying
    @Query("UPDATE User u SET u.lastLogin = :lastLogin WHERE u.id = :userId")
    void updateLastLogin(@Param("userId") Long userId, @Param("lastLogin") LocalDateTime lastLogin);

    /**
     * Ativa ou desativa um usuário.
     * 
     * @param userId o ID do usuário
     * @param active true para ativar, false para desativar
     */
    @Modifying
    @Query("UPDATE User u SET u.enabled = :enabled WHERE u.id = :userId")
    void updateUserStatus(@Param("userId") Long userId, @Param("enabled") boolean enabled);

    /**
     * Conta usuários por role.
     * 
     * @param role o papel do usuário
     * @return número de usuários com o papel especificado
     */
    long countByRole(UserRole role);

    /**
     * Conta usuários ativos.
     * 
     * @return número de usuários ativos
     */
    long countByEnabledTrue();

    /**
     * Verifica se é o primeiro usuário (banco vazio).
     * Usado para determinar se o usuário deve ser automaticamente promovido a administrador.
     * 
     * @return true se não há usuários no banco, false caso contrário
     */
    @Query("SELECT CASE WHEN COUNT(u) = 0 THEN true ELSE false END FROM User u")
    boolean isFirstUser();

    /**
     * Encontra o primeiro usuário criado (ordenado por data de criação).
     * 
     * @return Optional contendo o primeiro usuário criado
     */
    Optional<User> findFirstByOrderByCreatedAtAsc();

    /**
     * Busca usuários por parte do nome ou email (busca flexível).
     * 
     * @param searchTerm termo de busca
     * @return lista de usuários que correspondem ao termo
     */
    @Query("SELECT u FROM User u WHERE " +
           "LOWER(u.username) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(u.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(u.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<User> searchUsers(@Param("searchTerm") String searchTerm);

    /**
     * Busca usuários ordenados por data de criação (mais recentes primeiro).
     * 
     * @return lista de usuários ordenada por data de criação
     */
    List<User> findAllByOrderByCreatedAtDesc();

    /**
     * Busca usuários ordenados por último login (mais recentes primeiro).
     * 
     * @return lista de usuários ordenada por último login
     */
    List<User> findAllByOrderByLastLoginDesc();
}