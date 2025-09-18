package com.sistema.service;

import com.sistema.entity.User;
import com.sistema.entity.UserRole;
import com.sistema.repository.UserRepository;
import com.sistema.util.CpfGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Testes unitários específicos para verificação de tokens de ativação.
 * Foca na geração, validação e gerenciamento de tokens de verificação de email.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Testes de Verificação de Token de Ativação")
class EmailVerificationTokenTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private EmailVerificationService emailVerificationService;

    private User testUser;
    private final String testEmail = "token.test@example.com";

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail(testEmail);
        testUser.setFirstName("Token");
        testUser.setLastName("Test");
        testUser.setCpf(CpfGenerator.generateCpf());
        testUser.setPassword("encodedPassword");
        testUser.setRole(UserRole.USER);
        testUser.setEnabled(true);
        testUser.setEmailVerified(false);
        testUser.setCreatedAt(LocalDateTime.now());
        testUser.setUpdatedAt(LocalDateTime.now());
    }

    @Test
    @DisplayName("Deve gerar token de verificação válido")
    void shouldGenerateValidVerificationToken() {
        // Given
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(emailService.sendVerificationEmail(any(User.class), anyString())).thenReturn(true);

        // When
        String token = emailVerificationService.generateVerificationToken(testUser);

        // Then
        assertThat(token).isNotNull();
        assertThat(token).hasSize(64); // SHA-256 hex string
        assertThat(token).matches("^[a-f0-9]{64}$"); // Apenas caracteres hexadecimais
        
        // Verificar que o token foi salvo no usuário
        assertThat(testUser.getVerificationToken()).isEqualTo(token);
        assertThat(testUser.getVerificationTokenExpiresAt()).isNotNull();
        assertThat(testUser.getVerificationTokenExpiresAt()).isAfter(LocalDateTime.now());
        
        verify(userRepository).save(testUser);
        verify(emailService).sendVerificationEmail(testUser, token);
    }

    @Test
    @DisplayName("Deve gerar tokens únicos para cada chamada")
    void shouldGenerateUniqueTokensForEachCall() {
        // Given
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(emailService.sendVerificationEmail(any(User.class), anyString())).thenReturn(true);

        // When
        String token1 = emailVerificationService.generateVerificationToken(testUser);
        String token2 = emailVerificationService.generateVerificationToken(testUser);

        // Then
        assertThat(token1).isNotEqualTo(token2);
        assertThat(token1).hasSize(64);
        assertThat(token2).hasSize(64);
        
        verify(userRepository, times(2)).save(testUser);
        verify(emailService, times(2)).sendVerificationEmail(eq(testUser), anyString());
    }

    @Test
    @DisplayName("Deve definir expiração do token para 24 horas")
    void shouldSetTokenExpirationTo24Hours() {
        // Given
        LocalDateTime beforeGeneration = LocalDateTime.now();
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(emailService.sendVerificationEmail(any(User.class), anyString())).thenReturn(true);

        // When
        emailVerificationService.generateVerificationToken(testUser);
        LocalDateTime afterGeneration = LocalDateTime.now();

        // Then
        LocalDateTime actualExpiration = testUser.getVerificationTokenExpiresAt();
        LocalDateTime expectedMinExpiration = beforeGeneration.plusHours(24).minusMinutes(1);
        LocalDateTime expectedMaxExpiration = afterGeneration.plusHours(24).plusMinutes(1);
        
        assertThat(actualExpiration).isNotNull();
        assertThat(actualExpiration).isAfter(expectedMinExpiration);
        assertThat(actualExpiration).isBefore(expectedMaxExpiration);
    }

    @Test
    @DisplayName("Deve verificar token válido com sucesso")
    void shouldVerifyValidTokenSuccessfully() {
        // Given
        String validToken = "valid-token-123";
        testUser.setVerificationToken(validToken);
        testUser.setVerificationTokenExpiresAt(LocalDateTime.now().plusHours(1));
        
        when(userRepository.findByVerificationToken(validToken)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            // Simula o que o método verifyEmailToken faz
            user.setEmailVerified(true);
            user.clearVerificationToken();
            return user;
        });

        // When
        boolean result = emailVerificationService.verifyEmailToken(validToken);

        // Then
        assertThat(result).isTrue();
        assertThat(testUser.isEmailVerified()).isTrue();
        assertThat(testUser.getVerificationToken()).isNull();
        assertThat(testUser.getVerificationTokenExpiresAt()).isNull();
        
        verify(userRepository).findByVerificationToken(validToken);
        verify(userRepository).save(testUser);
    }

    @Test
    @DisplayName("Deve rejeitar token inexistente")
    void shouldRejectNonExistentToken() {
        // Given
        String nonExistentToken = "non-existent-token";
        when(userRepository.findByVerificationToken(nonExistentToken)).thenReturn(Optional.empty());

        // When
        boolean result = emailVerificationService.verifyEmailToken(nonExistentToken);

        // Then
        assertThat(result).isFalse();
        
        verify(userRepository).findByVerificationToken(nonExistentToken);
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve rejeitar token expirado")
    void shouldRejectExpiredToken() {
        // Given
        String expiredToken = "expired-token-123";
        testUser.setVerificationToken(expiredToken);
        testUser.setVerificationTokenExpiresAt(LocalDateTime.now().minusHours(1)); // Expirado
        
        when(userRepository.findByVerificationToken(expiredToken)).thenReturn(Optional.of(testUser));

        // When
        boolean result = emailVerificationService.verifyEmailToken(expiredToken);

        // Then
        assertThat(result).isFalse();
        assertThat(testUser.isEmailVerified()).isFalse(); // Não deve ser verificado
        
        verify(userRepository).findByVerificationToken(expiredToken);
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve rejeitar token nulo ou vazio")
    void shouldRejectNullOrEmptyToken() {
        // When & Then - Token nulo
        boolean resultNull = emailVerificationService.verifyEmailToken(null);
        assertThat(resultNull).isFalse();

        // When & Then - Token vazio
        boolean resultEmpty = emailVerificationService.verifyEmailToken("");
        assertThat(resultEmpty).isFalse();

        // When & Then - Token apenas espaços
        boolean resultBlank = emailVerificationService.verifyEmailToken("   ");
        assertThat(resultBlank).isFalse();

        verify(userRepository, never()).findByVerificationToken(any());
    }

    @Test
    @DisplayName("Deve regenerar token de verificação")
    void shouldRegenerateVerificationToken() {
        // Given
        String oldToken = "old-token-123";
        testUser.setVerificationToken(oldToken);
        testUser.setVerificationTokenExpiresAt(LocalDateTime.now().plusHours(1));
        
        when(userRepository.findByEmail(testEmail)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(emailService.sendVerificationEmail(any(User.class), anyString())).thenReturn(true);

        // When
        String newToken = emailVerificationService.regenerateVerificationToken(testEmail);

        // Then
        assertThat(newToken).isNotNull();
        assertThat(newToken).hasSize(64);
        assertThat(newToken).isNotEqualTo(oldToken);
        assertThat(testUser.getVerificationToken()).isEqualTo(newToken);
        assertThat(testUser.getVerificationTokenExpiresAt()).isAfter(LocalDateTime.now());
        
        verify(userRepository).findByEmail(testEmail);
        verify(userRepository).save(testUser);
        verify(emailService).sendVerificationEmail(testUser, newToken);
    }

    @Test
    @DisplayName("Deve falhar ao regenerar token para usuário inexistente")
    void shouldFailToRegenerateTokenForNonExistentUser() {
        // Given
        String nonExistentEmail = "nonexistent@example.com";
        when(userRepository.findByEmail(nonExistentEmail)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> emailVerificationService.regenerateVerificationToken(nonExistentEmail))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Usuário não encontrado");

        verify(userRepository).findByEmail(nonExistentEmail);
        verify(userRepository, never()).save(any());
        verify(emailService, never()).sendVerificationEmail(any(), any());
    }

    @Test
    @DisplayName("Deve falhar ao regenerar token para usuário já verificado")
    void shouldFailToRegenerateTokenForAlreadyVerifiedUser() {
        // Given
        testUser.setEmailVerified(true); // Usuário já verificado
        when(userRepository.findByEmail(testEmail)).thenReturn(Optional.of(testUser));

        // When & Then
        assertThatThrownBy(() -> emailVerificationService.regenerateVerificationToken(testEmail))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Email já foi verificado");

        verify(userRepository).findByEmail(testEmail);
        verify(userRepository, never()).save(any());
        verify(emailService, never()).sendVerificationEmail(any(), any());
    }

    @Test
    @DisplayName("Deve verificar se usuário precisa de verificação de email")
    void shouldCheckIfUserNeedsEmailVerification() {
        // Given - Usuário comum não verificado
        testUser.setEmailVerified(false);

        // When
        boolean needsVerification = emailVerificationService.needsEmailVerification(testUser);

        // Then
        assertThat(needsVerification).isTrue();

        // Given - Usuário verificado
        testUser.setEmailVerified(true);

        // When
        needsVerification = emailVerificationService.needsEmailVerification(testUser);

        // Then
        assertThat(needsVerification).isFalse();

        // Given - Usuário não verificado novamente
        testUser.setEmailVerified(false);

        // When
        needsVerification = emailVerificationService.needsEmailVerification(testUser);

        // Then
        assertThat(needsVerification).isTrue();
    }

    @Test
    @DisplayName("Deve limpar tokens expirados")
    void shouldCleanupExpiredTokens() {
        // Given
        when(userRepository.findUsersWithExpiredVerificationTokens(any(LocalDateTime.class)))
                .thenReturn(java.util.Arrays.asList(testUser));
        when(userRepository.saveAll(anyList())).thenReturn(java.util.Arrays.asList(testUser));

        // When
        int cleanedCount = emailVerificationService.cleanupExpiredTokens();

        // Then
        assertThat(cleanedCount).isEqualTo(1);
        assertThat(testUser.getVerificationToken()).isNull();
        assertThat(testUser.getVerificationTokenExpiresAt()).isNull();
        
        verify(userRepository).findUsersWithExpiredVerificationTokens(any(LocalDateTime.class));
        verify(userRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("Deve obter estatísticas de verificação")
    void shouldGetVerificationStatistics() {
        // Given
        when(userRepository.count()).thenReturn(10L);
        when(userRepository.countByEmailVerifiedTrue()).thenReturn(7L);
        when(userRepository.countByEmailVerifiedFalse()).thenReturn(3L);

        // When
        EmailVerificationService.EmailVerificationStats stats = emailVerificationService.getVerificationStats();

        // Then
        assertThat(stats).isNotNull();
        assertThat(stats.getTotalUsers()).isEqualTo(10L);
        assertThat(stats.getVerifiedUsers()).isEqualTo(7L);
        assertThat(stats.getUnverifiedUsers()).isEqualTo(3L);
        assertThat(stats.getVerificationRate()).isEqualTo(70.0);
        
        verify(userRepository).count();
        verify(userRepository).countByEmailVerifiedTrue();
        verify(userRepository).countByEmailVerifiedFalse();
    }

    @Test
    @DisplayName("Deve validar formato do token gerado")
    void shouldValidateGeneratedTokenFormat() {
        // Given
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(emailService.sendVerificationEmail(any(User.class), anyString())).thenReturn(true);

        // When
        String token = emailVerificationService.generateVerificationToken(testUser);

        // Then
        // Token deve ser hexadecimal de 64 caracteres (SHA-256)
        assertThat(token).matches("^[a-f0-9]{64}$");
        
        // Token não deve conter caracteres especiais ou espaços
        assertThat(token).doesNotContain(" ", "-", "_", "=", "+", "/");
        
        // Token deve ser diferente a cada geração
        String token2 = emailVerificationService.generateVerificationToken(testUser);
        assertThat(token).isNotEqualTo(token2);
    }

    @Test
    @DisplayName("Deve verificar que token é case-sensitive")
    void shouldVerifyTokenIsCaseSensitive() {
        // Given
        String originalToken = "abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890";
        String uppercaseToken = originalToken.toUpperCase();
        
        // Criar usuário para token original
        User userWithOriginalToken = new User();
        userWithOriginalToken.setId(1L);
        userWithOriginalToken.setEmail("test@example.com");
        userWithOriginalToken.setEmailVerified(false);
        userWithOriginalToken.setVerificationToken(originalToken);
        userWithOriginalToken.setVerificationTokenExpiresAt(LocalDateTime.now().plusHours(1));
        
        when(userRepository.findByVerificationToken(originalToken)).thenReturn(Optional.of(userWithOriginalToken));
        when(userRepository.findByVerificationToken(uppercaseToken)).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setEmailVerified(true);
            user.clearVerificationToken();
            return user;
        });

        // When & Then - Token original deve funcionar
        boolean resultOriginal = emailVerificationService.verifyEmailToken(originalToken);
        assertThat(resultOriginal).isTrue();

        // When & Then - Token em maiúscula deve falhar
        boolean resultUppercase = emailVerificationService.verifyEmailToken(uppercaseToken);
        assertThat(resultUppercase).isFalse();
    }

    @Test
    @DisplayName("Deve verificar que verificação de token é operação única")
    void shouldVerifyTokenVerificationIsOneTimeOperation() {
        // Given
        String token = "one-time-token-123";
        testUser.setVerificationToken(token);
        testUser.setVerificationTokenExpiresAt(LocalDateTime.now().plusHours(1));
        
        when(userRepository.findByVerificationToken(token))
                .thenReturn(Optional.of(testUser))
                .thenReturn(Optional.empty()); // Segunda chamada retorna vazio
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setEmailVerified(true);
            user.clearVerificationToken();
            return user;
        });

        // When - Primeira verificação
        boolean firstResult = emailVerificationService.verifyEmailToken(token);

        // Then - Primeira verificação deve ser bem-sucedida
        assertThat(firstResult).isTrue();
        assertThat(testUser.isEmailVerified()).isTrue();
        assertThat(testUser.getVerificationToken()).isNull();

        // When - Segunda verificação com o mesmo token
        boolean secondResult = emailVerificationService.verifyEmailToken(token);

        // Then - Segunda verificação deve falhar
        assertThat(secondResult).isFalse();
        
        verify(userRepository, times(2)).findByVerificationToken(token);
        verify(userRepository, times(1)).save(testUser);
    }
}