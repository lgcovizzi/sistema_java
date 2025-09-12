package com.sistema.java.controller;

import com.sistema.java.model.dto.NoticiaDTO;
import com.sistema.java.service.NoticiaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.Optional;

/**
 * Controller REST para gerenciamento de notícias
 * 
 * @author Sistema Java
 * @version 1.0
 */
@RestController
@RequestMapping("/api/noticias")
@Validated
@CrossOrigin(origins = "*")
public class NoticiaController {

    @Autowired
    private NoticiaService noticiaService;

    /**
     * Lista todas as notícias com paginação
     * 
     * @param pageable Configuração de paginação
     * @return Página de notícias
     */
    @GetMapping
    public ResponseEntity<Page<NoticiaDTO>> findAll(
            @PageableDefault(size = 20, sort = "dataPublicacao") Pageable pageable) {
        Page<NoticiaDTO> noticias = noticiaService.findAll(pageable);
        return ResponseEntity.ok(noticias);
    }

    /**
     * Lista notícias publicadas com paginação
     * 
     * @param pageable Configuração de paginação
     * @return Página de notícias publicadas
     */
    @GetMapping("/publicadas")
    public ResponseEntity<Page<NoticiaDTO>> findPublicadas(
            @PageableDefault(size = 20, sort = "dataPublicacao") Pageable pageable) {
        Page<NoticiaDTO> noticias = noticiaService.findPublicadas(pageable);
        return ResponseEntity.ok(noticias);
    }

