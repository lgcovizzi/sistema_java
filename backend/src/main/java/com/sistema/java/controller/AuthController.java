package com.sistema.java.controller;

import com.sistema.java.model.dto.LoginRequestDTO;
import com.sistema.java.model.dto.LoginResponseDTO;
import com.sistema.java.model.dto.RegistroRequestDTO;
import com.sistema.java.model.dto.RegistroResponseDTO;
import com.sistema.java.service.AuthService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller para autenticação e registro de usuários
 * Referência: Login e Registro - project_rules.md
 * Referência: Controle de Acesso - project_rules.md
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
    
    private final AuthService authService;
    
    public AuthController(AuthService authService) {
        this.authService = authService;
    }
    
    /**
     * Endpoint para login de usuários
     * Referência: Login e Registro - project_rules.md
     * 
     * @param loginRequest Dados de login (email e senha)
     * @return Token JWT e informações do usuário
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@Valid @RequestBody LoginRequestDTO loginRequest) {
        try {
            logger.info("Tentativa de login para email: {}", loginRequest.getEmail());
            
            // TODO: Implementar autenticação
            LoginResponseDTO response = null; // authService.autenticar(loginRequest);
            
            logger.info("Login realizado com sucesso para usuário: {} - Papel: {}", 
                       response.getUsuario().getEmail(), response.getUsuario().getPapel());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.warn("Falha no login para email: {} - Motivo: {}", loginRequest.getEmail(), e.getMessage());
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("erro", "Credenciais inválidas");
            errorResponse.put("mensagem", "Email ou senha incorretos");
            errorResponse.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.badRequest().body(LoginResponseDTO.builder()
                .sucesso(false)
                .mensagem("Credenciais inválidas")
                .erro(e.getMessage())
                .build());
        }
    }
    
    /**
     * Endpoint para registro de novos usuários
     * Referência: Login e Registro - project_rules.md
     * 
     * @param registroRequest Dados de registro
     * @return Confirmação de registro
     */
    @PostMapping("/registro")
    public ResponseEntity<RegistroResponseDTO> registro(@Valid @RequestBody RegistroRequestDTO registroRequest) {
        try {
            logger.info("Tentativa de registro para email: {} - CPF: {}", 
                       registroRequest.getEmail(), 
                       registroRequest.getCpf().replaceAll("\\d(?=\\d{4})", "*"));
            
            RegistroResponseDTO response = authService.registrar(registroRequest);
            
            logger.info("Registro realizado com sucesso para usuário: {} - ID: {}", 
                       response.getUsuario().getEmail(), response.getUsuario().getId());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.warn("Falha no registro para email: {} - Motivo: {}", registroRequest.getEmail(), e.getMessage());
            
            return ResponseEntity.badRequest().body(RegistroResponseDTO.builder()
                .sucesso(false)
                .mensagem("Erro no registro")
                .erro(e.getMessage())
                .build());
        }
    }
    
    /**
     * Endpoint para refresh do token JWT
     * Referência: Controle de Acesso - project_rules.md
     * 
     * @param refreshToken Token de refresh
     * @return Novo token de acesso
     */
    @PostMapping("/refresh")
    public ResponseEntity<Map<String, Object>> refreshToken(@RequestParam String refreshToken) {
        try {
            logger.debug("Tentativa de refresh de token");
            
            LoginResponseDTO loginResponse = authService.refreshToken(refreshToken);
            
            Map<String, Object> response = new HashMap<>();
            response.put("sucesso", true);
            response.put("token", loginResponse.getToken());
            response.put("tipo", "Bearer");
            response.put("mensagem", "Token renovado com sucesso");
            
            logger.debug("Token renovado com sucesso");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.warn("Falha ao renovar token: {}", e.getMessage());
            
            Map<String, Object> response = new HashMap<>();
            response.put("sucesso", false);
            response.put("erro", e.getMessage());
            response.put("mensagem", "Falha ao renovar token");
            
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * Endpoint para logout (invalidar token)
     * Referência: Controle de Acesso - project_rules.md
     * 
     * @param token Token a ser invalidado
     * @return Confirmação de logout
     */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(@RequestParam String token) {
        try {
            logger.debug("Tentativa de logout");
            
            authService.logout(token);
            
            Map<String, Object> response = new HashMap<>();
            response.put("sucesso", true);
            response.put("mensagem", "Logout realizado com sucesso");
            
            logger.debug("Logout realizado com sucesso");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.warn("Falha no logout: {}", e.getMessage());
            
            Map<String, Object> response = new HashMap<>();
            response.put("sucesso", false);
            response.put("erro", e.getMessage());
            response.put("mensagem", "Falha no logout");
            
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * Endpoint para validar token JWT
     * Referência: Controle de Acesso - project_rules.md
     * 
     * @param token Token a ser validado
     * @return Informações do token
     */
    @PostMapping("/validar-token")
    public ResponseEntity<Map<String, Object>> validarToken(@RequestParam String token) {
        try {
            boolean tokenValido = authService.validarToken(token);
            
            Map<String, Object> response = new HashMap<>();
            response.put("sucesso", true);
            response.put("valido", tokenValido);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.debug("Token inválido: {}", e.getMessage());
            
            Map<String, Object> response = new HashMap<>();
            response.put("sucesso", false);
            response.put("valido", false);
            response.put("erro", e.getMessage());
            
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * Endpoint para verificar disponibilidade de email
     * Referência: Login e Registro - project_rules.md
     * 
     * @param email Email a ser verificado
     * @return Disponibilidade do email
     */
    @GetMapping("/verificar-email")
    public ResponseEntity<Map<String, Object>> verificarEmail(@RequestParam String email) {
        try {
            boolean disponivel = authService.isEmailDisponivel(email);
            
            Map<String, Object> response = new HashMap<>();
            response.put("email", email);
            response.put("disponivel", disponivel);
            response.put("mensagem", disponivel ? "Email disponível" : "Email já está em uso");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Erro ao verificar email: {}", e.getMessage());
            
            Map<String, Object> response = new HashMap<>();
            response.put("erro", e.getMessage());
            response.put("mensagem", "Erro ao verificar email");
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * Endpoint para verificar disponibilidade de CPF
     * Referência: Login e Registro - project_rules.md
     * 
     * @param cpf CPF a ser verificado
     * @return Disponibilidade do CPF
     */
    @GetMapping("/verificar-cpf")
    public ResponseEntity<Map<String, Object>> verificarCpf(@RequestParam String cpf) {
        try {
            boolean disponivel = authService.isCpfDisponivel(cpf);
            
            Map<String, Object> response = new HashMap<>();
            response.put("cpf", cpf.replaceAll("\\d(?=\\d{4})", "*")); // Mascarar CPF na resposta
            response.put("disponivel", disponivel);
            response.put("mensagem", disponivel ? "CPF disponível" : "CPF já está em uso");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Erro ao verificar CPF: {}", e.getMessage());
            
            Map<String, Object> response = new HashMap<>();
            response.put("erro", e.getMessage());
            response.put("mensagem", "Erro ao verificar CPF");
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * Endpoint para obter informações do usuário autenticado
     * Referência: Controle de Acesso - project_rules.md
     * 
     * @return Informações do usuário
     */
    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> obterUsuarioAtual(@RequestHeader("Authorization") String authHeader) {
        try {
            // Extrair token do header Authorization
            String token = authHeader.startsWith("Bearer ") ? authHeader.substring(7) : authHeader;
            
            // TODO: Implementar obtenção de informações do usuário a partir do token
            // Por enquanto, retornar informações mockadas para desenvolvimento
            Map<String, Object> usuarioInfo = new HashMap<>();
            usuarioInfo.put("id", 1L);
            usuarioInfo.put("nome", "Usuário Teste");
            usuarioInfo.put("email", "teste@exemplo.com");
            
            Map<String, Object> response = new HashMap<>();
            response.put("sucesso", true);
            response.put("usuario", usuarioInfo);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Erro ao obter usuário atual: {}", e.getMessage());
            
            Map<String, Object> response = new HashMap<>();
            response.put("sucesso", false);
            response.put("erro", e.getMessage());
            response.put("mensagem", "Erro ao obter informações do usuário");
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
}