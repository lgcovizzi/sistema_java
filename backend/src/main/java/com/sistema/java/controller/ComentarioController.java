package com.sistema.java.controller;

import com.sistema.java.model.dto.ComentarioDTO;
import com.sistema.java.service.ComentarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Controller REST para gerenciamento de comentários
 * Referência: Sistema de Temas Claros e Escuros - project_rules.md
 * API deve retornar dados formatados considerando preferências de tema do cliente
 * 
 * @author Sistema Java
 * @version 1.0
 */
@RestController
@RequestMapping("/api/comentarios")
@Validated
@CrossOrigin(origins = "*")
public class ComentarioController {

    @Autowired
    private ComentarioService comentarioService;

    /**
     * Lista todos os comentários com paginação
     * Referência: Sistema de Temas Claros e Escuros - project_rules.md
     * Resposta deve incluir metadados de tema para renderização adequada
     * 
     * @param pageable Configuração de paginação
     * @return Página de comentários
     */
    @GetMapping
    public ResponseEntity<Page<ComentarioDTO>> findAll(
            @PageableDefault(size = 20, sort = "dataCriacao") Pageable pageable) {
        Page<ComentarioDTO> comentarios = comentarioService.findAll(pageable);
        return ResponseEntity.ok(comentarios);
    }

    /**
     * Lista comentários aprovados com paginação
     * Referência: Sistema de Temas Claros e Escuros - project_rules.md
     * Status de aprovação deve ser visualmente distinto em ambos os temas
     * 
     * @param pageable Configuração de paginação
     * @return Página de comentários aprovados
     */
    @GetMapping("/aprovados")
    public ResponseEntity<Page<ComentarioDTO>> findAprovados(
            @PageableDefault(size = 20, sort = "dataCriacao") Pageable pageable) {
        Page<ComentarioDTO> comentarios = comentarioService.findAprovados(pageable);
        return ResponseEntity.ok(comentarios);
    }

    /**
     * Lista comentários pendentes de aprovação
     * 
     * @param pageable Configuração de paginação
     * @return Página de comentários pendentes
     */
    @GetMapping("/pendentes")
    public ResponseEntity<Page<ComentarioDTO>> findPendentes(
            @PageableDefault(size = 20, sort = "dataCriacao") Pageable pageable) {
        Page<ComentarioDTO> comentarios = comentarioService.findPendentes(pageable);
        return ResponseEntity.ok(comentarios);
    }

    /**
     * Lista comentários pendentes com detalhes
     * 
     * @param pageable Configuração de paginação
     * @return Página de comentários pendentes com detalhes
     */
    @GetMapping("/pendentes-detalhes")
    public ResponseEntity<Page<ComentarioDTO>> findPendentesComDetalhes(
            @PageableDefault(size = 20, sort = "dataCriacao") Pageable pageable) {
        Page<ComentarioDTO> comentarios = comentarioService.findPendentesComDetalhes(pageable);
        return ResponseEntity.ok(comentarios);
    }

