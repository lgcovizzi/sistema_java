package com.sistema.java.service;

import com.sistema.java.model.dto.CategoriaDTO;
import com.sistema.java.model.entity.Categoria;
import com.sistema.java.repository.CategoriaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service para gerenciamento de categorias
 * 
 * @author Sistema Java
 * @version 1.0
 */
@Service
@Transactional
public class CategoriaService {

    @Autowired
    private CategoriaRepository categoriaRepository;

    /**
     * Busca todas as categorias com paginação
     * 
     * @param pageable Configuração de paginação
     * @return Página de categorias
     */
    @Transactional(readOnly = true)
    public Page<CategoriaDTO> findAll(Pageable pageable) {
        return categoriaRepository.findAll(pageable)
                .map(this::convertToDTO);
    }

    /**
     * Busca categorias ativas
     * 
     * @param pageable Configuração de paginação
     * @return Página de categorias ativas
     */
    @Transactional(readOnly = true)
    public Page<CategoriaDTO> findAtivas(Pageable pageable) {
        return categoriaRepository.findByAtivaTrue(pageable)
                .map(this::convertToDTO);
    }

    /**
     * Busca categoria por ID
     * 
     * @param id ID da categoria
     * @return Optional com a categoria encontrada
     */
    @Transactional(readOnly = true)
    public Optional<CategoriaDTO> findById(Long id) {
        return categoriaRepository.findById(id)
                .map(this::convertToDTO);
    }

    /**
     * Busca categoria por nome
     * 
     * @param nome Nome da categoria
     * @return Optional com a categoria encontrada
     */
    @Transactional(readOnly = true)
    public Optional<CategoriaDTO> findByNome(String nome) {
        return categoriaRepository.findByNome(nome)
                .map(this::convertToDTO);
    }

    /**
     * Busca categorias por nome (busca parcial)
     * 
     * @param nome Nome ou parte do nome
     * @param pageable Configuração de paginação
     * @return Página de categorias encontradas
     */
    @Transactional(readOnly = true)
    public Page<CategoriaDTO> findByNomeContaining(String nome, Pageable pageable) {
        return categoriaRepository.findByNomeContainingIgnoreCase(nome, pageable)
                .map(this::convertToDTO);
    }

