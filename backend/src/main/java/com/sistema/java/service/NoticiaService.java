package com.sistema.java.service;

import com.sistema.java.model.dto.CategoriaDTO;
import com.sistema.java.model.dto.NoticiaDTO;
import com.sistema.java.model.dto.UsuarioDTO;
import com.sistema.java.model.entity.Categoria;
import com.sistema.java.model.entity.Noticia;
import com.sistema.java.model.entity.Usuario;
import com.sistema.java.repository.CategoriaRepository;
import com.sistema.java.repository.ComentarioRepository;
import com.sistema.java.repository.NoticiaRepository;
import com.sistema.java.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service para gerenciamento de notícias
 * 
 * @author Sistema Java
 * @version 1.0
 */
@Service
@Transactional
public class NoticiaService {

    @Autowired
    private NoticiaRepository noticiaRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private ComentarioRepository comentarioRepository;

    /**
     * Busca todas as notícias com paginação
     * 
     * @param pageable Configuração de paginação
     * @return Página de notícias
     */
    @Transactional(readOnly = true)
    public Page<NoticiaDTO> findAll(Pageable pageable) {
        return noticiaRepository.findAll(pageable)
                .map(this::convertToDTO);
    }

    /**
     * Busca notícias publicadas
     * 
     * @param pageable Configuração de paginação
     * @return Página de notícias publicadas
     */
    @Transactional(readOnly = true)
    public Page<NoticiaDTO> findPublicadas(Pageable pageable) {
        return noticiaRepository.findByPublicadaTrueOrderByDataPublicacaoDesc(pageable)
                .map(this::convertToDTO);
    }

    /**
     * Busca notícia por ID
     * 
     * @param id ID da notícia
     * @return Optional com a notícia encontrada
     */
    @Transactional(readOnly = true)
    public Optional<NoticiaDTO> findById(Long id) {
        return noticiaRepository.findById(id)
                .map(this::convertToDTO);
    }

    /**
     * Busca notícias por autor
     * 
     * @param autorId ID do autor
     * @param pageable Configuração de paginação
     * @return Página de notícias do autor
     */
    @Transactional(readOnly = true)
    public Page<NoticiaDTO> findByAutor(Long autorId, Pageable pageable) {
        Usuario autor = usuarioRepository.findById(autorId)
                .orElseThrow(() -> new IllegalArgumentException("Autor não encontrado: " + autorId));
        
        return noticiaRepository.findByAutor(autor, pageable)
                .map(this::convertToDTO);
    }

    /**
     * Busca notícias por categoria
     * 
     * @param categoriaId ID da categoria
     * @param publicada Status de publicação
     * @param pageable Configuração de paginação
     * @return Página de notícias da categoria
     */
    @Transactional(readOnly = true)
    public Page<NoticiaDTO> findByCategoria(Long categoriaId, boolean publicada, Pageable pageable) {
        return noticiaRepository.findByCategoriaAndPublicada(categoriaId, publicada, pageable)
                .map(this::convertToDTO);
    }

    /**
     * Busca notícias por título
     * 
     * @param titulo Título ou parte do título
     * @param publicada Status de publicação
     * @param pageable Configuração de paginação
     * @return Página de notícias encontradas
     */
    @Transactional(readOnly = true)
    public Page<NoticiaDTO> findByTitulo(String titulo, boolean publicada, Pageable pageable) {
        if (publicada) {
            return noticiaRepository.findByTituloContainingIgnoreCaseAndPublicadaTrue(titulo, pageable)
                    .map(this::convertToDTO);
        } else {
            return noticiaRepository.findByTituloContainingIgnoreCase(titulo, pageable)
                    .map(this::convertToDTO);
        }
    }

    /**
     * Busca notícias por termo
     * 
     * @param termo Termo de busca
     * @param publicada Status de publicação
     * @param pageable Configuração de paginação
     * @return Página de notícias encontradas
     */
    @Transactional(readOnly = true)
    public Page<NoticiaDTO> buscar(String termo, boolean publicada, Pageable pageable) {
        return noticiaRepository.buscarPorTermo(termo, publicada, pageable)
                .map(this::convertToDTO);
    }

