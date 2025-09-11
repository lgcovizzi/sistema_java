package com.sistema.java.unit.service;

import com.sistema.java.model.entity.Noticia;
import com.sistema.java.model.entity.Usuario;
import com.sistema.java.model.entity.Categoria;
import com.sistema.java.model.enums.PapelUsuario;
import com.sistema.java.repository.NoticiaRepository;
import com.sistema.java.service.NoticiaService;
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
import java.util.Set;
import java.util.HashSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * Testes unitários para NoticiaService
 * Referência: Testes e Qualidade de Código - project_rules.md
 * Referência: Padrões de Teste - project_rules.md
 */
@ExtendWith(MockitoExtension.class)
class NoticiaServiceTest {

    @Mock
    private NoticiaRepository noticiaRepository;

    @InjectMocks
    private NoticiaService noticiaService;

    private Noticia noticia;
    private Usuario autor;
    private Categoria categoria;

    @BeforeEach
    void setUp() {
        // Arrange - Configuração dos objetos de teste
        autor = new Usuario();
        autor.setId(1L);
        autor.setNome("João");
        autor.setSobrenome("Silva");
        autor.setEmail("joao@teste.com");
        autor.setPapel(PapelUsuario.COLABORADOR);

        categoria = new Categoria();
        categoria.setId(1L);
        categoria.setNome("Tecnologia");
        categoria.setAtiva(true);

        noticia = new Noticia();
        noticia.setId(1L);
        noticia.setTitulo("Título da Notícia");
        noticia.setConteudo("Conteúdo da notícia de teste");
        noticia.setResumo("Resumo da notícia");
        noticia.setAutor(autor);
        noticia.setPublicada(false);
        noticia.setDataCriacao(LocalDateTime.now());
        noticia.setCategorias(new HashSet<>(Arrays.asList(categoria)));
    }

    @Test
    void should_SaveNoticia_When_ValidDataProvided() {
        // Arrange
        when(noticiaRepository.save(any(Noticia.class))).thenReturn(noticia);

        // Act
        Noticia resultado = noticiaService.salvar(noticia);

        // Assert
        assertThat(resultado).isNotNull();
        assertThat(resultado.getTitulo()).isEqualTo("Título da Notícia");
        assertThat(resultado.getAutor()).isEqualTo(autor);
        verify(noticiaRepository).save(noticia);
    }

    @Test
    void should_FindNoticiaById_When_NoticiaExists() {
        // Arrange
        when(noticiaRepository.findById(1L)).thenReturn(Optional.of(noticia));

        // Act
        Optional<Noticia> resultado = noticiaService.buscarPorId(1L);

        // Assert
        assertThat(resultado).isPresent();
        assertThat(resultado.get().getTitulo()).isEqualTo("Título da Notícia");
        verify(noticiaRepository).findById(1L);
    }

    @Test
    void should_ReturnEmpty_When_NoticiaNotExists() {
        // Arrange
        when(noticiaRepository.findById(999L)).thenReturn(Optional.empty());

        // Act
        Optional<Noticia> resultado = noticiaService.buscarPorId(999L);

        // Assert
        assertThat(resultado).isEmpty();
        verify(noticiaRepository).findById(999L);
    }

    @Test
    void should_FindPublishedNoticias_When_RequestingPublicContent() {
        // Arrange
        noticia.setPublicada(true);
        noticia.setDataPublicacao(LocalDateTime.now());
        List<Noticia> noticias = Arrays.asList(noticia);
        Page<Noticia> page = new PageImpl<>(noticias);
        Pageable pageable = PageRequest.of(0, 10);
        
        when(noticiaRepository.findByPublicadaTrueOrderByDataPublicacaoDesc(pageable))
            .thenReturn(page);

        // Act
        Page<Noticia> resultado = noticiaService.buscarPublicadas(pageable);

        // Assert
        assertThat(resultado.getContent()).hasSize(1);
        assertThat(resultado.getContent().get(0).isPublicada()).isTrue();
        verify(noticiaRepository).findByPublicadaTrueOrderByDataPublicacaoDesc(pageable);
    }