    /**
     * Busca categorias ativas para seleção
     * 
     * @return Lista de categorias ativas
     */
    @Transactional(readOnly = true)
    public List<CategoriaDTO> findAtivasParaSelecao() {
        return categoriaRepository.findAtivasParaSelecao()
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Busca categorias com notícias publicadas
     * 
     * @return Lista de categorias com notícias
     */
    @Transactional(readOnly = true)
    public List<CategoriaDTO> findComNoticiasPublicadas() {
        return categoriaRepository.findCategoriasComNoticiasPublicadas()
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Busca categorias mais utilizadas
     * 
     * @param limite Número máximo de categorias
     * @return Lista de categorias mais utilizadas
     */
    @Transactional(readOnly = true)
    public List<CategoriaDTO> findMaisUtilizadas(int limite) {
        return categoriaRepository.findCategoriasMaisUtilizadas(limite)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Busca por termo em nome ou descrição
     * 
     * @param termo Termo de busca
     * @param pageable Configuração de paginação
     * @return Página de categorias encontradas
     */
    @Transactional(readOnly = true)
    public Page<CategoriaDTO> buscarPorTermo(String termo, Pageable pageable) {
        return categoriaRepository.buscarPorTermo(termo, pageable)
                .map(this::convertToDTO);
    }

    /**
     * Cria nova categoria
     * 
     * @param categoriaDTO Dados da categoria
     * @return Categoria criada
     */
    public CategoriaDTO create(CategoriaDTO categoriaDTO) {
        // Verifica se já existe categoria com o mesmo nome
        if (categoriaRepository.existsByNome(categoriaDTO.getNome())) {
            throw new IllegalArgumentException("Já existe uma categoria com o nome: " + categoriaDTO.getNome());
        }

        Categoria categoria = convertToEntity(categoriaDTO);
        categoria.setAtiva(true);
        categoria.setDataCriacao(LocalDateTime.now());

        Categoria savedCategoria = categoriaRepository.save(categoria);
        return convertToDTO(savedCategoria);
    }

    /**
     * Atualiza categoria existente
     * 
     * @param id ID da categoria
     * @param categoriaDTO Dados atualizados
     * @return Categoria atualizada
     */
    public CategoriaDTO update(Long id, CategoriaDTO categoriaDTO) {
        Categoria categoria = categoriaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Categoria não encontrada: " + id));

        // Verifica se o novo nome já existe em outra categoria
        Optional<Categoria> existente = categoriaRepository.findByNome(categoriaDTO.getNome());
        if (existente.isPresent() && !existente.get().getId().equals(id)) {
            throw new IllegalArgumentException("Já existe uma categoria com o nome: " + categoriaDTO.getNome());
        }

        categoria.setNome(categoriaDTO.getNome());
        categoria.setDescricao(categoriaDTO.getDescricao());

        Categoria savedCategoria = categoriaRepository.save(categoria);
        return convertToDTO(savedCategoria);
    }

    /**
     * Ativa categoria
     * 
     * @param id ID da categoria
     * @return Categoria ativada
     */
    public CategoriaDTO ativar(Long id) {
        Categoria categoria = categoriaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Categoria não encontrada: " + id));

        categoria.setAtiva(true);
        Categoria savedCategoria = categoriaRepository.save(categoria);
        return convertToDTO(savedCategoria);
    }

    /**
     * Desativa categoria
     * 
     * @param id ID da categoria
     * @return Categoria desativada
     */
    public CategoriaDTO desativar(Long id) {
        Categoria categoria = categoriaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Categoria não encontrada: " + id));

        categoria.setAtiva(false);
        Categoria savedCategoria = categoriaRepository.save(categoria);
        return convertToDTO(savedCategoria);
    }

    /**
     * Remove categoria
     * 
     * @param id ID da categoria
     */
    public void delete(Long id) {
        if (!categoriaRepository.existsById(id)) {
            throw new IllegalArgumentException("Categoria não encontrada: " + id);
        }

        // Verifica se a categoria pode ser removida
        if (!categoriaRepository.podeSerRemovida(id)) {
            throw new IllegalStateException("Categoria não pode ser removida pois possui notícias associadas");
        }

        categoriaRepository.deleteById(id);
    }

    /**
     * Conta categorias ativas
     * 
     * @return Número de categorias ativas
     */
    @Transactional(readOnly = true)
    public long countAtivas() {
        return categoriaRepository.countByAtiva(true);
    }

    /**
     * Conta total de categorias
     * 
     * @return Número total de categorias
     */
    @Transactional(readOnly = true)
    public long countTotal() {
        return categoriaRepository.count();
    }

    /**
     * Obtém estatísticas das categorias
     * 
     * @return Lista com estatísticas das categorias
     */
    @Transactional(readOnly = true)
    public List<Object[]> getEstatisticas() {
        return categoriaRepository.getEstatisticasCategorias();
    }

    /**
     * Verifica se categoria pode ser removida
     * 
     * @param id ID da categoria
     * @return true se pode ser removida
     */
    @Transactional(readOnly = true)
    public boolean podeSerRemovida(Long id) {
        return categoriaRepository.podeSerRemovida(id);
    }

    /**
     * Atualiza status ativo em lote
     * 
     * @param ids Lista de IDs das categorias
     * @param ativo Novo status
     * @return Número de categorias atualizadas
     */
    public int atualizarStatusLote(List<Long> ids, boolean ativo) {
        return categoriaRepository.atualizarStatusAtivo(ids, ativo);
    }

    /**
     * Converte entidade para DTO
     * 
     * @param categoria Entidade categoria
     * @return DTO da categoria
     */
    private CategoriaDTO convertToDTO(Categoria categoria) {
        CategoriaDTO dto = new CategoriaDTO();
        dto.setId(categoria.getId());
        dto.setNome(categoria.getNome());
        dto.setDescricao(categoria.getDescricao());
        dto.setAtiva(categoria.getAtiva());
        dto.setDataCriacao(categoria.getDataCriacao());
        
        // Conta notícias da categoria
        long totalNoticias = categoriaRepository.contarNoticiasPorCategoria(categoria.getId());
        dto.setTotalNoticias((int) totalNoticias);
        
        return dto;
    }

    /**
     * Converte DTO para entidade
     * 
     * @param categoriaDTO DTO da categoria
     * @return Entidade categoria
     */
    private Categoria convertToEntity(CategoriaDTO categoriaDTO) {
        Categoria categoria = new Categoria();
        categoria.setId(categoriaDTO.getId());
        categoria.setNome(categoriaDTO.getNome());
        categoria.setDescricao(categoriaDTO.getDescricao());
        categoria.setAtiva(categoriaDTO.getAtiva());
        return categoria;
    }
}