    /**
     * Busca notícias mais recentes
     * 
     * @param limite Número máximo de notícias
     * @return Lista de notícias mais recentes
     */
    @Transactional(readOnly = true)
    public List<NoticiaDTO> findRecentes(int limite) {
        return noticiaRepository.findTop10ByPublicadaTrueOrderByDataPublicacaoDesc()
                .stream()
                .limit(limite)
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Cria nova notícia
     * 
     * @param noticiaDTO Dados da notícia
     * @param autorId ID do autor
     * @param categoriaIds IDs das categorias
     * @return Notícia criada
     */
    public NoticiaDTO create(NoticiaDTO noticiaDTO, Long autorId, List<Long> categoriaIds) {
        Usuario autor = usuarioRepository.findById(autorId)
                .orElseThrow(() -> new IllegalArgumentException("Autor não encontrado: " + autorId));

        List<Categoria> categorias = categoriaIds.stream()
                .map(id -> categoriaRepository.findById(id)
                        .orElseThrow(() -> new IllegalArgumentException("Categoria não encontrada: " + id)))
                .collect(Collectors.toList());

        Noticia noticia = convertToEntity(noticiaDTO);
        noticia.setAutor(autor);
        noticia.setCategorias(categorias);
        noticia.setPublicada(false);
        noticia.setDataCriacao(LocalDateTime.now());
        noticia.setDataAtualizacao(LocalDateTime.now());

        Noticia savedNoticia = noticiaRepository.save(noticia);
        return convertToDTO(savedNoticia);
    }

    /**
     * Atualiza notícia existente
     * 
     * @param id ID da notícia
     * @param noticiaDTO Dados atualizados
     * @param categoriaIds IDs das categorias
     * @return Notícia atualizada
     */
    public NoticiaDTO update(Long id, NoticiaDTO noticiaDTO, List<Long> categoriaIds) {
        Noticia noticia = noticiaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Notícia não encontrada: " + id));

        List<Categoria> categorias = categoriaIds.stream()
                .map(catId -> categoriaRepository.findById(catId)
                        .orElseThrow(() -> new IllegalArgumentException("Categoria não encontrada: " + catId)))
                .collect(Collectors.toList());

        noticia.setTitulo(noticiaDTO.getTitulo());
        noticia.setConteudo(noticiaDTO.getConteudo());
        noticia.setResumo(noticiaDTO.getResumo());
        noticia.setCategorias(categorias);
        noticia.setDataAtualizacao(LocalDateTime.now());

        Noticia savedNoticia = noticiaRepository.save(noticia);
        return convertToDTO(savedNoticia);
    }

    /**
     * Publica notícia
     * 
     * @param id ID da notícia
     * @return Notícia publicada
     */
    public NoticiaDTO publicar(Long id) {
        Noticia noticia = noticiaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Notícia não encontrada: " + id));

        noticia.publicar();
        noticia.setDataAtualizacao(LocalDateTime.now());

        Noticia savedNoticia = noticiaRepository.save(noticia);
        return convertToDTO(savedNoticia);
    }

    /**
     * Despublica notícia
     * 
     * @param id ID da notícia
     * @return Notícia despublicada
     */
    public NoticiaDTO despublicar(Long id) {
        Noticia noticia = noticiaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Notícia não encontrada: " + id));

        noticia.despublicar();
        noticia.setDataAtualizacao(LocalDateTime.now());

        Noticia savedNoticia = noticiaRepository.save(noticia);
        return convertToDTO(savedNoticia);
    }

    /**
     * Remove notícia
     * 
     * @param id ID da notícia
     */
    public void delete(Long id) {
        if (!noticiaRepository.existsById(id)) {
            throw new IllegalArgumentException("Notícia não encontrada: " + id);
        }
        noticiaRepository.deleteById(id);
    }

    /**
     * Conta notícias publicadas
     * 
     * @return Número de notícias publicadas
     */
    @Transactional(readOnly = true)
    public long countPublicadas() {
        return noticiaRepository.countByPublicada(true);
    }

    /**
     * Conta total de notícias
     * 
     * @return Número total de notícias
     */
    @Transactional(readOnly = true)
    public long countTotal() {
        return noticiaRepository.count();
    }

    /**
     * Busca notícias com mais comentários
     * 
     * @param limite Número máximo de notícias
     * @return Lista de notícias mais comentadas
     */
    @Transactional(readOnly = true)
    public List<NoticiaDTO> findMaisComentadas(int limite) {
        Pageable pageable = Pageable.ofSize(limite);
        return noticiaRepository.findNoticiasComMaisComentarios(true, pageable)
                .getContent()
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Converte entidade para DTO
     * 
     * @param noticia Entidade notícia
     * @return DTO da notícia
     */
    private NoticiaDTO convertToDTO(Noticia noticia) {
        NoticiaDTO dto = new NoticiaDTO();
        dto.setId(noticia.getId());
        dto.setTitulo(noticia.getTitulo());
        dto.setConteudo(noticia.getConteudo());
        dto.setResumo(noticia.getResumo());
        dto.setPublicada(noticia.getPublicada());
        dto.setDataPublicacao(noticia.getDataPublicacao());
        dto.setDataCriacao(noticia.getDataCriacao());
        dto.setDataAtualizacao(noticia.getDataAtualizacao());
        // Autor - converter para UsuarioDTO
        UsuarioDTO autorDTO = new UsuarioDTO();
        autorDTO.setId(noticia.getAutor().getId());
        autorDTO.setNome(noticia.getAutor().getNome());
        autorDTO.setSobrenome(noticia.getAutor().getSobrenome());
        autorDTO.setEmail(noticia.getAutor().getEmail());
        autorDTO.setPapel(noticia.getAutor().getPapel());
        dto.setAutor(autorDTO);
        
        // Categorias - converter para List<CategoriaDTO>
        List<CategoriaDTO> categoriaDTOs = noticia.getCategorias().stream()
                .map(categoria -> new CategoriaDTO(categoria.getId(), categoria.getNome(), categoria.getDescricao(), categoria.getAtiva(), categoria.getDataCriacao()))
                .collect(Collectors.toList());
        dto.setCategorias(categoriaDTOs);
        
        // Total de comentários aprovados
        long totalComentarios = comentarioRepository.countByNoticiaAndAprovado(noticia, true);
        dto.setTotalComentarios((int) totalComentarios);
        
        return dto;
    }

    /**
     * Converte DTO para entidade
     * 
     * @param noticiaDTO DTO da notícia
     * @return Entidade notícia
     */
    private Noticia convertToEntity(NoticiaDTO noticiaDTO) {
        Noticia noticia = new Noticia();
        noticia.setId(noticiaDTO.getId());
        noticia.setTitulo(noticiaDTO.getTitulo());
        noticia.setConteudo(noticiaDTO.getConteudo());
        noticia.setResumo(noticiaDTO.getResumo());
        noticia.setPublicada(noticiaDTO.getPublicada());
        return noticia;
    }

    /**
     * Conta notícias com filtros
     */
    @Transactional(readOnly = true)
    public long contarComFiltros(String titulo, Long autorId, Long categoriaId, Boolean publicada, 
                                java.util.Date dataInicio, java.util.Date dataFim, String termo) {
        // Implementação simplificada - retorna contagem total por enquanto
        return noticiaRepository.count();
    }

    /**
     * Conta notícias com filtros simplificados
     */
    @Transactional(readOnly = true)
    public long contarComFiltros(String termo, Long categoriaId) {
        if (termo != null && !termo.trim().isEmpty() && categoriaId != null) {
            // Busca por termo e categoria
            return noticiaRepository.countByTituloContainingIgnoreCaseAndCategoriasIdAndPublicada(
                termo.trim(), categoriaId, true);
        } else if (termo != null && !termo.trim().isEmpty()) {
            // Busca apenas por termo
            return noticiaRepository.countByTituloContainingIgnoreCaseAndPublicada(termo.trim(), true);
        } else if (categoriaId != null) {
            // Busca apenas por categoria
            return noticiaRepository.countByCategoriasIdAndPublicada(categoriaId, true);
        } else {
            // Sem filtros - retorna todas publicadas
            return noticiaRepository.countByPublicada(true);
        }
    }

    /**
     * Busca notícias com filtros
     */
    @Transactional(readOnly = true)
    public List<NoticiaDTO> buscarComFiltros(String titulo, Long autorId, Long categoriaId, Boolean publicada,
                                           java.util.Date dataInicio, java.util.Date dataFim, String termo,
                                           String ordenacao, int first, int pageSize) {
        // Implementação simplificada - retorna lista vazia por enquanto
        return List.of();
    }

    /**
     * Busca notícias com filtros simplificados
     */
    @Transactional(readOnly = true)
    public List<NoticiaDTO> buscarComFiltros(String termo, Long categoriaId, String ordenacao, int pagina, int tamanho) {
        // Implementação simplificada - retorna as notícias mais recentes
        return findRecentes(tamanho);
    }

    /**
     * Conta total de notícias
     */
    @Transactional(readOnly = true)
    public long contar() {
        return noticiaRepository.count();
    }

    @Transactional(readOnly = true)
    public Long contarPublicadas() {
        // Implementação temporária
        return 0L;
    }

    @Transactional(readOnly = true)
    public Long contarRascunhos() {
        // Implementação temporária
        return 0L;
    }

    @Transactional(readOnly = true)
    public Long contarPublicadasHoje() {
        // Implementação temporária
        return 0L;
    }
}