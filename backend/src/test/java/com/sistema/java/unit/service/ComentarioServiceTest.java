package com.sistema.java.unit.service;

import com.sistema.java.model.entity.Comentario;
import com.sistema.java.model.entity.Noticia;
import com.sistema.java.model.entity.Usuario;
import com.sistema.java.model.enums.PapelUsuario;
import com.sistema.java.repository.ComentarioRepository;
import com.sistema.java.service.ComentarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * Testes unitários para ComentarioService
 * Referência: Testes e Qualidade de Código - project_rules.md
 * Referência: Padrões de Teste - project_rules.md
 */
@ExtendWith(MockitoExtension.class)
class ComentarioServiceTest {

    @Mock
    private ComentarioRepository comentarioRepository;

    @InjectMocks
    private ComentarioService comentarioService;

    private Comentario comentario;
    private Usuario autor;
    private Noticia noticia;

    @BeforeEach
    void setUp() {
        // Arrange - Configuração dos objetos de teste
        autor = new Usuario();
        autor.setId(1L);
        autor.setNome("João");
        autor.setSobrenome("Silva");
        autor.setEmail("joao@teste.com");
        autor.setPapel(PapelUsuario.USUARIO);

        noticia = new Noticia();
        noticia.setId(1L);
        noticia.setTitulo("Título da Notícia");
        noticia.setConteudo("Conteúdo da notícia");
        noticia.setPublicada(true);
        noticia.setDataPublicacao(LocalDateTime.now());

        comentario = new Comentario();
        comentario.setId(1L);
        comentario.setConteudo("Este é um comentário de teste");
        comentario.setAutor(autor);
        comentario.setNoticia(noticia);
        comentario.setAprovado(false);
        comentario.setDataCriacao(LocalDateTime.now());
    }

    @Test
    void should_SaveComentario_When_ValidDataProvided() {
        // Arrange
        when(comentarioRepository.save(any(Comentario.class))).thenReturn(comentario);

        // Act
        Comentario resultado = comentarioService.salvar(comentario);

        // Assert
        assertThat(resultado).isNotNull();
        assertThat(resultado.getConteudo()).isEqualTo("Este é um comentário de teste");
        assertThat(resultado.getAutor()).isEqualTo(autor);
        assertThat(resultado.getNoticia()).isEqualTo(noticia);
        assertThat(resultado.isAprovado()).isFalse();
        verify(comentarioRepository).save(comentario);
    }

    @Test
    void should_FindComentarioById_When_ComentarioExists() {
        // Arrange
        when(comentarioRepository.findById(1L)).thenReturn(Optional.of(comentario));

        // Act
        Optional<Comentario> resultado = comentarioService.buscarPorId(1L);

        // Assert
        assertThat(resultado).isPresent();
        assertThat(resultado.get().getConteudo()).isEqualTo("Este é um comentário de teste");
        verify(comentarioRepository).findById(1L);
    }

    @Test
    void should_ReturnEmpty_When_ComentarioNotExists() {
        // Arrange
        when(comentarioRepository.findById(999L)).thenReturn(Optional.empty());

        // Act
        Optional<Comentario> resultado = comentarioService.buscarPorId(999L);

        // Assert
        assertThat(resultado).isEmpty();
        verify(comentarioRepository).findById(999L);
    }

    @Test
    void should_FindApprovedComentariosByNoticia_When_RequestingPublicComments() {
        // Arrange
        comentario.setAprovado(true);
        List<Comentario> comentarios = Arrays.asList(comentario);
        Page<Comentario> page = new PageImpl<>(comentarios);
        Pageable pageable = PageRequest.of(0, 10);
        
        when(comentarioRepository.findByNoticiaAndAprovadoTrueOrderByDataCriacaoDesc(noticia, pageable))
            .thenReturn(page);

        // Act
        Page<Comentario> resultado = comentarioService.buscarAprovadosPorNoticia(noticia, pageable);

        // Assert
        assertThat(resultado.getContent()).hasSize(1);
        assertThat(resultado.getContent().get(0).isAprovado()).isTrue();
        assertThat(resultado.getContent().get(0).getNoticia()).isEqualTo(noticia);
        verify(comentarioRepository).findByNoticiaAndAprovadoTrueOrderByDataCriacaoDesc(noticia, pageable);
    }

    @Test
    void should_FindPendingComentarios_When_RequestingModerationQueue() {
        // Arrange
        List<Comentario> comentarios = Arrays.asList(comentario);
        Page<Comentario> page = new PageImpl<>(comentarios);
        Pageable pageable = PageRequest.of(0, 10);
        
        when(comentarioRepository.findByAprovadoFalseOrderByDataCriacaoAsc(pageable))
            .thenReturn(page);

        // Act
        Page<Comentario> resultado = comentarioService.buscarPendentes(pageable);

        // Assert
        assertThat(resultado.getContent()).hasSize(1);
        assertThat(resultado.getContent().get(0).isAprovado()).isFalse();
        verify(comentarioRepository).findByAprovadoFalseOrderByDataCriacaoAsc(pageable);
    }