    /**
     * Busca comentário por ID
     * 
     * @param id ID do comentário
     * @return Comentário encontrado
     */
    @GetMapping("/{id}")
    public ResponseEntity<ComentarioDTO> findById(@PathVariable Long id) {
        Optional<ComentarioDTO> comentario = comentarioService.findById(id);
        return comentario.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Busca comentários por notícia
     * 
     * @param noticiaId ID da notícia
     * @param aprovado Status de aprovação (opcional)
     * @param pageable Configuração de paginação
     * @return Página de comentários da notícia
     */
    @GetMapping("/noticia/{noticiaId}")
    public ResponseEntity<Page<ComentarioDTO>> findByNoticia(
            @PathVariable Long noticiaId,
            @RequestParam(defaultValue = "true") boolean aprovado,
            @PageableDefault(size = 20, sort = "dataCriacao") Pageable pageable) {
        try {
            Page<ComentarioDTO> comentarios = comentarioService.findByNoticia(noticiaId, aprovado, pageable);
            return ResponseEntity.ok(comentarios);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Busca comentários por autor
     * 
     * @param autorId ID do autor
     * @param pageable Configuração de paginação
     * @return Página de comentários do autor
     */
    @GetMapping("/autor/{autorId}")
    public ResponseEntity<Page<ComentarioDTO>> findByAutor(
            @PathVariable Long autorId,
            @PageableDefault(size = 20, sort = "dataCriacao") Pageable pageable) {
        try {
            Page<ComentarioDTO> comentarios = comentarioService.findByAutor(autorId, pageable);
            return ResponseEntity.ok(comentarios);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Busca comentários por período
     * 
     * @param dataInicio Data de início
     * @param dataFim Data de fim
     * @param pageable Configuração de paginação
     * @return Página de comentários do período
     */
    @GetMapping("/periodo")
    public ResponseEntity<Page<ComentarioDTO>> findByPeriodo(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dataInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dataFim,
            @PageableDefault(size = 20, sort = "dataCriacao") Pageable pageable) {
        Page<ComentarioDTO> comentarios = comentarioService.findByPeriodo(dataInicio, dataFim, pageable);
        return ResponseEntity.ok(comentarios);
    }

    /**
     * Busca comentários por termo
     * 
     * @param termo Termo de busca
     * @param aprovado Status de aprovação (opcional)
     * @param pageable Configuração de paginação
     * @return Página de comentários encontrados
     */
    @GetMapping("/buscar")
    public ResponseEntity<Page<ComentarioDTO>> buscar(
            @RequestParam @NotBlank(message = "Termo não pode estar vazio") String termo,
            @RequestParam(defaultValue = "true") boolean aprovado,
            @PageableDefault(size = 20, sort = "dataCriacao") Pageable pageable) {
        Page<ComentarioDTO> comentarios = comentarioService.buscar(termo, aprovado, pageable);
        return ResponseEntity.ok(comentarios);
    }

    /**
     * Busca comentários recentes
     * 
     * @param limite Número máximo de comentários
     * @return Lista de comentários recentes
     */
    @GetMapping("/recentes")
    public ResponseEntity<List<ComentarioDTO>> findRecentes(
            @RequestParam(defaultValue = "10") int limite) {
        List<ComentarioDTO> comentarios = comentarioService.findRecentes(limite);
        return ResponseEntity.ok(comentarios);
    }

    /**
     * Cria novo comentário
     * 
     * @param request Dados do comentário
     * @return Comentário criado
     */
    @PostMapping
    public ResponseEntity<ComentarioDTO> create(@Valid @RequestBody CriarComentarioRequest request) {
        try {
            ComentarioDTO novoComentario = comentarioService.create(
                    request.getComentario(), 
                    request.getAutorId(), 
                    request.getNoticiaId()
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(novoComentario);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Atualiza comentário existente
     * 
     * @param id ID do comentário
     * @param comentarioDTO Dados atualizados
     * @return Comentário atualizado
     */
    @PutMapping("/{id}")
    public ResponseEntity<ComentarioDTO> update(
            @PathVariable Long id,
            @Valid @RequestBody ComentarioDTO comentarioDTO) {
        try {
            ComentarioDTO comentarioAtualizado = comentarioService.update(id, comentarioDTO);
            return ResponseEntity.ok(comentarioAtualizado);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Aprova comentário
     * 
     * @param id ID do comentário
     * @return Comentário aprovado
     */
    @PatchMapping("/{id}/aprovar")
    public ResponseEntity<ComentarioDTO> aprovar(@PathVariable Long id) {
        try {
            ComentarioDTO comentario = comentarioService.aprovar(id);
            return ResponseEntity.ok(comentario);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Reprova comentário
     * 
     * @param id ID do comentário
     * @return Comentário reprovado
     */
    @PatchMapping("/{id}/reprovar")
    public ResponseEntity<ComentarioDTO> reprovar(@PathVariable Long id) {
        try {
            ComentarioDTO comentario = comentarioService.reprovar(id);
            return ResponseEntity.ok(comentario);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Remove comentário
     * 
     * @param id ID do comentário
     * @return Resposta de sucesso
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        try {
            comentarioService.delete(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Aprova comentários em lote
     * 
     * @param request Lista de IDs dos comentários
     * @return Número de comentários aprovados
     */
    @PatchMapping("/aprovar-lote")
    public ResponseEntity<Integer> aprovarLote(@Valid @RequestBody AprovarLoteRequest request) {
        int count = comentarioService.aprovarLote(request.getIds());
        return ResponseEntity.ok(count);
    }

    /**
     * Reprova comentários em lote
     * 
     * @param request Lista de IDs dos comentários
     * @return Número de comentários reprovados
     */
    @PatchMapping("/reprovar-lote")
    public ResponseEntity<Integer> reprovarLote(@Valid @RequestBody ReprovarLoteRequest request) {
        int count = comentarioService.reprovarLote(request.getIds());
        return ResponseEntity.ok(count);
    }

    /**
     * Remove comentários antigos não aprovados
     * 
     * @param diasAntigos Número de dias
     * @return Número de comentários removidos
     */
    @DeleteMapping("/limpar-antigos")
    public ResponseEntity<Integer> removerAntigosNaoAprovados(
            @RequestParam(defaultValue = "30") int diasAntigos) {
        int count = comentarioService.removerAntigosNaoAprovados(diasAntigos);
        return ResponseEntity.ok(count);
    }

    /**
     * Conta comentários aprovados
     * 
     * @return Número de comentários aprovados
     */
    @GetMapping("/count/aprovados")
    public ResponseEntity<Long> countAprovados() {
        long count = comentarioService.countAprovados();
        return ResponseEntity.ok(count);
    }

    /**
     * Conta comentários pendentes
     * 
     * @return Número de comentários pendentes
     */
    @GetMapping("/count/pendentes")
    public ResponseEntity<Long> countPendentes() {
        long count = comentarioService.countPendentes();
        return ResponseEntity.ok(count);
    }

    /**
     * Conta total de comentários
     * 
     * @return Número total de comentários
     */
    @GetMapping("/count/total")
    public ResponseEntity<Long> countTotal() {
        long count = comentarioService.countTotal();
        return ResponseEntity.ok(count);
    }

    /**
     * Obtém estatísticas dos comentários
     * 
     * @return Lista com estatísticas dos comentários
     */
    @GetMapping("/estatisticas")
    public ResponseEntity<List<Object[]>> getEstatisticas() {
        List<Object[]> estatisticas = comentarioService.getEstatisticas();
        return ResponseEntity.ok(estatisticas);
    }

    /**
     * Busca usuários mais ativos nos comentários
     * 
     * @param limite Número máximo de usuários
     * @return Lista de usuários mais ativos
     */
    @GetMapping("/usuarios-mais-ativos")
    public ResponseEntity<List<Object[]>> findUsuariosMaisAtivos(
            @RequestParam(defaultValue = "10") int limite) {
        List<Object[]> usuarios = comentarioService.findUsuariosMaisAtivos(limite);
        return ResponseEntity.ok(usuarios);
    }

    /**
     * Classe para request de criação de comentário
     */
    public static class CriarComentarioRequest {
        @Valid
        private ComentarioDTO comentario;
        
        private Long autorId;
        
        private Long noticiaId;

        public ComentarioDTO getComentario() {
            return comentario;
        }

        public void setComentario(ComentarioDTO comentario) {
            this.comentario = comentario;
        }

        public Long getAutorId() {
            return autorId;
        }

        public void setAutorId(Long autorId) {
            this.autorId = autorId;
        }

        public Long getNoticiaId() {
            return noticiaId;
        }

        public void setNoticiaId(Long noticiaId) {
            this.noticiaId = noticiaId;
        }
    }

    /**
     * Classe para request de aprovação em lote
     */
    public static class AprovarLoteRequest {
        @NotEmpty(message = "Lista de IDs é obrigatória")
        private List<Long> ids;

        public List<Long> getIds() {
            return ids;
        }

        public void setIds(List<Long> ids) {
            this.ids = ids;
        }
    }

    /**
     * Classe para request de reprovação em lote
     */
    public static class ReprovarLoteRequest {
        @NotEmpty(message = "Lista de IDs é obrigatória")
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