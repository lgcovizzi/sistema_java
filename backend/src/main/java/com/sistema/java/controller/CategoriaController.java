package com.sistema.java.controller;

import com.sistema.java.model.dto.CategoriaDTO;
import com.sistema.java.service.CategoriaService;
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
 * Controller REST para gerenciamento de categorias
 * 
 * @author Sistema Java
 * @version 1.0
 */
@RestController
@RequestMapping("/api/categorias")
@Validated
@CrossOrigin(origins = "*")
public class CategoriaController {

    @Autowired
    private CategoriaService categoriaService;

    /**
     * Lista todas as categorias com paginação
     * 
     * @param pageable Configuração de paginação
     * @return Página de categorias
     */
    @GetMapping
    public ResponseEntity<Page<CategoriaDTO>> findAll(
            @PageableDefault(size = 20, sort = "nome") Pageable pageable) {
        Page<CategoriaDTO> categorias = categoriaService.findAll(pageable);
        return ResponseEntity.ok(categorias);
    }

    /**
     * Lista categorias ativas com paginação
     * 
     * @param pageable Configuração de paginação
     * @return Página de categorias ativas
     */
    @GetMapping("/ativas")
    public ResponseEntity<Page<CategoriaDTO>> findAtivas(
            @PageableDefault(size = 20, sort = "nome") Pageable pageable) {
        Page<CategoriaDTO> categorias = categoriaService.findAtivas(pageable);
        return ResponseEntity.ok(categorias);
    }

    /**
     * Lista categorias ativas para seleção
     * 
     * @return Lista de categorias ativas
     */
    @GetMapping("/selecao")
    public ResponseEntity<List<CategoriaDTO>> findAtivasParaSelecao() {
        List<CategoriaDTO> categorias = categoriaService.findAtivasParaSelecao();
        return ResponseEntity.ok(categorias);
    }