    @Test
    void should_ApproveComentario_When_ValidIdProvided() {
        // Arrange
        when(comentarioRepository.findById(1L)).thenReturn(Optional.of(comentario));
        when(comentarioRepository.save(any(Comentario.class))).thenReturn(comentario);

        // Act
        Comentario resultado = comentarioService.aprovar(1L);

        // Assert
        assertThat(resultado.isAprovado()).isTrue();
        verify(comentarioRepository).findById(1L);
        verify(comentarioRepository).save(comentario);
    }

    @Test
    void should_ThrowException_When_ApprovingNonExistentComentario() {
        // Arrange
        when(comentarioRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> comentarioService.aprovar(999L))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Comentário não encontrado");
        
        verify(comentarioRepository).findById(999L);
        verify(comentarioRepository, never()).save(any());
    }

    @Test
    void should_RejectComentario_When_ValidIdProvided() {
        // Arrange
        comentario.setAprovado(true);
        when(comentarioRepository.findById(1L)).thenReturn(Optional.of(comentario));
        when(comentarioRepository.save(any(Comentario.class))).thenReturn(comentario);

        // Act
        Comentario resultado = comentarioService.rejeitar(1L);

        // Assert
        assertThat(resultado.isAprovado()).isFalse();
        verify(comentarioRepository).findById(1L);
        verify(comentarioRepository).save(comentario);
    }

    @Test
    void should_DeleteComentario_When_ValidIdProvided() {
        // Arrange
        when(comentarioRepository.existsById(1L)).thenReturn(true);
        doNothing().when(comentarioRepository).deleteById(1L);

        // Act
        comentarioService.deletar(1L);

        // Assert
        verify(comentarioRepository).existsById(1L);
        verify(comentarioRepository).deleteById(1L);
    }

    @Test
    void should_ThrowException_When_DeletingNonExistentComentario() {
        // Arrange
        when(comentarioRepository.existsById(999L)).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> comentarioService.deletar(999L))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Comentário não encontrado");
        
        verify(comentarioRepository).existsById(999L);
        verify(comentarioRepository, never()).deleteById(anyLong());
    }

    @Test
    void should_FindComentariosByAuthor_When_ValidAuthorProvided() {
        // Arrange
        List<Comentario> comentarios = Arrays.asList(comentario);
        Page<Comentario> page = new PageImpl<>(comentarios);
        Pageable pageable = PageRequest.of(0, 10);
        
        when(comentarioRepository.findByAutorOrderByDataCriacaoDesc(autor, pageable))
            .thenReturn(page);

        // Act
        Page<Comentario> resultado = comentarioService.buscarPorAutor(autor, pageable);

        // Assert
        assertThat(resultado.getContent()).hasSize(1);
        assertThat(resultado.getContent().get(0).getAutor()).isEqualTo(autor);
        verify(comentarioRepository).findByAutorOrderByDataCriacaoDesc(autor, pageable);
    }

    @Test
    void should_CountPendingComentarios_When_RequestingModerationCount() {
        // Arrange
        when(comentarioRepository.countByAprovadoFalse()).thenReturn(5L);

        // Act
        Long resultado = comentarioService.contarPendentes();

        // Assert
        assertThat(resultado).isEqualTo(5L);
        verify(comentarioRepository).countByAprovadoFalse();
    }

    @Test
    void should_CountApprovedComentariosByNoticia_When_RequestingNoticiaStats() {
        // Arrange
        when(comentarioRepository.countByNoticiaAndAprovadoTrue(noticia)).thenReturn(3L);

        // Act
        Long resultado = comentarioService.contarAprovadosPorNoticia(noticia);

        // Assert
        assertThat(resultado).isEqualTo(3L);
        verify(comentarioRepository).countByNoticiaAndAprovadoTrue(noticia);
    }

    @Test
    void should_UpdateComentario_When_ValidDataProvided() {
        // Arrange
        Comentario comentarioAtualizado = new Comentario();
        comentarioAtualizado.setId(1L);
        comentarioAtualizado.setConteudo("Comentário atualizado");
        comentarioAtualizado.setAutor(autor);
        comentarioAtualizado.setNoticia(noticia);
        
        when(comentarioRepository.findById(1L)).thenReturn(Optional.of(comentario));
        when(comentarioRepository.save(any(Comentario.class))).thenReturn(comentarioAtualizado);

        // Act
        Comentario resultado = comentarioService.atualizar(1L, comentarioAtualizado);

        // Assert
        assertThat(resultado.getConteudo()).isEqualTo("Comentário atualizado");
        verify(comentarioRepository).findById(1L);
        verify(comentarioRepository).save(any(Comentario.class));
    }

    @Test
    void should_ThrowException_When_UpdatingNonExistentComentario() {
        // Arrange
        Comentario comentarioAtualizado = new Comentario();
        comentarioAtualizado.setConteudo("Comentário atualizado");
        
        when(comentarioRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> comentarioService.atualizar(999L, comentarioAtualizado))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Comentário não encontrado");
        
        verify(comentarioRepository).findById(999L);
        verify(comentarioRepository, never()).save(any());
    }
}