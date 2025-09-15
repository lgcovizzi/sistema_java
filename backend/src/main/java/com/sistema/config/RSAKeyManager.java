package com.sistema.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import javax.crypto.Cipher;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * Gerenciador de chaves RSA para o sistema.
 * Responsável por gerar, validar e carregar chaves RSA na inicialização da aplicação.
 */
@Component
public class RSAKeyManager {

    private static final Logger logger = LoggerFactory.getLogger(RSAKeyManager.class);
    private static final int KEY_SIZE = 2048;
    private static final String ALGORITHM = "RSA";
    private static final String PRIVATE_KEY_FILE = "private_key.pem";
    private static final String PUBLIC_KEY_FILE = "public_key.pem";

    @Value("${app.rsa.keys.directory:./keys}")
    private String keysDirectory;

    private PrivateKey privateKey;
    private PublicKey publicKey;

    /**
     * Inicializa o gerenciador de chaves RSA.
     * Verifica se as chaves existem e são válidas, caso contrário gera novas chaves.
     */
    @PostConstruct
    public void initializeKeys() {
        try {
            createKeysDirectoryIfNotExists();
            
            if (keysExist()) {
                logger.info("Chaves RSA encontradas. Verificando validade...");
                if (loadAndValidateKeys()) {
                    logger.info("Chaves RSA válidas carregadas com sucesso.");
                } else {
                    logger.warn("Chaves RSA inválidas. Gerando novas chaves...");
                    generateNewKeys();
                }
            } else {
                logger.info("Chaves RSA não encontradas. Gerando novas chaves...");
                generateNewKeys();
            }
        } catch (Exception e) {
            logger.error("Erro ao inicializar chaves RSA: {}", e.getMessage(), e);
            throw new RuntimeException("Falha na inicialização das chaves RSA", e);
        }
    }

    /**
     * Verifica se os arquivos de chaves existem.
     */
    private boolean keysExist() {
        Path privateKeyPath = Paths.get(keysDirectory, PRIVATE_KEY_FILE);
        Path publicKeyPath = Paths.get(keysDirectory, PUBLIC_KEY_FILE);
        return Files.exists(privateKeyPath) && Files.exists(publicKeyPath);
    }

    /**
     * Carrega e valida as chaves existentes.
     */
    private boolean loadAndValidateKeys() {
        try {
            // Carrega as chaves
            privateKey = loadPrivateKey();
            publicKey = loadPublicKey();

            // Valida as chaves testando criptografia/descriptografia
            return validateKeyPair(privateKey, publicKey);
        } catch (Exception e) {
            logger.error("Erro ao carregar chaves: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Gera um novo par de chaves RSA.
     */
    private void generateNewKeys() throws NoSuchAlgorithmException, IOException {
        logger.info("Gerando novo par de chaves RSA de {} bits...", KEY_SIZE);
        
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(ALGORITHM);
        keyPairGenerator.initialize(KEY_SIZE);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();

        privateKey = keyPair.getPrivate();
        publicKey = keyPair.getPublic();

        // Salva as chaves em arquivos
        savePrivateKey(privateKey);
        savePublicKey(publicKey);

        logger.info("Novo par de chaves RSA gerado e salvo com sucesso.");
    }

    /**
     * Valida se o par de chaves funciona corretamente.
     */
    public boolean validateKeyPair(PrivateKey privateKey, PublicKey publicKey) {
        try {
            // Teste de criptografia/descriptografia
            String testMessage = "teste-validacao-chaves-rsa";
            
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            
            // Criptografa com a chave pública
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
            byte[] encryptedData = cipher.doFinal(testMessage.getBytes());
            
            // Descriptografa com a chave privada
            cipher.init(Cipher.DECRYPT_MODE, privateKey);
            byte[] decryptedData = cipher.doFinal(encryptedData);
            
            String decryptedMessage = new String(decryptedData);
            boolean isValid = testMessage.equals(decryptedMessage);
            
            if (isValid) {
                logger.debug("Validação das chaves RSA bem-sucedida.");
            } else {
                logger.warn("Falha na validação das chaves RSA: mensagens não coincidem.");
            }
            
            return isValid;
        } catch (Exception e) {
            logger.error("Erro durante validação das chaves RSA: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Carrega a chave privada do arquivo.
     */
    private PrivateKey loadPrivateKey() throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        Path keyPath = Paths.get(keysDirectory, PRIVATE_KEY_FILE);
        String keyContent = Files.readString(keyPath)
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        
        byte[] keyBytes = Base64.getDecoder().decode(keyContent);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance(ALGORITHM);
        return keyFactory.generatePrivate(keySpec);
    }

    /**
     * Carrega a chave pública do arquivo.
     */
    private PublicKey loadPublicKey() throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        Path keyPath = Paths.get(keysDirectory, PUBLIC_KEY_FILE);
        String keyContent = Files.readString(keyPath)
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");
        
        byte[] keyBytes = Base64.getDecoder().decode(keyContent);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance(ALGORITHM);
        return keyFactory.generatePublic(keySpec);
    }

    /**
     * Salva a chave privada em arquivo.
     */
    private void savePrivateKey(PrivateKey privateKey) throws IOException {
        Path keyPath = Paths.get(keysDirectory, PRIVATE_KEY_FILE);
        String encodedKey = Base64.getEncoder().encodeToString(privateKey.getEncoded());
        String pemKey = "-----BEGIN PRIVATE KEY-----\n" +
                formatKeyString(encodedKey) +
                "\n-----END PRIVATE KEY-----";
        Files.writeString(keyPath, pemKey);
        logger.debug("Chave privada salva em: {}", keyPath.toAbsolutePath());
    }

    /**
     * Salva a chave pública em arquivo.
     */
    private void savePublicKey(PublicKey publicKey) throws IOException {
        Path keyPath = Paths.get(keysDirectory, PUBLIC_KEY_FILE);
        String encodedKey = Base64.getEncoder().encodeToString(publicKey.getEncoded());
        String pemKey = "-----BEGIN PUBLIC KEY-----\n" +
                formatKeyString(encodedKey) +
                "\n-----END PUBLIC KEY-----";
        Files.writeString(keyPath, pemKey);
        logger.debug("Chave pública salva em: {}", keyPath.toAbsolutePath());
    }

    /**
     * Formata a string da chave em linhas de 64 caracteres.
     */
    private String formatKeyString(String key) {
        StringBuilder formatted = new StringBuilder();
        for (int i = 0; i < key.length(); i += 64) {
            formatted.append(key, i, Math.min(i + 64, key.length())).append("\n");
        }
        return formatted.toString().trim();
    }

    /**
     * Cria o diretório de chaves se não existir.
     */
    private void createKeysDirectoryIfNotExists() throws IOException {
        Path keysPath = Paths.get(keysDirectory);
        if (!Files.exists(keysPath)) {
            Files.createDirectories(keysPath);
            logger.info("Diretório de chaves criado: {}", keysPath.toAbsolutePath());
        }
    }

    // Getters para acesso às chaves
    public PrivateKey getPrivateKey() {
        return privateKey;
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }

    public String getKeysDirectory() {
        return keysDirectory;
    }
    
    /**
     * Força a regeneração das chaves RSA.
     * Usado principalmente para testes.
     */
    public void forceRegenerateKeys() {
        try {
            logger.info("Forçando regeneração das chaves RSA...");
            generateNewKeys();
            logger.info("Chaves RSA regeneradas com sucesso.");
        } catch (Exception e) {
            logger.error("Erro ao regenerar chaves RSA: {}", e.getMessage(), e);
            throw new RuntimeException("Falha na regeneração das chaves RSA", e);
        }
    }
}