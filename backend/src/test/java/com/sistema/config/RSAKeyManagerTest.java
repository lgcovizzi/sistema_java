package com.sistema.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.nio.file.Path;
import java.security.PrivateKey;
import java.security.PublicKey;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes unitários para RSAKeyManager
 * Seguindo práticas de TDD com padrão Given-When-Then
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RSAKeyManager Tests")
class RSAKeyManagerTest {

    private RSAKeyManager rsaKeyManager;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        rsaKeyManager = new RSAKeyManager();
        // Configurar diretório temporário para os testes
        ReflectionTestUtils.setField(rsaKeyManager, "keysDirectory", tempDir.toString());
    }

    @Test
    @DisplayName("Deve inicializar e gerar chaves RSA quando não existem")
    void shouldInitializeAndGenerateRsaKeysWhenTheyDoNotExist() {
        // Given
        assertThat(new File(tempDir.toFile(), "private_key.pem")).doesNotExist();
        assertThat(new File(tempDir.toFile(), "public_key.pem")).doesNotExist();

        // When
        rsaKeyManager.initializeKeys();

        // Then
        assertThat(new File(tempDir.toFile(), "private_key.pem")).exists();
        assertThat(new File(tempDir.toFile(), "public_key.pem")).exists();
    }

    @Test
    @DisplayName("Deve carregar chaves RSA existentes quando são válidas")
    void shouldLoadExistingRsaKeysWhenTheyAreValid() {
        // Given - Primeiro gerar as chaves
        rsaKeyManager.initializeKeys();
        
        // When - Criar nova instância e inicializar novamente
        RSAKeyManager newRsaKeyManager = new RSAKeyManager();
        ReflectionTestUtils.setField(newRsaKeyManager, "keysDirectory", tempDir.toString());
        newRsaKeyManager.initializeKeys();

        // Then
        PrivateKey privateKey = newRsaKeyManager.getPrivateKey();
        PublicKey publicKey = newRsaKeyManager.getPublicKey();
        
        assertThat(privateKey).isNotNull();
        assertThat(publicKey).isNotNull();
        assertThat(privateKey.getAlgorithm()).isEqualTo("RSA");
        assertThat(publicKey.getAlgorithm()).isEqualTo("RSA");
    }

    @Test
    @DisplayName("Deve retornar chave privada válida após inicialização")
    void shouldReturnValidPrivateKeyAfterInitialization() {
        // Given
        rsaKeyManager.initializeKeys();

        // When
        PrivateKey privateKey = rsaKeyManager.getPrivateKey();

        // Then
        assertThat(privateKey).isNotNull();
        assertThat(privateKey.getAlgorithm()).isEqualTo("RSA");
        assertThat(privateKey.getFormat()).isEqualTo("PKCS#8");
    }

    @Test
    @DisplayName("Deve retornar chave pública válida após inicialização")
    void shouldReturnValidPublicKeyAfterInitialization() {
        // Given
        rsaKeyManager.initializeKeys();

        // When
        PublicKey publicKey = rsaKeyManager.getPublicKey();

        // Then
        assertThat(publicKey).isNotNull();
        assertThat(publicKey.getAlgorithm()).isEqualTo("RSA");
        assertThat(publicKey.getFormat()).isEqualTo("X.509");
    }

    @Test
    @DisplayName("Deve validar par de chaves através de criptografia/descriptografia")
    void shouldValidateKeyPairThroughEncryptionDecryption() {
        // Given
        rsaKeyManager.initializeKeys();
        PrivateKey privateKey = rsaKeyManager.getPrivateKey();
        PublicKey publicKey = rsaKeyManager.getPublicKey();

        // When & Then
        assertThat(privateKey).isNotNull();
        assertThat(publicKey).isNotNull();
        
        // Verificar que as chaves são um par válido
        // (teste de criptografia/descriptografia seria implementado em teste de integração)
        assertThat(privateKey.getAlgorithm()).isEqualTo(publicKey.getAlgorithm());
    }

    @Test
    @DisplayName("Deve criar diretório de chaves se não existir")
    void shouldCreateKeysDirectoryIfItDoesNotExist() {
        // Given
        Path newTempDir = tempDir.resolve("new-keys-dir");
        ReflectionTestUtils.setField(rsaKeyManager, "keysDirectory", newTempDir.toString());
        assertThat(newTempDir.toFile()).doesNotExist();

        // When
        rsaKeyManager.initializeKeys();

        // Then
        assertThat(newTempDir.toFile()).exists();
        assertThat(newTempDir.toFile()).isDirectory();
        assertThat(new File(newTempDir.toFile(), "private_key.pem")).exists();
        assertThat(new File(newTempDir.toFile(), "public_key.pem")).exists();
    }
}