    /**
     * Busca categoria por ID
     * 
     * @param id ID da categoria
     * @return Categoria encontrada
     */
    @GetMapping("/{id}")
    public ResponseEntity<CategoriaDTO> findById(@PathVariable Long id) {
        Optional<CategoriaDTO> categoria = categoriaService.findById(id);
        return categoria.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Busca categoria por nome
     * 
     * @param nome Nome da categoria
     * @return Categoria encontrada
     */
    @GetMapping("/nome/{nome}")
    public ResponseEntity<CategoriaDTO> findByNome(@PathVariable String nome) {
        Optional<CategoriaDTO> categoria = categoriaService.findByNome(nome);
        return categoria.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Busca categorias por nome (busca parcial)
     * 
     * @param nome Nome ou parte do nome
     * @param pageable Configuração de paginação
     * @return Página de categorias encontradas
     */
    @GetMapping("/buscar")
    public ResponseEntity<Page<CategoriaDTO>> findByNomeContaining(
            @RequestParam @NotBlank(message = "Nome não pode estar vazio") String nome,
            @PageableDefault(size = 20, sort = "nome") Pageable pageable) {
        Page<CategoriaDTO> categorias = categoriaService.findByNomeContaining(nome, pageable);
        return ResponseEntity.ok(categorias);
    }

    /**
     * Busca por termo em nome ou descrição
     * 
     * @param termo Termo de busca
     * @param pageable Configuração de paginação
     * @return Página de categorias encontradas
     */
    @GetMapping("/buscar-termo")
    public ResponseEntity<Page<CategoriaDTO>> buscarPorTermo(
            @RequestParam @NotBlank(message = "Termo não pode estar vazio") String termo,
            @PageableDefault(size = 20, sort = "nome") Pageable pageable) {
        Page<CategoriaDTO> categorias = categoriaService.buscarPorTermo(termo, pageable);
        return ResponseEntity.ok(categorias);
    }

    /**
     * Busca categorias com notícias publicadas
     * 
     * @return Lista de categorias com notícias
     */
    @GetMapping("/com-noticias")
    public ResponseEntity<List<CategoriaDTO>> findComNoticiasPublicadas() {
        List<CategoriaDTO> categorias = categoriaService.findComNoticiasPublicadas();
        return ResponseEntity.ok(categorias);
    }

    /**
     * Busca categorias mais utilizadas
     * 
     * @param limite Número máximo de categorias
     * @return Lista de categorias mais utilizadas
     */
    @GetMapping("/mais-utilizadas")
    public ResponseEntity<List<CategoriaDTO>> findMaisUtilizadas(
            @RequestParam(defaultValue = "10") int limite) {
        List<CategoriaDTO> categorias = categoriaService.findMaisUtilizadas(limite);
        return ResponseEntity.ok(categorias);
    }

    /**
     * Cria nova categoria
     * 
     * @param categoriaDTO Dados da categoria
     * @return Categoria criada
     */
    @PostMapping
    public ResponseEntity<CategoriaDTO> create(@Valid @RequestBody CategoriaDTO categoriaDTO) {
        try {
            CategoriaDTO novaCategoria = categoriaService.create(categoriaDTO);
            return ResponseEntity.status(HttpStatus.CREATED).body(novaCategoria);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Atualiza categoria existente
     * 
     * @param id ID da categoria
     * @param categoriaDTO Dados atualizados
     * @return Categoria atualizada
     */
    @PutMapping("/{id}")
    public ResponseEntity<CategoriaDTO> update(
            @PathVariable Long id,
            @Valid @RequestBody CategoriaDTO categoriaDTO) {
        try {
            CategoriaDTO categoriaAtualizada = categoriaService.update(id, categoriaDTO);
            return ResponseEntity.ok(categoriaAtualizada);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Ativa categoria
     * 
     * @param id ID da categoria
     * @return Categoria ativada
     */
    @PatchMapping("/{id}/ativar")
    public ResponseEntity<CategoriaDTO> ativar(@PathVariable Long id) {
        try {
            CategoriaDTO categoria = categoriaService.ativar(id);
            return ResponseEntity.ok(categoria);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Desativa categoria
     * 
     * @param id ID da categoria
     * @return Categoria desativada
     */
    @PatchMapping("/{id}/desativar")
    public ResponseEntity<CategoriaDTO> desativar(@PathVariable Long id) {
        try {
            CategoriaDTO categoria = categoriaService.desativar(id);
            return ResponseEntity.ok(categoria);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Remove categoria
     * 
     * @param id ID da categoria
     * @return Resposta de sucesso
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        try {
            categoriaService.delete(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    /**
     * Verifica se categoria pode ser removida
     * 
     * @param id ID da categoria
     * @return true se pode ser removida
     */
    @GetMapping("/{id}/pode-remover")
    public ResponseEntity<Boolean> podeSerRemovida(@PathVariable Long id) {
        boolean podeRemover = categoriaService.podeSerRemovida(id);
        return ResponseEntity.ok(podeRemover);
    }

    /**
     * Conta categorias ativas
     * 
     * @return Número de categorias ativas
     */
    @GetMapping("/count/ativas")
    public ResponseEntity<Long> countAtivas() {
        long count = categoriaService.countAtivas();
        return ResponseEntity.ok(count);
    }

    /**
     * Conta total de categorias
     * 
     * @return Número total de categorias
     */
    @GetMapping("/count/total")
    public ResponseEntity<Long> countTotal() {
        long count = categoriaService.countTotal();
        return ResponseEntity.ok(count);
    }

    /**
     * Obtém estatísticas das categorias
     * 
     * @return Lista com estatísticas das categorias
     */
    @GetMapping("/estatisticas")
    public ResponseEntity<List<Object[]>> getEstatisticas() {
        List<Object[]> estatisticas = categoriaService.getEstatisticas();
        return ResponseEntity.ok(estatisticas);
    }

    /**
     * Ativa categorias em lote
     * 
     * @param request Lista de IDs das categorias
     * @return Número de categorias ativadas
     */
    @PatchMapping("/ativar-lote")
    public ResponseEntity<Integer> ativarLote(@Valid @RequestBody AtivarLoteRequest request) {
        int count = categoriaService.atualizarStatusLote(request.getIds(), true);
        return ResponseEntity.ok(count);
    }

    /**
     * Desativa categorias em lote
     * 
     * @param request Lista de IDs das categorias
     * @return Número de categorias desativadas
     */
    @PatchMapping("/desativar-lote")
    public ResponseEntity<Integer> desativarLote(@Valid @RequestBody DesativarLoteRequest request) {
        int count = categoriaService.atualizarStatusLote(request.getIds(), false);
        return ResponseEntity.ok(count);
    }

    /**
     * Classe para request de ativação em lote
     */
    public static class AtivarLoteRequest {
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
     * Classe para request de desativação em lote
     */
    public static class DesativarLoteRequest {
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