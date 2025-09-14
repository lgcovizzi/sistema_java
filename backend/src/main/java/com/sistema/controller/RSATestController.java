package com.sistema.controller;

import com.sistema.config.RSAKeyManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Controlador para testar o sistema de chaves RSA.
 */
@RestController
@RequestMapping("/api")
public class RSATestController {

    @Autowired
    private RSAKeyManager rsaKeyManager;

    /**
     * Endpoint para testar o sistema de chaves RSA.
     */
    @GetMapping("/rsa-test")
    public ResponseEntity<Map<String, Object>> testRSAKeys() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Verifica se as chaves estão disponíveis
            boolean privateKeyExists = rsaKeyManager.getPrivateKey() != null;
            boolean publicKeyExists = rsaKeyManager.getPublicKey() != null;
            
            response.put("status", "success");
            response.put("privateKeyExists", privateKeyExists);
            response.put("publicKeyExists", publicKeyExists);
            response.put("keysDirectory", rsaKeyManager.getKeysDirectory());
            response.put("privateKeyAlgorithm", privateKeyExists ? rsaKeyManager.getPrivateKey().getAlgorithm() : "N/A");
            response.put("publicKeyAlgorithm", publicKeyExists ? rsaKeyManager.getPublicKey().getAlgorithm() : "N/A");
            response.put("message", "Sistema de chaves RSA funcionando corretamente");
            
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "Erro ao acessar sistema de chaves RSA: " + e.getMessage());
        }
        
        return ResponseEntity.ok(response);
    }
}