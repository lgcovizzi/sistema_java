package com.sistema.java.controller;

import com.sistema.java.model.dto.UsuarioDTO;
import com.sistema.java.service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Optional;

/**
 * Controller REST para gerenciamento de usuários
 * 
 * @author Sistema Java
 * @version 1.0
 */
@RestController
@RequestMapping("/api/usuarios")
@Validated
@CrossOrigin(origins = "*")
public class UsuarioController {

    @Autowired
    private UsuarioService usuarioService;

    /**
     * Lista todos os usuários com paginação
     * 
     * @param pageable Configuração de paginação
     * @return Página de usuários
     */
    @GetMapping
    public ResponseEntity<Page<UsuarioDTO>> findAll(
            @PageableDefault(size = 20, sort = "nome") Pageable pageable) {
        Page<UsuarioDTO> usuarios = usuarioService.findAll(pageable);
        return ResponseEntity.ok(usuarios);
    }

    /**
     * Lista usuários ativos com paginação
     * 
     * @param pageable Configuração de paginação
     * @return Página de usuários ativos
     */
    @GetMapping("/ativos")
    public ResponseEntity<Page<UsuarioDTO>> findAtivos(
            @PageableDefault(size = 20, sort = "nome") Pageable pageable) {
        Page<UsuarioDTO> usuarios = usuarioService.findAtivos(pageable);
        return ResponseEntity.ok(usuarios);
    }

