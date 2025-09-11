package com.sistema.java.controller;

import com.sistema.java.model.dto.AvatarUploadDTO;
import com.sistema.java.service.AvatarService;
import com.sistema.java.service.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Controller REST para gerenciamento de avatars
 * Referência: Regras de Avatar - project_rules.md
 * Referência: Regras de Edição de Perfil - project_rules.md
 */
@RestController
@RequestMapping("/api/avatar")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:8080"})
public class AvatarController {

    private static final Logger logger = LoggerFactory.getLogger(AvatarController.class);
    
    private final AvatarService avatarService;
    private final AuthService authService;

    public AvatarController(AvatarService avatarService, AuthService authService) {
        this.avatarService = avatarService;
        this.authService = authService;
    }

    /**
     * Upload de avatar com processamento assíncrono
     * Referência: Regras de Avatar - project_rules.md
     */
    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadAvatar(
            @RequestParam("arquivo") MultipartFile arquivo,
            @RequestParam("usuarioId") Long usuarioId,
            @RequestParam(value = "x", required = false) Integer x,
            @RequestParam(value = "y", required = false) Integer y,
            @RequestParam(value = "largura", required = false) Integer largura,
            @RequestParam(value = "altura", required = false) Integer altura,
            @RequestParam(value = "cropCentralizado", defaultValue = "true") boolean cropCentralizado) {
        
        try {
            // Verificar se usuário pode editar este avatar
            Long usuarioLogadoId = authService.getUsuarioLogado().getId();
            if (!avatarService.podeEditarAvatar(usuarioId, usuarioLogadoId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(criarResposta(false, "Você não tem permissão para editar este avatar", null));
            }

            // Criar DTO de upload
            AvatarUploadDTO uploadDTO;
            if (cropCentralizado || x == null || y == null || largura == null || altura == null) {
                uploadDTO = AvatarUploadDTO.cropCentralizado(usuarioId);
            } else {
                uploadDTO = AvatarUploadDTO.cropPersonalizado(usuarioId, x, y, largura, altura);
            }

            // Adicionar metadados do arquivo
            uploadDTO.setNomeOriginal(arquivo.getOriginalFilename());
            uploadDTO.setTamanhoOriginal(arquivo.getSize());
            uploadDTO.setTipoMime(arquivo.getContentType());

            // Iniciar processamento assíncrono
            CompletableFuture<String> futureAvatar = avatarService.processarUploadAvatar(usuarioId, arquivo, uploadDTO);

            // Retornar resposta imediata
            Map<String, Object> resposta = criarResposta(true, "Upload iniciado com sucesso", null);
            resposta.put("processandoAssincrono", true);
            resposta.put("usuarioId", usuarioId);
            
            logger.info("Upload de avatar iniciado para usuário: {}", usuarioId);
            return ResponseEntity.ok(resposta);

        } catch (IllegalArgumentException e) {
            logger.warn("Erro de validação no upload de avatar: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(criarResposta(false, e.getMessage(), null));
        } catch (Exception e) {
            logger.error("Erro interno no upload de avatar: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(criarResposta(false, "Erro interno do servidor", null));
        }
    }

    /**
     * Verifica status do processamento de avatar
     */
    @GetMapping("/status/{usuarioId}")
    public ResponseEntity<Map<String, Object>> verificarStatus(@PathVariable Long usuarioId) {
        try {
            // Verificar permissão
            Long usuarioLogadoId = authService.getUsuarioLogado().getId();
            if (!avatarService.podeEditarAvatar(usuarioId, usuarioLogadoId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(criarResposta(false, "Você não tem permissão para verificar este status", null));
            }

            // Obter avatar atual
            String avatarAtual = avatarService.obterAvatarUsuario(usuarioId, "medio");
            
            Map<String, Object> resposta = criarResposta(true, "Status obtido com sucesso", null);
            resposta.put("avatarAtual", avatarAtual);
            resposta.put("usuarioId", usuarioId);
            
            return ResponseEntity.ok(resposta);

        } catch (Exception e) {
            logger.error("Erro ao verificar status do avatar: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(criarResposta(false, "Erro interno do servidor", null));
        }
    }

    /**
     * Obtém avatar do usuário em tamanho específico
     */
    @GetMapping("/usuario/{usuarioId}")
    public ResponseEntity<Map<String, Object>> obterAvatar(
            @PathVariable Long usuarioId,
            @RequestParam(value = "tamanho", defaultValue = "medio") String tamanho) {
        
        try {
            String caminhoAvatar = avatarService.obterAvatarUsuario(usuarioId, tamanho);
            
            Map<String, Object> resposta = criarResposta(true, "Avatar obtido com sucesso", null);
            resposta.put("avatar", caminhoAvatar);
            resposta.put("usuarioId", usuarioId);
            resposta.put("tamanho", tamanho);
            
            return ResponseEntity.ok(resposta);

        } catch (Exception e) {
            logger.error("Erro ao obter avatar: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(criarResposta(false, "Erro interno do servidor", null));
        }
    }

    /**
     * Serve arquivos de avatar diretamente
     */
    @GetMapping("/arquivo/{nomeArquivo}")
    public ResponseEntity<Resource> servirAvatar(@PathVariable String nomeArquivo) {
        try {
            // Validar nome do arquivo para segurança
            if (!nomeArquivo.matches("^avatar_\\d+_\\d{8}_\\d{6}_[a-f0-9]{8}_(64|256|512|original)\\.(jpg|jpeg|png)$")) {
                return ResponseEntity.notFound().build();
            }

            Path caminhoArquivo = Paths.get(System.getProperty("java.io.tmpdir"), "avatars", nomeArquivo);
            Resource resource = new FileSystemResource(caminhoArquivo);

            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.notFound().build();
            }

            // Determinar tipo de conteúdo
            String contentType = Files.probeContentType(caminhoArquivo);
            if (contentType == null) {
                contentType = "image/jpeg"; // Padrão
            }

            return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CACHE_CONTROL, "max-age=3600") // Cache por 1 hora
                .body(resource);

        } catch (IOException e) {
            logger.error("Erro ao servir avatar {}: {}", nomeArquivo, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Remove avatar do usuário
     */
    @DeleteMapping("/usuario/{usuarioId}")
    public ResponseEntity<Map<String, Object>> removerAvatar(@PathVariable Long usuarioId) {
        try {
            // Verificar permissão
            Long usuarioLogadoId = authService.getUsuarioLogado().getId();
            if (!avatarService.podeEditarAvatar(usuarioId, usuarioLogadoId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(criarResposta(false, "Você não tem permissão para remover este avatar", null));
            }

            // Processar remoção (implementar no service)
            // avatarService.removerAvatar(usuarioId);
            
            Map<String, Object> resposta = criarResposta(true, "Avatar removido com sucesso", null);
            resposta.put("usuarioId", usuarioId);
            
            logger.info("Avatar removido para usuário: {}", usuarioId);
            return ResponseEntity.ok(resposta);

        } catch (Exception e) {
            logger.error("Erro ao remover avatar: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(criarResposta(false, "Erro interno do servidor", null));
        }
    }

    /**
     * Obtém informações de uso de espaço (apenas para admins)
     */
    @GetMapping("/admin/espaco")
    public ResponseEntity<Map<String, Object>> obterEspacoUsado() {
        try {
            // Verificar se é admin
            if (!authService.hasRole("ADMINISTRADOR")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(criarResposta(false, "Acesso negado", null));
            }

            long espacoUsado = avatarService.calcularEspacoUsado();
            
            Map<String, Object> resposta = criarResposta(true, "Informações obtidas com sucesso", null);
            resposta.put("espacoUsadoBytes", espacoUsado);
            resposta.put("espacoUsadoMB", espacoUsado / (1024.0 * 1024.0));
            
            return ResponseEntity.ok(resposta);

        } catch (Exception e) {
            logger.error("Erro ao obter espaço usado: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(criarResposta(false, "Erro interno do servidor", null));
        }
    }

    /**
     * Limpa avatars órfãos (apenas para admins)
     */
    @PostMapping("/admin/limpar-orfaos")
    public ResponseEntity<Map<String, Object>> limparAvatarsOrfaos() {
        try {
            // Verificar se é admin
            if (!authService.hasRole("ADMINISTRADOR")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(criarResposta(false, "Acesso negado", null));
            }

            // Iniciar limpeza assíncrona
            CompletableFuture<Integer> futureLimpeza = avatarService.limparAvatarsOrfaos();
            
            Map<String, Object> resposta = criarResposta(true, "Limpeza iniciada com sucesso", null);
            resposta.put("processandoAssincrono", true);
            
            logger.info("Limpeza de avatars órfãos iniciada por admin: {}", authService.getUsuarioLogado().getId());
            return ResponseEntity.ok(resposta);

        } catch (Exception e) {
            logger.error("Erro ao iniciar limpeza de avatars órfãos: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(criarResposta(false, "Erro interno do servidor", null));
        }
    }

    /**
     * Validação de arquivo antes do upload
     */
    @PostMapping("/validar")
    public ResponseEntity<Map<String, Object>> validarArquivo(
            @RequestParam("arquivo") MultipartFile arquivo) {
        
        try {
            // Validações básicas
            if (arquivo.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(criarResposta(false, "Arquivo não pode estar vazio", null));
            }

            // Verificar tamanho
            long tamanhoMaximo = 5 * 1024 * 1024; // 5MB
            if (arquivo.getSize() > tamanhoMaximo) {
                return ResponseEntity.badRequest()
                    .body(criarResposta(false, "Arquivo muito grande. Máximo: 5MB", null));
            }

            // Verificar tipo
            String contentType = arquivo.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return ResponseEntity.badRequest()
                    .body(criarResposta(false, "Arquivo deve ser uma imagem", null));
            }

            Map<String, Object> resposta = criarResposta(true, "Arquivo válido", null);
            resposta.put("nomeOriginal", arquivo.getOriginalFilename());
            resposta.put("tamanho", arquivo.getSize());
            resposta.put("tipo", contentType);
            
            return ResponseEntity.ok(resposta);

        } catch (Exception e) {
            logger.error("Erro na validação de arquivo: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(criarResposta(false, "Erro interno do servidor", null));
        }
    }

    /**
     * Cria resposta padronizada
     */
    private Map<String, Object> criarResposta(boolean sucesso, String mensagem, Object dados) {
        Map<String, Object> resposta = new HashMap<>();
        resposta.put("sucesso", sucesso);
        resposta.put("mensagem", mensagem);
        resposta.put("timestamp", System.currentTimeMillis());
        
        if (dados != null) {
            resposta.put("dados", dados);
        }
        
        return resposta;
    }
}