    @Test
    void should_PublishNoticia_When_ValidNoticiaProvided() {
        // Arrange
        noticia.setPublicada(false);
        noticia.setDataPublicacao(null);
        when(noticiaRepository.findById(1L)).thenReturn(Optional.of(noticia));
        when(noticiaRepository.save(any(Noticia.class))).thenReturn(noticia);

        // Act
        Noticia resultado = noticiaService.publicar(1L);

        // Assert
        assertThat(resultado.isPublicada()).isTrue();
        assertThat(resultado.getDataPublicacao()).isNotNull();
        verify(noticiaRepository).findById(1L);
        verify(noticiaRepository).save(noticia);
    }

    @Test
    void should_ThrowException_When_PublishingNonExistentNoticia() {
        // Arrange
        when(noticiaRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> noticiaService.publicar(999L))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Notícia não encontrada");
        
        verify(noticiaRepository).findById(999L);
        verify(noticiaRepository, never()).save(any());
    }

    @Test
    void should_UnpublishNoticia_When_ValidNoticiaProvided() {
        // Arrange
        noticia.setPublicada(true);
        noticia.setDataPublicacao(LocalDateTime.now());
        when(noticiaRepository.findById(1L)).thenReturn(Optional.of(noticia));
        when(noticiaRepository.save(any(Noticia.class))).thenReturn(noticia);

        // Act
        Noticia resultado = noticiaService.despublicar(1L);

        // Assert
        assertThat(resultado.isPublicada()).isFalse();
        verify(noticiaRepository).findById(1L);
        verify(noticiaRepository).save(noticia);
    }

    @Test
    void should_DeleteNoticia_When_ValidIdProvided() {
        // Arrange
        when(noticiaRepository.existsById(1L)).thenReturn(true);
        doNothing().when(noticiaRepository).deleteById(1L);

        // Act
        noticiaService.deletar(1L);

        // Assert
        verify(noticiaRepository).existsById(1L);
        verify(noticiaRepository).deleteById(1L);
    }

    @Test
    void should_ThrowException_When_DeletingNonExistentNoticia() {
        // Arrange
        when(noticiaRepository.existsById(999L)).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> noticiaService.deletar(999L))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Notícia não encontrada");
        
        verify(noticiaRepository).existsById(999L);
        verify(noticiaRepository, never()).deleteById(anyLong());
    }

    @Test
    void should_FindNoticiasByAuthor_When_ValidAuthorProvided() {
        // Arrange
        List<Noticia> noticias = Arrays.asList(noticia);
        Page<Noticia> page = new PageImpl<>(noticias);
        Pageable pageable = PageRequest.of(0, 10);
        
        when(noticiaRepository.findByAutorOrderByDataCriacaoDesc(autor, pageable))
            .thenReturn(page);

        // Act
        Page<Noticia> resultado = noticiaService.buscarPorAutor(autor, pageable);

        // Assert
        assertThat(resultado.getContent()).hasSize(1);
        assertThat(resultado.getContent().get(0).getAutor()).isEqualTo(autor);
        verify(noticiaRepository).findByAutorOrderByDataCriacaoDesc(autor, pageable);
    }

    @Test
    void should_FindNoticiasByCategory_When_ValidCategoryProvided() {
        // Arrange
        List<Noticia> noticias = Arrays.asList(noticia);
        Page<Noticia> page = new PageImpl<>(noticias);
        Pageable pageable = PageRequest.of(0, 10);
        
        when(noticiaRepository.findByCategoriasContainingAndPublicadaTrueOrderByDataPublicacaoDesc(categoria, pageable))
            .thenReturn(page);

        // Act
        Page<Noticia> resultado = noticiaService.buscarPorCategoria(categoria, pageable);

        // Assert
        assertThat(resultado.getContent()).hasSize(1);
        assertThat(resultado.getContent().get(0).getCategorias()).contains(categoria);
        verify(noticiaRepository).findByCategoriasContainingAndPublicadaTrueOrderByDataPublicacaoDesc(categoria, pageable);
    }
}