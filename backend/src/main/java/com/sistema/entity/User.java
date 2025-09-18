package com.sistema.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import com.sistema.validation.ValidCpf;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Entidade User para autenticação e autorização.
 * Implementa UserDetails para integração com Spring Security.
 */
@Entity(name = "User")
@Table(name = "users")
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;



    @NotBlank(message = "Password é obrigatório")
    @Size(min = 6, message = "Password deve ter pelo menos 6 caracteres")
    @Column(nullable = false)
    private String password;

    @Email(message = "Email deve ser válido")
    @NotBlank(message = "Email é obrigatório")
    @Column(unique = true, nullable = false)
    private String email;

    @NotBlank(message = "Nome é obrigatório")
    @Size(min = 2, max = 50, message = "Nome deve ter entre 2 e 50 caracteres")
    @Column(name = "first_name", nullable = false)
    private String firstName;

    @NotBlank(message = "Sobrenome é obrigatório")
    @Size(min = 2, max = 50, message = "Sobrenome deve ter entre 2 e 50 caracteres")
    @Column(name = "last_name", nullable = false)
    private String lastName;

    @NotBlank(message = "CPF é obrigatório")
    @ValidCpf(message = "CPF inválido")
    @Column(name = "cpf", unique = true, nullable = false)
    private String cpf;

    @Size(min = 10, max = 15, message = "Telefone deve ter entre 10 e 15 caracteres")
    @Column(name = "phone", nullable = true)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_role")
    private UserRole role = UserRole.USER;

    @Column(name = "account_non_expired")
    private boolean accountNonExpired = true;

    @Column(name = "account_non_locked")
    private boolean accountNonLocked = true;

    @Column(name = "credentials_non_expired")
    private boolean credentialsNonExpired = true;

    @Column(name = "enabled")
    private boolean enabled = true;

    @Column(name = "email_verified")
    private boolean emailVerified = false;

    @Column(name = "verification_token")
    private String verificationToken;

    @Column(name = "verification_token_expires_at")
    private LocalDateTime verificationTokenExpiresAt;

    @Column(name = "reset_password_token")
    private String resetPasswordToken;

    @Column(name = "reset_password_token_expires_at")
    private LocalDateTime resetPasswordTokenExpiresAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "last_login")
    private LocalDateTime lastLogin;

    // Construtores
    public User() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PrePersist
    protected void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public User(String password, String email, String firstName, String lastName, String cpf) {
        this();
        this.password = password;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.cpf = cpf;
        this.role = UserRole.USER;
    }

    public User(String password, String email, String firstName, String lastName, String cpf, String phone) {
        this();
        this.password = password;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.cpf = cpf;
        this.phone = phone;
        this.role = UserRole.USER;
    }

    // Métodos do UserDetails
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role.getAuthority()));
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }



    @Override
    public boolean isAccountNonExpired() {
        return accountNonExpired;
    }

    @Override
    public boolean isAccountNonLocked() {
        return accountNonLocked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return credentialsNonExpired;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    // Getters e Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }





    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getCpf() {
        return cpf;
    }

    public void setCpf(String cpf) {
        this.cpf = cpf;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }



    public void setAccountNonExpired(boolean accountNonExpired) {
        this.accountNonExpired = accountNonExpired;
    }

    public void setAccountNonLocked(boolean accountNonLocked) {
        this.accountNonLocked = accountNonLocked;
    }

    public void setCredentialsNonExpired(boolean credentialsNonExpired) {
        this.credentialsNonExpired = credentialsNonExpired;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
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

    public LocalDateTime getLastLogin() {
        return lastLogin;
    }

    public void setLastLogin(LocalDateTime lastLogin) {
        this.lastLogin = lastLogin;
    }

    public void setLastLoginAt(LocalDateTime lastLoginAt) {
        this.lastLogin = lastLoginAt;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(boolean emailVerified) {
        this.emailVerified = emailVerified;
    }

    public String getVerificationToken() {
        return verificationToken;
    }

    public void setVerificationToken(String verificationToken) {
        this.verificationToken = verificationToken;
    }

    public LocalDateTime getVerificationTokenExpiresAt() {
        return verificationTokenExpiresAt;
    }

    public void setVerificationTokenExpiresAt(LocalDateTime verificationTokenExpiresAt) {
        this.verificationTokenExpiresAt = verificationTokenExpiresAt;
    }

    public String getResetPasswordToken() {
        return resetPasswordToken;
    }

    public void setResetPasswordToken(String resetPasswordToken) {
        this.resetPasswordToken = resetPasswordToken;
    }

    public LocalDateTime getResetPasswordTokenExpiresAt() {
        return resetPasswordTokenExpiresAt;
    }

    public void setResetPasswordTokenExpiresAt(LocalDateTime resetPasswordTokenExpiresAt) {
        this.resetPasswordTokenExpiresAt = resetPasswordTokenExpiresAt;
    }

    // Métodos de conveniência
    public String getFullName() {
        if (firstName != null && lastName != null) {
            return firstName + " " + lastName;
        } else if (firstName != null) {
            return firstName;
        } else if (lastName != null) {
            return lastName;
        }
        return email;
    }



    // Métodos para compatibilidade com UserRole
    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public boolean isAdmin() {
        return this.role == UserRole.ADMIN;
    }

    public boolean isActive() {
        return this.enabled;
    }

    public void setActive(boolean active) {
        this.enabled = active;
    }

    // Métodos de conveniência para verificação de email
    public boolean isVerificationTokenExpired() {
        return verificationTokenExpiresAt != null && 
               LocalDateTime.now().isAfter(verificationTokenExpiresAt);
    }

    public boolean hasValidVerificationToken(String token) {
        return verificationToken != null && 
               verificationToken.equals(token) && 
               !isVerificationTokenExpired();
    }

    public void clearVerificationToken() {
        this.verificationToken = null;
        this.verificationTokenExpiresAt = null;
    }

    // Métodos de conveniência para reset de senha
    public boolean isResetPasswordTokenExpired() {
        return resetPasswordTokenExpiresAt != null && 
               LocalDateTime.now().isAfter(resetPasswordTokenExpiresAt);
    }

    public boolean hasValidResetPasswordToken(String token) {
        return resetPasswordToken != null && 
               resetPasswordToken.equals(token) && 
               !isResetPasswordTokenExpired();
    }

    public void clearResetPasswordToken() {
        this.resetPasswordToken = null;
        this.resetPasswordTokenExpiresAt = null;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", email=" + email +
                ", role=" + role +
                ", active=" + enabled +
                ", createdAt=" + createdAt +
                '}';
    }
}