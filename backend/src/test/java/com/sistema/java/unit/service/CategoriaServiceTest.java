package com.sistema.java.unit.service;

import com.sistema.java.model.entity.Categoria;
import com.sistema.java.repository.CategoriaRepository;
import com.sistema.java.service.CategoriaService;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Testes unitários para CategoriaService
 * Referência: Testes e Qualidade de Código - project_rules.md
 * Referência: Padrões de Teste - project_rules.md
 */
@ExtendWith(MockitoExtension.class)
class CategoriaServiceTest {

    @Mock
    private CategoriaRepository categoriaRepository;

    @InjectMocks
    private CategoriaService categoriaService;

    private Categoria categoria;

    @BeforeEach
    void setUp() {
        // Arrange - Configuração dos objetos de teste
        categoria = new Categoria();
        categoria.setId(1L);
        categoria.setNome("Tecnologia");
        categoria.setDescricao("Categoria sobre tecnologia");
        categoria.setAtiva(true);
        categoria.setDataCriacao(LocalDateTime.now());
    }

    @Test
    void should_SaveCategoria_When_ValidDataProvided() {
        // Arrange
        when(categoriaRepository.existsByNome("Tecnologia")).thenReturn(false);
        when(categoriaRepository.save(any(Categoria.class))).thenReturn(categoria);

        // Act
        Categoria resultado = categoriaService.salvar(categoria);

        // Assert
        assertThat(resultado).isNotNull();
        assertThat(resultado.getNome()).isEqualTo("Tecnologia");
        assertThat(resultado.isAtiva()).isTrue();
        verify(categoriaRepository).existsByNome("Tecnologia");
        verify(categoriaRepository).save(categoria);
    }

    @Test
    void should_ThrowException_When_CategoryNameAlreadyExists() {
        // Arrange
        when(categoriaRepository.existsByNome("Tecnologia")).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> categoriaService.salvar(categoria))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Categoria com este nome já existe");
        
