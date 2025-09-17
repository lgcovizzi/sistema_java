package com.sistema.service;

import com.sistema.entity.User;
import com.sistema.entity.UserRole;
import com.sistema.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EmailVerificationService Tests")
class EmailVerificationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private EmailVerificationService emailVerificationService;

    private User testUser;
    private User verifiedUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setFirstName("João");
        testUser.setLastName("Silva");
        testUser.setEmail("test@example.com");
        testUser.setPassword("hashedPassword");
        testUser.setCpf("12345678901");
        testUser.setRole(UserRole.USER);
        testUser.setEnabled(true);
        testUser.setEmailVerified(false);
        testUser.setCreatedAt(LocalDateTime.now());
        testUser.setUpdatedAt(LocalDateTime.now());

        verifiedUser = new User();
        verifiedUser.setId(2L);
        verifiedUser.setFirstName("Maria");
        verifiedUser.setLastName("Santos");
        verifiedUser.setEmail("verified@example.com");
        verifiedUser.setPassword("hashedPassword");
        verifiedUser.setCpf("98765432100");
        verifiedUser.setRole(UserRole.USER);
        verifiedUser.setEnabled(true);
        verifiedUser.setEmailVerified(true);
        verifiedUser.setCreatedAt(LocalDateTime.now());
        verifiedUser.setUpdatedAt(LocalDateTime.now());

        // Configurar propriedades via reflection
        ReflectionTestUtils.setField(emailVerificationService, "tokenExpirationHours", 24);
        ReflectionTestUtils.setField(emailVerificationService, "emailVerificationEnabled", true);
    }

    @Test
    @DisplayName("Deve gerar token de verificação e enviar email")
    void shouldGenerateVerificationTokenAndSendEmail() {
        // Arrange
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(emailService.sendVerificationEmail(any(User.class), anyString())).thenReturn(true);

        // Act
        String token = emailVerificationService.generateVerificationToken(testUser);

        // Assert
        assertThat(token).isNotNull().isNotEmpty();
        verify(userRepository).save(testUser);
        verify(emailService).sendVerificationEmail(eq(testUser), anyString());
    }

    @Test
    @DisplayName("Deve lançar exceção quando usuário é nulo")
    void shouldThrowExceptionWhenUserIsNull() {
        // When & Then
        assertThatThrownBy(() -> emailVerificationService.generateVerificationToken(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("user");

        verify(emailService, never()).sendVerificationEmail(any(User.class), anyString());
    }

    @Test
    @DisplayName("Deve verificar token com sucesso")
    void shouldVerifyTokenSuccessfully() {
        // Arrange
        String token = "valid-token";
        testUser.setVerificationToken(token);
        testUser.setVerificationTokenExpiresAt(LocalDateTime.now().plusHours(1));
        
        when(userRepository.findByVerificationToken(token)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act
        boolean result = emailVerificationService.verifyEmailToken(token);

        // Assert
        assertThat(result).isTrue();
        verify(userRepository).findByVerificationToken(token);
        verify(userRepository).save(testUser);
    }

    @Test
    @DisplayName("Deve retornar false quando token não existe")
    void shouldReturnFalseWhenTokenNotExists() {
        // Arrange
        String token = "non-existent-token";
        when(userRepository.findByVerificationToken(token)).thenReturn(Optional.empty());

        // Act
        boolean result = emailVerificationService.verifyEmailToken(token);

        // Assert
        assertThat(result).isFalse();
        verify(userRepository).findByVerificationToken(token);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Deve retornar false quando token está expirado")
    void shouldReturnFalseWhenTokenIsExpired() {
        // Arrange
        String token = "expired-token";
        testUser.setVerificationToken(token);
        testUser.setVerificationTokenExpiresAt(LocalDateTime.now().minusHours(1)); // Expirado
        
        when(userRepository.findByVerificationToken(token)).thenReturn(Optional.of(testUser));

        // Act
        boolean result = emailVerificationService.verifyEmailToken(token);

        // Assert
        assertThat(result).isFalse();
        verify(userRepository).findByVerificationToken(token);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Deve verificar se usuário precisa verificar email")
    void shouldCheckIfUserNeedsEmailVerification() {
        // Act & Assert
        boolean needsVerification = emailVerificationService.needsEmailVerification(testUser);
        assertThat(needsVerification).isTrue();

        boolean verifiedUserNeedsVerification = emailVerificationService.needsEmailVerification(verifiedUser);
        assertThat(verifiedUserNeedsVerification).isFalse();
    }

    @Test
    @DisplayName("Deve regenerar token de verificação")
    void shouldRegenerateVerificationToken() {
        // Arrange
        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(emailService.sendVerificationEmail(any(User.class), anyString())).thenReturn(true);

        // Act
        String token = emailVerificationService.regenerateVerificationToken(testUser.getEmail());

        // Assert
        assertThat(token).isNotNull().isNotEmpty();
        verify(userRepository).findByEmail(testUser.getEmail());
        verify(userRepository).save(testUser);
        verify(emailService).sendVerificationEmail(eq(testUser), anyString());
    }

    @Test
    @DisplayName("Deve retornar null ao regenerar token para usuário já verificado")
    void shouldReturnNullWhenRegeneratingTokenForVerifiedUser() {
        // Arrange
        when(userRepository.findByEmail(verifiedUser.getEmail())).thenReturn(Optional.of(verifiedUser));

        // Act
        String token = emailVerificationService.regenerateVerificationToken(verifiedUser.getEmail());

        // Assert
        assertThat(token).isNull();
        verify(userRepository).findByEmail(verifiedUser.getEmail());
        verify(userRepository, never()).save(any(User.class));
        verify(emailService, never()).sendVerificationEmail(any(User.class), anyString());
    }

    @Test
    @DisplayName("Deve buscar usuário por token")
    void shouldFindUserByToken() {
        // Arrange
        String token = "valid-token";
        when(userRepository.findByVerificationToken(token)).thenReturn(Optional.of(testUser));

        // Act
        Optional<User> result = emailVerificationService.findUserByVerificationToken(token);

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(testUser);
        verify(userRepository).findByVerificationToken(token);
    }

    @Test
    @DisplayName("Deve retornar empty quando usuário não encontrado por token")
    void shouldReturnEmptyWhenUserNotFoundByToken() {
        // Arrange
        String token = "non-existent-token";
        when(userRepository.findByVerificationToken(token)).thenReturn(Optional.empty());

        // Act
        Optional<User> result = emailVerificationService.findUserByVerificationToken(token);

        // Assert
        assertThat(result).isEmpty();
        verify(userRepository).findByVerificationToken(token);
    }

    @Test
    @DisplayName("Deve listar usuários não verificados")
    void shouldListUnverifiedUsers() {
        // Arrange
        List<User> unverifiedUsers = Arrays.asList(testUser);
        when(userRepository.findByEmailVerifiedFalse()).thenReturn(unverifiedUsers);

        // Act
        List<User> result = emailVerificationService.findUnverifiedUsers();

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(testUser);
        verify(userRepository).findByEmailVerifiedFalse();
    }

    @Test
    @DisplayName("Deve limpar tokens expirados")
    void shouldCleanupExpiredTokens() {
        // Arrange
        User userWithExpiredToken = new User();
        userWithExpiredToken.setVerificationToken("expired-token");
        userWithExpiredToken.setVerificationTokenExpiresAt(LocalDateTime.now().minusHours(1));
        
        List<User> usersWithExpiredTokens = Arrays.asList(userWithExpiredToken);
        when(userRepository.findUsersWithExpiredVerificationTokens(any(LocalDateTime.class)))
                .thenReturn(usersWithExpiredTokens);
        when(userRepository.save(any(User.class))).thenReturn(userWithExpiredToken);

        // Act
        int count = emailVerificationService.cleanupExpiredTokens();

        // Assert
        assertThat(count).isEqualTo(1);
        verify(userRepository).findUsersWithExpiredVerificationTokens(any(LocalDateTime.class));
        verify(userRepository).save(userWithExpiredToken);
    }

    @Test
    @DisplayName("Deve verificar se verificação está habilitada")
    void shouldCheckIfVerificationIsEnabled() {
        // Act
        boolean isEnabled = emailVerificationService.isEmailVerificationEnabled();

        // Assert
        assertThat(isEnabled).isTrue();
    }

    @Test
    @DisplayName("Deve obter estatísticas de verificação")
    void shouldGetVerificationStatistics() {
        // Arrange
        when(userRepository.count()).thenReturn(10L);
        when(userRepository.countByEmailVerifiedTrue()).thenReturn(7L);
        when(userRepository.countByEmailVerifiedFalse()).thenReturn(3L);

        // Act
        EmailVerificationService.EmailVerificationStats stats = emailVerificationService.getVerificationStats();

        // Assert
        assertThat(stats.getTotalUsers()).isEqualTo(10L);
        assertThat(stats.getVerifiedUsers()).isEqualTo(7L);
        assertThat(stats.getUnverifiedUsers()).isEqualTo(3L);
        assertThat(stats.getVerificationRate()).isEqualTo(70.0);
    }

    @Test
    @DisplayName("Deve validar entrada nula")
    void shouldValidateNullInput() {
        // When & Then
        assertThatThrownBy(() -> emailVerificationService.verifyEmailToken(null))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> emailVerificationService.findUserByVerificationToken(null))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> emailVerificationService.regenerateVerificationToken(null))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> emailVerificationService.needsEmailVerification(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Deve validar entrada vazia")
    void shouldValidateEmptyInput() {
        // When & Then
        assertThatThrownBy(() -> emailVerificationService.verifyEmailToken(""))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> emailVerificationService.findUserByVerificationToken("   "))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> emailVerificationService.regenerateVerificationToken(""))
                .isInstanceOf(IllegalArgumentException.class);
    }
}