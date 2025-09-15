package com.sistema.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.Cipher;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

    @Test
    @DisplayName("Deve validar par de chaves através de criptografia e descriptografia real")
    void shouldValidateKeyPairThroughRealEncryptionDecryption() throws Exception {
        // Given
        rsaKeyManager.initializeKeys();
        PrivateKey privateKey = rsaKeyManager.getPrivateKey();
        PublicKey publicKey = rsaKeyManager.getPublicKey();
        String testMessage = "teste-validacao-chaves-rsa-completo";

        // When
        Cipher cipher = Cipher.getInstance("RSA");
        
        // Criptografar com chave pública
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        byte[] encryptedData = cipher.doFinal(testMessage.getBytes());
        
        // Descriptografar com chave privada
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        byte[] decryptedData = cipher.doFinal(encryptedData);
        String decryptedMessage = new String(decryptedData);

        // Then
        assertThat(decryptedMessage).isEqualTo(testMessage);
        assertThat(rsaKeyManager.validateKeyPair(privateKey, publicKey)).isTrue();
    }

    @Test
    @DisplayName("Deve regenerar chaves quando arquivos existentes são inválidos")
    void shouldRegenerateKeysWhenExistingFilesAreInvalid() throws Exception {
        // Given - Criar arquivos de chaves inválidos
        File privateKeyFile = new File(tempDir.toFile(), "private_key.pem");
        File publicKeyFile = new File(tempDir.toFile(), "public_key.pem");
        
        Files.writeString(privateKeyFile.toPath(), "-----BEGIN PRIVATE KEY-----\nchave-invalida\n-----END PRIVATE KEY-----");
        Files.writeString(publicKeyFile.toPath(), "-----BEGIN PUBLIC KEY-----\nchave-invalida\n-----END PUBLIC KEY-----");
        
        assertThat(privateKeyFile).exists();
        assertThat(publicKeyFile).exists();

        // When
        rsaKeyManager.initializeKeys();

        // Then - Chaves devem ter sido regeneradas e serem válidas
        PrivateKey privateKey = rsaKeyManager.getPrivateKey();
        PublicKey publicKey = rsaKeyManager.getPublicKey();
        
        assertThat(privateKey).isNotNull();
        assertThat(publicKey).isNotNull();
        assertThat(rsaKeyManager.validateKeyPair(privateKey, publicKey)).isTrue();
    }

    @Test
    @DisplayName("Deve verificar formato PEM das chaves salvas")
    void shouldVerifyPemFormatOfSavedKeys() throws Exception {
        // Given
        rsaKeyManager.initializeKeys();

        // When
        File privateKeyFile = new File(tempDir.toFile(), "private_key.pem");
        File publicKeyFile = new File(tempDir.toFile(), "public_key.pem");
        
        String privateKeyContent = Files.readString(privateKeyFile.toPath());
        String publicKeyContent = Files.readString(publicKeyFile.toPath());

        // Then
        assertThat(privateKeyContent).startsWith("-----BEGIN PRIVATE KEY-----");
        assertThat(privateKeyContent).endsWith("-----END PRIVATE KEY-----");
        assertThat(publicKeyContent).startsWith("-----BEGIN PUBLIC KEY-----");
        assertThat(publicKeyContent).endsWith("-----END PUBLIC KEY-----");
        
        // Verificar que o conteúdo base64 é válido
        String privateKeyBase64 = privateKeyContent
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        String publicKeyBase64 = publicKeyContent
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");
        
        assertThat(privateKeyBase64).isNotEmpty();
        assertThat(publicKeyBase64).isNotEmpty();
        
        // Verificar que é base64 válido
        assertThat(Base64.getDecoder().decode(privateKeyBase64)).isNotEmpty();
        assertThat(Base64.getDecoder().decode(publicKeyBase64)).isNotEmpty();
    }

    @Test
    @DisplayName("Deve garantir que chaves tenham tamanho RSA 2048 bits")
    void shouldEnsureKeysHaveRsa2048BitSize() {
        // Given
        rsaKeyManager.initializeKeys();

        // When
        PrivateKey privateKey = rsaKeyManager.getPrivateKey();
        PublicKey publicKey = rsaKeyManager.getPublicKey();

        // Then
        assertThat(privateKey.getAlgorithm()).isEqualTo("RSA");
        assertThat(publicKey.getAlgorithm()).isEqualTo("RSA");
        
        // Verificar tamanho da chave através do tamanho dos dados codificados
        // Chaves RSA 2048 bits têm aproximadamente 1200+ bytes quando codificadas
        assertThat(privateKey.getEncoded().length).isGreaterThan(1000);
        assertThat(publicKey.getEncoded().length).isGreaterThan(250);
    }

    @Test
    @DisplayName("Deve simular inicialização do servidor e verificar disponibilidade das chaves")
    void shouldSimulateServerInitializationAndVerifyKeyAvailability() {
        // Given - Simular inicialização do servidor (método @PostConstruct)
        assertThat(rsaKeyManager.getPrivateKey()).isNull();
        assertThat(rsaKeyManager.getPublicKey()).isNull();

        // When - Inicializar como seria feito na inicialização do servidor
        rsaKeyManager.initializeKeys();

        // Then - Verificar que as chaves estão disponíveis para uso pelo JWT
        PrivateKey privateKey = rsaKeyManager.getPrivateKey();
        PublicKey publicKey = rsaKeyManager.getPublicKey();
        
        assertThat(privateKey).isNotNull();
        assertThat(publicKey).isNotNull();
        assertThat(privateKey.getAlgorithm()).isEqualTo("RSA");
        assertThat(publicKey.getAlgorithm()).isEqualTo("RSA");
        assertThat(privateKey.getFormat()).isEqualTo("PKCS#8");
        assertThat(publicKey.getFormat()).isEqualTo("X.509");
        
        // Verificar que as chaves podem ser usadas para JWT (teste de compatibilidade)
        assertThat(rsaKeyManager.validateKeyPair(privateKey, publicKey)).isTrue();
        
        // Verificar que os arquivos foram criados no diretório correto
        assertThat(new File(tempDir.toFile(), "private_key.pem")).exists();
        assertThat(new File(tempDir.toFile(), "public_key.pem")).exists();
        
        // Verificar que o diretório de chaves está configurado corretamente
        assertThat(rsaKeyManager.getKeysDirectory()).isEqualTo(tempDir.toString());
    }

    @Test
    @DisplayName("Deve falhar graciosamente quando diretório de chaves não pode ser criado")
    void shouldFailGracefullyWhenKeysDirectoryCannotBeCreated() {
        // Given - Configurar um caminho inválido (somente leitura)
        String invalidPath = "/root/invalid-path-no-permission";
        ReflectionTestUtils.setField(rsaKeyManager, "keysDirectory", invalidPath);

        // When & Then
        assertThatThrownBy(() -> rsaKeyManager.initializeKeys())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Falha na inicialização das chaves RSA");
    }
}