package com.example.sistemajava.security;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.*;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.*;
import java.util.Base64;

@Component
public class RsaKeyProvider implements InitializingBean {

    @Value("${app.security.keysDir:keys}")
    private String keysDir;

    private RSAPrivateKey privateKey;
    private RSAPublicKey publicKey;

    public RSAPrivateKey getPrivateKey() { return privateKey; }
    public RSAPublicKey getPublicKey() { return publicKey; }

    @Override
    public void afterPropertiesSet() throws Exception {
        Path dir = Path.of(keysDir);
        if (!Files.exists(dir)) {
            Files.createDirectories(dir);
        }
        Path privatePem = dir.resolve("jwt_key.pem");
        Path publicPem = dir.resolve("jwt_key.pub.pem");

        if (Files.exists(privatePem) && Files.exists(publicPem)) {
            try {
                this.privateKey = readPrivateKeyPem(Files.readString(privatePem));
                this.publicKey = readPublicKeyPem(Files.readString(publicPem));
                validateKeyPair();
                return;
            } catch (Exception e) {
                // fall through to regenerate
            }
        }

        KeyPair keyPair = generateRsaKeyPair();
        this.privateKey = (RSAPrivateKey) keyPair.getPrivate();
        this.publicKey = (RSAPublicKey) keyPair.getPublic();
        writePem(privatePem, toPem("PRIVATE KEY", privateKey.getEncoded()));
        writePem(publicPem, toPem("PUBLIC KEY", publicKey.getEncoded()));
        validateKeyPair();
    }

    private void validateKeyPair() throws GeneralSecurityException {
        byte[] message = "validation-test".getBytes(StandardCharsets.UTF_8);
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(privateKey);
        signature.update(message);
        byte[] sig = signature.sign();

        Signature verify = Signature.getInstance("SHA256withRSA");
        verify.initVerify(publicKey);
        verify.update(message);
        if (!verify.verify(sig)) {
            throw new GeneralSecurityException("Chaves RSA inválidas: falha na verificação de assinatura");
        }
    }

    private KeyPair generateRsaKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
        gen.initialize(2048);
        return gen.generateKeyPair();
    }

    private void writePem(Path path, String pem) throws IOException {
        Files.writeString(path, pem);
    }

    private String toPem(String type, byte[] derBytes) {
        String b64 = Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.UTF_8)).encodeToString(derBytes);
        return "-----BEGIN " + type + "-----\n" + b64 + "\n-----END " + type + "-----\n";
    }

    private RSAPrivateKey readPrivateKeyPem(String pem) throws GeneralSecurityException {
        String content = pem.replaceAll("-----BEGIN (.*)-----", "")
                .replaceAll("-----END (.*)-----", "")
                .replaceAll("\n", "")
                .trim();
        byte[] der = Base64.getMimeDecoder().decode(content);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(der);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return (RSAPrivateKey) kf.generatePrivate(spec);
    }

    private RSAPublicKey readPublicKeyPem(String pem) throws GeneralSecurityException {
        String content = pem.replaceAll("-----BEGIN (.*)-----", "")
                .replaceAll("-----END (.*)-----", "")
                .replaceAll("\n", "")
                .trim();
        byte[] der = Base64.getMimeDecoder().decode(content);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(der);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return (RSAPublicKey) kf.generatePublic(spec);
    }
}