    /**
     * Busca notícia por ID
     * 
     * @param id ID da notícia
     * @return Notícia encontrada
     */
    @GetMapping("/{id}")
    public ResponseEntity<NoticiaDTO> findById(@PathVariable Long id) {
        Optional<NoticiaDTO> noticia = noticiaService.findById(id);
        return noticia.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Busca notícias por autor
     * 
     * @param autorId ID do autor
     * @param pageable Configuração de paginação
     * @return Página de notícias do autor
     */
    @GetMapping("/autor/{autorId}")
    public ResponseEntity<Page<NoticiaDTO>> findByAutor(
            @PathVariable Long autorId,
            @PageableDefault(size = 20, sort = "dataPublicacao") Pageable pageable) {
        try {
            Page<NoticiaDTO> noticias = noticiaService.findByAutor(autorId, pageable);
            return ResponseEntity.ok(noticias);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Busca notícias por categoria
     * 
     * @param categoriaId ID da categoria
     * @param publicada Status de publicação (opcional)
     * @param pageable Configuração de paginação
     * @return Página de notícias da categoria
     */
    @GetMapping("/categoria/{categoriaId}")
    public ResponseEntity<Page<NoticiaDTO>> findByCategoria(
            @PathVariable Long categoriaId,
            @RequestParam(defaultValue = "true") boolean publicada,
            @PageableDefault(size = 20, sort = "dataPublicacao") Pageable pageable) {
        Page<NoticiaDTO> noticias = noticiaService.findByCategoria(categoriaId, publicada, pageable);
        return ResponseEntity.ok(noticias);
    }

    /**
     * Busca notícias por título
     * 
     * @param titulo Título ou parte do título
     * @param publicada Status de publicação (opcional)
     * @param pageable Configuração de paginação
     * @return Página de notícias encontradas
     */
    @GetMapping("/titulo")
    public ResponseEntity<Page<NoticiaDTO>> findByTitulo(
            @RequestParam @NotBlank(message = "Título não pode estar vazio") String titulo,
            @RequestParam(defaultValue = "true") boolean publicada,
            @PageableDefault(size = 20, sort = "dataPublicacao") Pageable pageable) {
        Page<NoticiaDTO> noticias = noticiaService.findByTitulo(titulo, publicada, pageable);
        return ResponseEntity.ok(noticias);
    }

    /**
     * Busca notícias por termo
     * 
     * @param termo Termo de busca
     * @param publicada Status de publicação (opcional)
     * @param pageable Configuração de paginação
     * @return Página de notícias encontradas
     */
    @GetMapping("/buscar")
    public ResponseEntity<Page<NoticiaDTO>> buscar(
            @RequestParam @NotBlank(message = "Termo não pode estar vazio") String termo,
            @RequestParam(defaultValue = "true") boolean publicada,
            @PageableDefault(size = 20, sort = "dataPublicacao") Pageable pageable) {
        Page<NoticiaDTO> noticias = noticiaService.buscar(termo, publicada, pageable);
        return ResponseEntity.ok(noticias);
    }

    /**
     * Busca notícias mais recentes
     * 
     * @param limite Número máximo de notícias
     * @return Lista de notícias mais recentes
     */
    @GetMapping("/recentes")
    public ResponseEntity<List<NoticiaDTO>> findRecentes(
            @RequestParam(defaultValue = "10") int limite) {
        List<NoticiaDTO> noticias = noticiaService.findRecentes(limite);
        return ResponseEntity.ok(noticias);
    }

    /**
     * Busca notícias mais comentadas
     * 
     * @param limite Número máximo de notícias
     * @return Lista de notícias mais comentadas
     */
    @GetMapping("/mais-comentadas")
    public ResponseEntity<List<NoticiaDTO>> findMaisComentadas(
            @RequestParam(defaultValue = "10") int limite) {
        List<NoticiaDTO> noticias = noticiaService.findMaisComentadas(limite);
        return ResponseEntity.ok(noticias);
    }

    /**
     * Cria nova notícia
     * 
     * @param request Dados da notícia com categorias
     * @return Notícia criada
     */
    @PostMapping
    public ResponseEntity<NoticiaDTO> create(@Valid @RequestBody CriarNoticiaRequest request) {
        try {
            NoticiaDTO novaNoticia = noticiaService.create(
                    request.getNoticia(), 
                    request.getAutorId(), 
                    request.getCategoriaIds()
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(novaNoticia);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Atualiza notícia existente
     * 
     * @param id ID da notícia
     * @param request Dados atualizados
     * @return Notícia atualizada
     */
    @PutMapping("/{id}")
    public ResponseEntity<NoticiaDTO> update(
            @PathVariable Long id,
            @Valid @RequestBody AtualizarNoticiaRequest request) {
        try {
            NoticiaDTO noticiaAtualizada = noticiaService.update(
                    id, 
                    request.getNoticia(), 
                    request.getCategoriaIds()
            );
            return ResponseEntity.ok(noticiaAtualizada);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Publica notícia
     * 
     * @param id ID da notícia
     * @return Notícia publicada
     */
    @PatchMapping("/{id}/publicar")
    public ResponseEntity<NoticiaDTO> publicar(@PathVariable Long id) {
        try {
            NoticiaDTO noticia = noticiaService.publicar(id);
            return ResponseEntity.ok(noticia);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Despublica notícia
     * 
     * @param id ID da notícia
     * @return Notícia despublicada
     */
    @PatchMapping("/{id}/despublicar")
    public ResponseEntity<NoticiaDTO> despublicar(@PathVariable Long id) {
        try {
            NoticiaDTO noticia = noticiaService.despublicar(id);
            return ResponseEntity.ok(noticia);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Remove notícia
     * 
     * @param id ID da notícia
     * @return Resposta de sucesso
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        try {
            noticiaService.delete(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Conta notícias publicadas
     * 
     * @return Número de notícias publicadas
     */
    @GetMapping("/count/publicadas")
    public ResponseEntity<Long> countPublicadas() {
        long count = noticiaService.countPublicadas();
        return ResponseEntity.ok(count);
    }

    /**
     * Conta total de notícias
     * 
     * @return Número total de notícias
     */
    @GetMapping("/count/total")
    public ResponseEntity<Long> countTotal() {
        long count = noticiaService.countTotal();
        return ResponseEntity.ok(count);
    }

    /**
     * Classe para request de criação de notícia
     */
    public static class CriarNoticiaRequest {
        @Valid
        private NoticiaDTO noticia;
        
        private Long autorId;
        
        @NotEmpty(message = "Pelo menos uma categoria deve ser selecionada")
        private List<Long> categoriaIds;

        public NoticiaDTO getNoticia() {
            return noticia;
        }

        public void setNoticia(NoticiaDTO noticia) {
            this.noticia = noticia;
        }

        public Long getAutorId() {
            return autorId;
        }

        public void setAutorId(Long autorId) {
            this.autorId = autorId;
        }

        public List<Long> getCategoriaIds() {
            return categoriaIds;
        }

        public void setCategoriaIds(List<Long> categoriaIds) {
            this.categoriaIds = categoriaIds;
        }
    }

    /**
     * Classe para request de atualização de notícia
     */
    public static class AtualizarNoticiaRequest {
        @Valid
        private NoticiaDTO noticia;
        
        @NotEmpty(message = "Pelo menos uma categoria deve ser selecionada")
        private List<Long> categoriaIds;

        public NoticiaDTO getNoticia() {
            return noticia;
        }

        public void setNoticia(NoticiaDTO noticia) {
            this.noticia = noticia;
        }

        public List<Long> getCategoriaIds() {
            return categoriaIds;
        }

        public void setCategoriaIds(List<Long> categoriaIds) {
            this.categoriaIds = categoriaIds;
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