        verify(categoriaRepository).existsByNome("Tecnologia");
        verify(categoriaRepository, never()).save(any());
    }

    @Test
    void should_FindCategoriaById_When_CategoriaExists() {
        // Arrange
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(categoria));

        // Act
        Optional<Categoria> resultado = categoriaService.buscarPorId(1L);

        // Assert
        assertThat(resultado).isPresent();
        assertThat(resultado.get().getNome()).isEqualTo("Tecnologia");
        verify(categoriaRepository).findById(1L);
    }

    @Test
    void should_ReturnEmpty_When_CategoriaNotExists() {
        // Arrange
        when(categoriaRepository.findById(999L)).thenReturn(Optional.empty());

        // Act
        Optional<Categoria> resultado = categoriaService.buscarPorId(999L);

        // Assert
        assertThat(resultado).isEmpty();
        verify(categoriaRepository).findById(999L);
    }

    @Test
    void should_FindAllActiveCategorias_When_RequestingActiveCategories() {
        // Arrange
        Categoria categoria2 = new Categoria();
        categoria2.setId(2L);
        categoria2.setNome("Esportes");
        categoria2.setAtiva(true);
        
        List<Categoria> categorias = Arrays.asList(categoria, categoria2);
        when(categoriaRepository.findByAtivaTrue()).thenReturn(categorias);

        // Act
        List<Categoria> resultado = categoriaService.buscarAtivas();

        // Assert
        assertThat(resultado).hasSize(2);
        assertThat(resultado).allMatch(Categoria::isAtiva);
        verify(categoriaRepository).findByAtivaTrue();
    }

    @Test
    void should_FindAllCategorias_When_RequestingWithPagination() {
        // Arrange
        List<Categoria> categorias = Arrays.asList(categoria);
        Page<Categoria> page = new PageImpl<>(categorias);
        Pageable pageable = PageRequest.of(0, 10);
        
        when(categoriaRepository.findAll(pageable)).thenReturn(page);

        // Act
        Page<Categoria> resultado = categoriaService.buscarTodas(pageable);

        // Assert
        assertThat(resultado.getContent()).hasSize(1);
        assertThat(resultado.getContent().get(0).getNome()).isEqualTo("Tecnologia");
        verify(categoriaRepository).findAll(pageable);
    }

    @Test
    void should_UpdateCategoria_When_ValidDataProvided() {
        // Arrange
        Categoria categoriaAtualizada = new Categoria();
        categoriaAtualizada.setId(1L);
        categoriaAtualizada.setNome("Tecnologia Avançada");
        categoriaAtualizada.setDescricao("Categoria sobre tecnologia avançada");
        categoriaAtualizada.setAtiva(true);
        
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(categoria));
        when(categoriaRepository.existsByNomeAndIdNot("Tecnologia Avançada", 1L)).thenReturn(false);
        when(categoriaRepository.save(any(Categoria.class))).thenReturn(categoriaAtualizada);

        // Act
        Categoria resultado = categoriaService.atualizar(1L, categoriaAtualizada);

        // Assert
        assertThat(resultado.getNome()).isEqualTo("Tecnologia Avançada");
        verify(categoriaRepository).findById(1L);
        verify(categoriaRepository).existsByNomeAndIdNot("Tecnologia Avançada", 1L);
        verify(categoriaRepository).save(any(Categoria.class));
    }

    @Test
    void should_ThrowException_When_UpdatingWithExistingName() {
        // Arrange
        Categoria categoriaAtualizada = new Categoria();
        categoriaAtualizada.setNome("Esportes");
        
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(categoria));
        when(categoriaRepository.existsByNomeAndIdNot("Esportes", 1L)).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> categoriaService.atualizar(1L, categoriaAtualizada))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Categoria com este nome já existe");
        
        verify(categoriaRepository).findById(1L);
        verify(categoriaRepository).existsByNomeAndIdNot("Esportes", 1L);
        verify(categoriaRepository, never()).save(any());
    }

    @Test
    void should_DeactivateCategoria_When_ValidIdProvided() {
        // Arrange
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(categoria));
        when(categoriaRepository.save(any(Categoria.class))).thenReturn(categoria);

        // Act
        Categoria resultado = categoriaService.desativar(1L);

        // Assert
        assertThat(resultado.isAtiva()).isFalse();
        verify(categoriaRepository).findById(1L);
        verify(categoriaRepository).save(categoria);
    }

    @Test
    void should_ActivateCategoria_When_ValidIdProvided() {
        // Arrange
        categoria.setAtiva(false);
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(categoria));
        when(categoriaRepository.save(any(Categoria.class))).thenReturn(categoria);

        // Act
        Categoria resultado = categoriaService.ativar(1L);

        // Assert
        assertThat(resultado.isAtiva()).isTrue();
        verify(categoriaRepository).findById(1L);
        verify(categoriaRepository).save(categoria);
    }

    @Test
    void should_ThrowException_When_DeactivatingNonExistentCategoria() {
        // Arrange
        when(categoriaRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> categoriaService.desativar(999L))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Categoria não encontrada");
        
        verify(categoriaRepository).findById(999L);
        verify(categoriaRepository, never()).save(any());
    }

    @Test
    void should_SearchCategoriasByName_When_ValidSearchTermProvided() {
        // Arrange
        List<Categoria> categorias = Arrays.asList(categoria);
        when(categoriaRepository.findByNomeContainingIgnoreCase("tecno")).thenReturn(categorias);

        // Act
        List<Categoria> resultado = categoriaService.buscarPorNome("tecno");

        // Assert
        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getNome()).containsIgnoringCase("tecno");
        verify(categoriaRepository).findByNomeContainingIgnoreCase("tecno");
    }

    @Test
    void should_CountActiveCategories_When_RequestingCount() {
        // Arrange
        when(categoriaRepository.countByAtivaTrue()).thenReturn(5L);

        // Act
        Long resultado = categoriaService.contarAtivas();

        // Assert
        assertThat(resultado).isEqualTo(5L);
        verify(categoriaRepository).countByAtivaTrue();
    }
}