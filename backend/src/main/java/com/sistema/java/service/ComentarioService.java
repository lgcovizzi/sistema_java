package com.sistema.java.service;

import com.sistema.java.model.dto.ComentarioDTO;
import com.sistema.java.model.entity.Comentario;
import com.sistema.java.model.entity.Noticia;
import com.sistema.java.model.entity.Usuario;
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
import java.util.stream.Collectors;

/**
 * Service para gerenciamento de comentários
 * Referência: Sistema de Temas Claros e Escuros - project_rules.md
 * Este service deve considerar preferências de tema do usuário para formatação de dados
 * 
 * @author Sistema Java
 * @version 1.0
 */
@Service
@Transactional
public class ComentarioService {

    @Autowired
    private ComentarioRepository comentarioRepository;

    @Autowired
    private NoticiaRepository noticiaRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    /**
     * Busca todos os comentários com paginação
     * Referência: Sistema de Temas Claros e Escuros - project_rules.md
     * Dados retornados devem incluir informações de tema para renderização adequada
     * 
     * @param pageable Configuração de paginação
     * @return Página de comentários
     */
    @Transactional(readOnly = true)
    public Page<ComentarioDTO> findAll(Pageable pageable) {
        return comentarioRepository.findAll(pageable)
                .map(this::convertToDTO);
    }

    /**
     * Busca comentários aprovados
     * Referência: Sistema de Temas Claros e Escuros - project_rules.md
     * Interface deve adaptar cores de status conforme tema ativo
     * 
     * @param pageable Configuração de paginação
     * @return Página de comentários aprovados
     */
    @Transactional(readOnly = true)
    public Page<ComentarioDTO> findAprovados(Pageable pageable) {
        return comentarioRepository.findByAprovadoTrueOrderByDataCriacaoDesc(pageable)
                .map(this::convertToDTO);
    }

    /**
     * Busca comentários pendentes de aprovação
     * 
     * @param pageable Configuração de paginação
     * @return Página de comentários pendentes
     */
    @Transactional(readOnly = true)
    public Page<ComentarioDTO> findPendentes(Pageable pageable) {
        return comentarioRepository.findByAprovadoFalseOrderByDataCriacaoAsc(pageable)
                .map(this::convertToDTO);
    }

    /**
     * Busca comentário por ID
     * 
     * @param id ID do comentário
     * @return Optional com o comentário encontrado
     */
    @Transactional(readOnly = true)
    public Optional<ComentarioDTO> findById(Long id) {
        return comentarioRepository.findById(id)
                .map(this::convertToDTO);
    }

    /**
     * Busca comentários por notícia
     * 
     * @param noticiaId ID da notícia
     * @param aprovado Status de aprovação
     * @param pageable Configuração de paginação
     * @return Página de comentários da notícia
     */
    @Transactional(readOnly = true)
    public Page<ComentarioDTO> findByNoticia(Long noticiaId, boolean aprovado, Pageable pageable) {
        Noticia noticia = noticiaRepository.findById(noticiaId)
                .orElseThrow(() -> new IllegalArgumentException("Notícia não encontrada: " + noticiaId));
        
        return comentarioRepository.findByNoticiaAndAprovado(noticia, aprovado, pageable)
                .map(this::convertToDTO);
    }

    /**
     * Busca comentários por autor
     * 
     * @param autorId ID do autor
     * @param pageable Configuração de paginação
     * @return Página de comentários do autor
     */
    @Transactional(readOnly = true)
    public Page<ComentarioDTO> findByAutor(Long autorId, Pageable pageable) {
        Usuario autor = usuarioRepository.findById(autorId)
                .orElseThrow(() -> new IllegalArgumentException("Autor não encontrado: " + autorId));
        
        return comentarioRepository.findByAutor(autor, pageable)
                .map(this::convertToDTO);
    }