    /**
     * Busca usuário por ID
     * 
     * @param id ID do usuário
     * @return Usuário encontrado
     */
    @GetMapping("/{id}")
    public ResponseEntity<UsuarioDTO> findById(@PathVariable Long id) {
        Optional<UsuarioDTO> usuario = usuarioService.findById(id);
        return usuario.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Busca usuário por email
     * 
     * @param email Email do usuário
     * @return Usuário encontrado
     */
    @GetMapping("/email/{email}")
    public ResponseEntity<UsuarioDTO> findByEmail(
            @PathVariable @Email(message = "Email deve ter formato válido") String email) {
        Optional<UsuarioDTO> usuario = usuarioService.findByEmail(email);
        return usuario.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Busca usuários por nome
     * 
     * @param nome Nome ou parte do nome
     * @param pageable Configuração de paginação
     * @return Página de usuários encontrados
     */
    @GetMapping("/buscar")
    public ResponseEntity<Page<UsuarioDTO>> findByNome(
            @RequestParam @NotBlank(message = "Nome não pode estar vazio") String nome,
            @PageableDefault(size = 20, sort = "nome") Pageable pageable) {
        Page<UsuarioDTO> usuarios = usuarioService.findByNome(nome, pageable);
        return ResponseEntity.ok(usuarios);
    }

    /**
     * Busca usuários mais ativos
     * 
     * @param limite Número máximo de usuários
     * @return Lista de usuários mais ativos
     */
    @GetMapping("/mais-ativos")
    public ResponseEntity<List<UsuarioDTO>> findMaisAtivos(
            @RequestParam(defaultValue = "10") int limite) {
        List<UsuarioDTO> usuarios = usuarioService.findMaisAtivos(limite);
        return ResponseEntity.ok(usuarios);
    }

    /**
     * Cria novo usuário
     * 
     * @param usuarioDTO Dados do usuário
     * @return Usuário criado
     */
    @PostMapping
    public ResponseEntity<UsuarioDTO> create(@Valid @RequestBody UsuarioDTO usuarioDTO) {
        try {
            UsuarioDTO novoUsuario = usuarioService.create(usuarioDTO);
            return ResponseEntity.status(HttpStatus.CREATED).body(novoUsuario);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Atualiza usuário existente
     * 
     * @param id ID do usuário
     * @param usuarioDTO Dados atualizados
     * @return Usuário atualizado
     */
    @PutMapping("/{id}")
    public ResponseEntity<UsuarioDTO> update(
            @PathVariable Long id,
            @Valid @RequestBody UsuarioDTO usuarioDTO) {
        try {
            UsuarioDTO usuarioAtualizado = usuarioService.update(id, usuarioDTO);
            return ResponseEntity.ok(usuarioAtualizado);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Ativa usuário
     * 
     * @param id ID do usuário
     * @return Usuário ativado
     */
    @PatchMapping("/{id}/ativar")
    public ResponseEntity<UsuarioDTO> ativar(@PathVariable Long id) {
        try {
            UsuarioDTO usuario = usuarioService.ativar(id);
            return ResponseEntity.ok(usuario);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Desativa usuário
     * 
     * @param id ID do usuário
     * @return Usuário desativado
     */
    @PatchMapping("/{id}/desativar")
    public ResponseEntity<UsuarioDTO> desativar(@PathVariable Long id) {
        try {
            UsuarioDTO usuario = usuarioService.desativar(id);
            return ResponseEntity.ok(usuario);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Altera senha do usuário
     * 
     * @param id ID do usuário
     * @param request Dados da nova senha
     * @return Resposta de sucesso
     */
    @PatchMapping("/{id}/senha")
    public ResponseEntity<Void> alterarSenha(
            @PathVariable Long id,
            @Valid @RequestBody AlterarSenhaRequest request) {
        try {
            usuarioService.alterarSenha(id, request.getNovaSenha());
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Remove usuário
     * 
     * @param id ID do usuário
     * @return Resposta de sucesso
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        try {
            usuarioService.delete(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Verifica se email já existe
     * 
     * @param email Email a verificar
     * @return true se existe
     */
    @GetMapping("/verificar-email")
    public ResponseEntity<Boolean> verificarEmail(
            @RequestParam @Email(message = "Email deve ter formato válido") String email) {
        boolean existe = usuarioService.existsByEmail(email);
        return ResponseEntity.ok(existe);
    }

    /**
     * Conta usuários ativos
     * 
     * @return Número de usuários ativos
     */
    @GetMapping("/count/ativos")
    public ResponseEntity<Long> countAtivos() {
        long count = usuarioService.countAtivos();
        return ResponseEntity.ok(count);
    }

    /**
     * Conta total de usuários
     * 
     * @return Número total de usuários
     */
    @GetMapping("/count/total")
    public ResponseEntity<Long> countTotal() {
        long count = usuarioService.countTotal();
        return ResponseEntity.ok(count);
    }

    /**
     * Ativa usuários em lote
     * 
     * @param request Lista de IDs dos usuários
     * @return Número de usuários ativados
     */
    @PatchMapping("/ativar-lote")
    public ResponseEntity<Integer> ativarLote(@Valid @RequestBody AtivarLoteRequest request) {
        int count = usuarioService.ativarLote(request.getIds());
        return ResponseEntity.ok(count);
    }

    /**
     * Desativa usuários em lote
     * 
     * @param request Lista de IDs dos usuários
     * @return Número de usuários desativados
     */
    @PatchMapping("/desativar-lote")
    public ResponseEntity<Integer> desativarLote(@Valid @RequestBody DesativarLoteRequest request) {
        int count = usuarioService.desativarLote(request.getIds());
        return ResponseEntity.ok(count);
    }

    /**
     * Classe para request de alteração de senha
     */
    public static class AlterarSenhaRequest {
        @NotBlank(message = "Nova senha é obrigatória")
        private String novaSenha;

        public String getNovaSenha() {
            return novaSenha;
        }

        public void setNovaSenha(String novaSenha) {
            this.novaSenha = novaSenha;
        }
    }

    /**
     * Classe para request de ativação em lote
     */
    public static class AtivarLoteRequest {
        @NotBlank(message = "Lista de IDs é obrigatória")
        private List<Long> ids;

        public List<Long> getIds() {
            return ids;
        }

        public void setIds(List<Long> ids) {
            this.ids = ids;
        }
    }

    /**
     * Classe para request de desativação em lote
     */
    public static class DesativarLoteRequest {
        @NotBlank(message = "Lista de IDs é obrigatória")
        private List<Long> ids;

        public List<Long> getIds() {
            return ids;
        }

        public void setIds(List<Long> ids) {
            this.ids = ids;
        }
    }

    /**
     * Tratamento de exceções de validação
     * 
     * @param ex Exceção de validação
     * @return Resposta de erro
     */
    @ExceptionHandler(jakarta.validation.ConstraintViolationException.class)
    public ResponseEntity<String> handleValidationException(
            jakarta.validation.ConstraintViolationException ex) {
        return ResponseEntity.badRequest().body("Erro de validação: " + ex.getMessage());
    }

    /**
     * Tratamento de exceções gerais
     * 
     * @param ex Exceção
     * @return Resposta de erro
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleException(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Erro interno: " + ex.getMessage());
    }
}