    /**
     * Busca comentários por período
     * 
     * @param dataInicio Data de início
     * @param dataFim Data de fim
     * @param pageable Configuração de paginação
     * @return Página de comentários do período
     */
    @Transactional(readOnly = true)
    public Page<ComentarioDTO> findByPeriodo(LocalDateTime dataInicio, LocalDateTime dataFim, Pageable pageable) {
        return comentarioRepository.findByDataCriacaoBetween(dataInicio, dataFim, pageable)
                .map(this::convertToDTO);
    }

    /**
     * Busca comentários por termo
     * 
     * @param termo Termo de busca
     * @param aprovado Status de aprovação
     * @param pageable Configuração de paginação
     * @return Página de comentários encontrados
     */
    @Transactional(readOnly = true)
    public Page<ComentarioDTO> buscar(String termo, boolean aprovado, Pageable pageable) {
        return comentarioRepository.buscarPorTermo(termo, aprovado, pageable)
                .map(this::convertToDTO);
    }

    /**
     * Busca comentários recentes
     * 
     * @param limite Número máximo de comentários
     * @return Lista de comentários recentes
     */
    @Transactional(readOnly = true)
    public List<ComentarioDTO> findRecentes(int limite) {
        return comentarioRepository.findTop10ByAprovadoTrueOrderByDataCriacaoDesc()
                .stream()
                .limit(limite)
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Busca comentários pendentes com detalhes
     * 
     * @param pageable Configuração de paginação
     * @return Página de comentários pendentes com detalhes
     */
    @Transactional(readOnly = true)
    public Page<ComentarioDTO> findPendentesComDetalhes(Pageable pageable) {
        return comentarioRepository.findComentariosPendentesComDetalhes(pageable)
                .map(this::convertToDTO);
    }

    /**
     * Cria novo comentário
     * 
     * @param comentarioDTO Dados do comentário
     * @param autorId ID do autor
     * @param noticiaId ID da notícia
     * @return Comentário criado
     */
    public ComentarioDTO create(ComentarioDTO comentarioDTO, Long autorId, Long noticiaId) {
        Usuario autor = usuarioRepository.findById(autorId)
                .orElseThrow(() -> new IllegalArgumentException("Autor não encontrado: " + autorId));

        Noticia noticia = noticiaRepository.findById(noticiaId)
                .orElseThrow(() -> new IllegalArgumentException("Notícia não encontrada: " + noticiaId));

        // Verifica se a notícia está publicada
        if (!noticia.getPublicada()) {
            throw new IllegalStateException("Não é possível comentar em notícia não publicada");
        }

        Comentario comentario = convertToEntity(comentarioDTO);
        comentario.setAutor(autor);
        comentario.setNoticia(noticia);
        comentario.setAprovado(false); // Comentários precisam ser aprovados
        comentario.setDataCriacao(LocalDateTime.now());

        Comentario savedComentario = comentarioRepository.save(comentario);
        return convertToDTO(savedComentario);
    }

    /**
     * Atualiza comentário existente
     * 
     * @param id ID do comentário
     * @param comentarioDTO Dados atualizados
     * @return Comentário atualizado
     */
    public ComentarioDTO update(Long id, ComentarioDTO comentarioDTO) {
        Comentario comentario = comentarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Comentário não encontrado: " + id));

        comentario.setConteudo(comentarioDTO.getConteudo());

        Comentario savedComentario = comentarioRepository.save(comentario);
        return convertToDTO(savedComentario);
    }

    /**
     * Aprova comentário
     * 
     * @param id ID do comentário
     * @return Comentário aprovado
     */
    public ComentarioDTO aprovar(Long id) {
        Comentario comentario = comentarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Comentário não encontrado: " + id));

        comentario.setAprovado(true);
        Comentario savedComentario = comentarioRepository.save(comentario);
        return convertToDTO(savedComentario);
    }

    /**
     * Reprova comentário
     * 
     * @param id ID do comentário
     * @return Comentário reprovado
     */
    public ComentarioDTO reprovar(Long id) {
        Comentario comentario = comentarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Comentário não encontrado: " + id));

        comentario.setAprovado(false);
        Comentario savedComentario = comentarioRepository.save(comentario);
        return convertToDTO(savedComentario);
    }

    /**
     * Remove comentário
     * 
     * @param id ID do comentário
     */
    public void delete(Long id) {
        if (!comentarioRepository.existsById(id)) {
            throw new IllegalArgumentException("Comentário não encontrado: " + id);
        }
        comentarioRepository.deleteById(id);
    }

    /**
     * Aprova comentários em lote
     * 
     * @param ids Lista de IDs dos comentários
     * @return Número de comentários aprovados
     */
    public int aprovarLote(List<Long> ids) {
        return comentarioRepository.atualizarStatusAprovacao(ids, true);
    }

    /**
     * Reprova comentários em lote
     * 
     * @param ids Lista de IDs dos comentários
     * @return Número de comentários reprovados
     */
    public int reprovarLote(List<Long> ids) {
        return comentarioRepository.atualizarStatusAprovacao(ids, false);
    }

    /**
     * Remove comentários antigos não aprovados
     * 
     * @param diasAntigos Número de dias
     * @return Número de comentários removidos
     */
    public int removerAntigosNaoAprovados(int diasAntigos) {
        LocalDateTime dataLimite = LocalDateTime.now().minusDays(diasAntigos);
        return comentarioRepository.removerAntigosNaoAprovados(dataLimite);
    }

    /**
     * Conta comentários aprovados
     * 
     * @return Número de comentários aprovados
     */
    @Transactional(readOnly = true)
    public long countAprovados() {
        return comentarioRepository.countByAprovado(true);
    }

    /**
     * Conta comentários pendentes
     * 
     * @return Número de comentários pendentes
     */
    @Transactional(readOnly = true)
    public long countPendentes() {
        return comentarioRepository.countByAprovado(false);
    }

    /**
     * Conta total de comentários
     * 
     * @return Número total de comentários
     */
    @Transactional(readOnly = true)
    public long countTotal() {
        return comentarioRepository.count();
    }

    /**
     * Obtém estatísticas dos comentários
     * 
     * @return Lista com estatísticas dos comentários
     */
    @Transactional(readOnly = true)
    public List<Object[]> getEstatisticas() {
        return comentarioRepository.getEstatisticasComentarios();
    }

    /**
     * Busca usuários mais ativos nos comentários
     * 
     * @param limite Número máximo de usuários
     * @return Lista de usuários mais ativos
     */
    @Transactional(readOnly = true)
    public List<Object[]> findUsuariosMaisAtivos(int limite) {
        return comentarioRepository.findUsuariosMaisAtivos(limite);
    }

    /**
     * Converte entidade para DTO
     * 
     * @param comentario Entidade comentário
     * @return DTO do comentário
     */
    private ComentarioDTO convertToDTO(Comentario comentario) {
        ComentarioDTO dto = new ComentarioDTO();
        dto.setId(comentario.getId());
        dto.setConteudo(comentario.getConteudo());
        dto.setAprovado(comentario.getAprovado());
        dto.setDataCriacao(comentario.getDataCriacao());
        dto.setAutor(comentario.getAutor().getNome());
        dto.setNoticiaId(comentario.getNoticia().getId());
        dto.setNoticiaTitle(comentario.getNoticia().getTitulo());
        return dto;
    }

    /**
     * Converte DTO para entidade
     * 
     * @param comentarioDTO DTO do comentário
     * @return Entidade comentário
     */
    private Comentario convertToEntity(ComentarioDTO comentarioDTO) {
        Comentario comentario = new Comentario();
        comentario.setId(comentarioDTO.getId());
        comentario.setConteudo(comentarioDTO.getConteudo());
        comentario.setAprovado(comentarioDTO.getAprovado());
        return comentario;
    }
}