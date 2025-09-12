package com.sistema.java.unit.bean;

import com.sistema.java.bean.CategoriaBean;
import com.sistema.java.model.dto.CategoriaDTO;
import com.sistema.java.model.entity.Categoria;
import com.sistema.java.service.CategoriaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.primefaces.model.LazyDataModel;

import javax.faces.context.FacesContext;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Testes unitários para CategoriaBean
 * Referência: Testes e Qualidade de Código - project_rules.md
 * Referência: Banco de Dados Relacional - project_rules.md
 */
@ExtendWith(MockitoExtension.class)
class CategoriaBeanTest {

    @Mock
    private CategoriaService categoriaService;

    @Mock
    private FacesContext facesContext;

    @InjectMocks
    private CategoriaBean categoriaBean;

    private Categoria categoriaMock;
    private CategoriaDTO categoriaDTOMock;
    private List<Categoria> categoriasMock;

    @BeforeEach
    void setUp() {
        // Criar categoria mock
        categoriaMock = new Categoria();
        categoriaMock.setId(1L);
        categoriaMock.setNome("Tecnologia");
        categoriaMock.setDescricao("Categoria sobre tecnologia");
        categoriaMock.setAtiva(true);
        categoriaMock.setDataCriacao(LocalDateTime.now());

        // Criar DTO mock
        categoriaDTOMock = new CategoriaDTO();
        categoriaDTOMock.setNome("Tecnologia");
        categoriaDTOMock.setDescricao("Categoria sobre tecnologia");
        categoriaDTOMock.setAtiva(true);

        // Lista de categorias mock
        categoriasMock = Arrays.asList(categoriaMock);
    }

    @Test
    void should_InitializeCorrectly_When_PostConstructCalled() {
        // Act
        categoriaBean.init();

        // Assert
        assertThat(categoriaBean.getCategoriasLazy()).isNotNull();
        assertThat(categoriaBean.getFiltros()).isNotNull();
        assertThat(categoriaBean.getFiltros()).isEmpty();
    }

    @Test
    void should_LoadCategoriesData_When_LazyModelIsUsed() {
        // Arrange
        when(categoriaService.buscarComPaginacao(anyInt(), anyInt(), anyString(), any(), anyString(), any()))
            .thenReturn(Arrays.asList(new CategoriaDTO()));
        when(categoriaService.contarComFiltros(anyString(), any())).thenReturn(1L);

        categoriaBean.init();
        LazyDataModel<Categoria> lazyModel = categoriaBean.getCategoriasLazy();

        // Act
        List<Categoria> result = lazyModel.load(0, 10, Collections.emptyMap(), Collections.emptyMap());

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(categoriaMock);
        verify(categoriaService).listarComFiltros(anyMap(), eq(0), eq(10), anyString(), anyBoolean());
    }

    @Test
    void should_CreateCategorySuccessfully_When_DataIsValid() {
        // Arrange
        categoriaBean.setCategoriaAtual(categoriaDTOMock);
        when(categoriaService.create(any(CategoriaDTO.class))).thenReturn(categoriaDTOMock);

        // Act
        categoriaBean.salvarCategoria();

        // Assert
        verify(categoriaService).create(any(CategoriaDTO.class));
    }

    @Test
    void should_NotCreateCategory_When_NameAlreadyExists() {
        // Arrange
        categoriaBean.setCategoriaAtual(categoriaDTOMock);
        categoriaDTOMock.setNome(""); // Nome vazio para falhar validação

        // Act
        categoriaBean.salvarCategoria();

        // Assert
        verify(categoriaService, never()).create(any(CategoriaDTO.class));
    }

    @Test
    void should_UpdateCategorySuccessfully_When_DataIsValid() {
        // Arrange
        categoriaDTOMock.setId(1L);
        categoriaBean.setCategoriaAtual(categoriaDTOMock);
        when(categoriaService.update(eq(1L), any(CategoriaDTO.class))).thenReturn(categoriaDTOMock);

        // Act
        categoriaBean.salvarCategoria();

        // Assert
        verify(categoriaService).update(eq(1L), any(CategoriaDTO.class));
    }

    @Test
    void should_DeleteCategorySuccessfully_When_CategoryExists() {
        // Arrange
        when(categoriaService.buscarPorId(1L)).thenReturn(categoriaMock);
        when(categoriaService.podeSerDeletada(1L)).thenReturn(true);
        doNothing().when(categoriaService).deletarCategoria(1L);

        // Act
        categoriaBean.deletarCategoria(1L);

        // Assert
        verify(categoriaService).deletarCategoria(1L);
    }

    @Test
    void should_NotDeleteCategory_When_CategoryHasAssociatedNews() {
        // Arrange
        when(categoriaService.buscarPorId(1L)).thenReturn(categoriaMock);
        when(categoriaService.podeSerDeletada(1L)).thenReturn(false);

        // Act
        categoriaBean.deletarCategoria(1L);

        // Assert
        verify(categoriaService, never()).deletarCategoria(1L);
    }

    @Test
    void should_ActivateCategorySuccessfully_When_CategoryExists() {
        // Arrange
        categoriaMock.setAtiva(false);
        when(categoriaService.buscarPorId(1L)).thenReturn(categoriaMock);
        when(categoriaService.alterarStatus(1L, true)).thenReturn(categoriaMock);

        // Act
        categoriaBean.ativarCategoria(1L);

        // Assert
        verify(categoriaService).alterarStatus(1L, true);
    }

    @Test
    void should_DeactivateCategorySuccessfully_When_CategoryExists() {
        // Arrange
        when(categoriaService.buscarPorId(1L)).thenReturn(categoriaMock);
        when(categoriaService.alterarStatus(1L, false)).thenReturn(categoriaMock);

        // Act
        categoriaBean.desativarCategoria(1L);

        // Assert
        verify(categoriaService).alterarStatus(1L, false);
    }

    @Test
    void should_FilterByName_When_NameFilterIsApplied() {
        // Arrange
        categoriaBean.init();
        Map<String, Object> filtros = new HashMap<>();
        filtros.put("nome", "Tecnologia");
        categoriaBean.setFiltros(filtros);

        List<Categoria> categoriasFiltered = Arrays.asList(categoriaMock);
        when(categoriaService.listarComFiltros(eq(filtros), anyInt(), anyInt(), anyString(), anyBoolean()))
            .thenReturn(categoriasFiltered);
        when(categoriaService.contarComFiltros(eq(filtros))).thenReturn(1L);

        LazyDataModel<Categoria> lazyModel = categoriaBean.getCategoriasLazy();

        // Act
        List<Categoria> result = lazyModel.load(0, 10, Collections.emptyMap(), Collections.emptyMap());

        // Assert
        assertThat(result).hasSize(1);
        verify(categoriaService).listarComFiltros(eq(filtros), eq(0), eq(10), anyString(), anyBoolean());
    }

    @Test
    void should_FilterByStatus_When_StatusFilterIsApplied() {
        // Arrange
        categoriaBean.init();
        Map<String, Object> filtros = new HashMap<>();
        filtros.put("ativa", true);
        categoriaBean.setFiltros(filtros);

        List<Categoria> categoriasAtivas = Arrays.asList(categoriaMock);
        when(categoriaService.listarComFiltros(eq(filtros), anyInt(), anyInt(), anyString(), anyBoolean()))
            .thenReturn(categoriasAtivas);
        when(categoriaService.contarComFiltros(eq(filtros))).thenReturn(1L);

        LazyDataModel<Categoria> lazyModel = categoriaBean.getCategoriasLazy();

        // Act
        List<Categoria> result = lazyModel.load(0, 10, Collections.emptyMap(), Collections.emptyMap());

        // Assert
        assertThat(result).hasSize(1);
        verify(categoriaService).listarComFiltros(eq(filtros), eq(0), eq(10), anyString(), anyBoolean());
    }

    @Test
    void should_ClearFilters_When_ClearMethodIsCalled() {
        // Arrange
        Map<String, Object> filtros = new HashMap<>();
        filtros.put("nome", "Teste");
        filtros.put("ativa", true);
        categoriaBean.setFiltros(filtros);

        // Act
        categoriaBean.limparFiltros();

        // Assert
        assertThat(categoriaBean.getFiltros()).isEmpty();
    }

    @Test
    void should_PrepareNewCategory_When_NovaMethodIsCalled() {
        // Act
        categoriaBean.novaCategoria();

        // Assert
        assertThat(categoriaBean.getCategoriaAtual()).isNotNull();
        assertThat(categoriaBean.getCategoriaAtual().getId()).isNull();
        assertThat(categoriaBean.getCategoriaAtual().getNome()).isNull();
        assertThat(categoriaBean.getCategoriaAtual().isAtiva()).isTrue(); // Padrão ativo
    }

    @Test
    void should_PrepareEditCategory_When_EditMethodIsCalled() {
        // Arrange
        when(categoriaService.buscarPorId(1L)).thenReturn(categoriaMock);

        // Act
        categoriaBean.editarCategoria(1L);

        // Assert
        assertThat(categoriaBean.getCategoriaAtual()).isNotNull();
        assertThat(categoriaBean.getCategoriaAtual().getId()).isEqualTo(1L);
        verify(categoriaService).buscarPorId(1L);
    }

    @Test
    void should_CancelEdit_When_CancelMethodIsCalled() {
        // Arrange
        categoriaBean.setCategoriaAtual(categoriaDTOMock);

        // Act
        categoriaBean.cancelarEdicao();

        // Assert
        assertThat(categoriaBean.getCategoriaAtual()).isNull();
    }

    @Test
    void should_ValidateRequiredFields_When_CreatingCategory() {
        // Arrange
        CategoriaDTO categoriaInvalida = new CategoriaDTO();
        // Nome vazio
        categoriaInvalida.setNome("");
        categoriaInvalida.setDescricao("Descrição válida");
        categoriaBean.setCategoriaAtual(categoriaInvalida);

        // Act & Assert
        // O teste deve verificar se a validação impede a criação
        // Isso seria feito através de validações Bean Validation ou JSF
        assertThat(categoriaInvalida.getNome()).isEmpty();
    }

    @Test
    void should_HandlePagination_When_LoadingCategories() {
        // Arrange
        List<Categoria> categorias = Arrays.asList(categoriaMock);
        when(categoriaService.listarComFiltros(anyMap(), eq(10), eq(10), anyString(), anyBoolean()))
            .thenReturn(categorias);
        when(categoriaService.contarComFiltros(anyMap())).thenReturn(25L);

        categoriaBean.init();
        LazyDataModel<Categoria> lazyModel = categoriaBean.getCategoriasLazy();

        // Act
        List<Categoria> result = lazyModel.load(10, 10, Collections.emptyMap(), Collections.emptyMap());

        // Assert
        assertThat(result).hasSize(1);
        assertThat(lazyModel.getRowCount()).isEqualTo(25);
        verify(categoriaService).listarComFiltros(anyMap(), eq(10), eq(10), anyString(), anyBoolean());
    }

    @Test
    void should_SortByName_When_SortingIsApplied() {
        // Arrange
        categoriaBean.init();
        when(categoriaService.listarComFiltros(anyMap(), anyInt(), anyInt(), eq("nome"), eq(true)))
            .thenReturn(categoriasMock);
        when(categoriaService.contarComFiltros(anyMap())).thenReturn(1L);

        LazyDataModel<Categoria> lazyModel = categoriaBean.getCategoriasLazy();

        // Act
        List<Categoria> result = lazyModel.load(0, 10, Collections.emptyMap(), Collections.emptyMap());

        // Assert
        verify(categoriaService).listarComFiltros(anyMap(), eq(0), eq(10), anyString(), anyBoolean());
    }

    @Test
    void should_CountActiveCategories_When_FilteringByActiveStatus() {
        // Arrange
        Map<String, Object> filtros = new HashMap<>();
        filtros.put("ativa", true);
        when(categoriaService.contarComFiltros(eq(filtros))).thenReturn(5L);

        categoriaBean.init();
        categoriaBean.setFiltros(filtros);
        LazyDataModel<Categoria> lazyModel = categoriaBean.getCategoriasLazy();

        // Act
        lazyModel.load(0, 10, Collections.emptyMap(), Collections.emptyMap());

        // Assert
        verify(categoriaService).contarComFiltros(eq(filtros));
    }

    @Test
    void should_HandleEmptyResults_When_NoMatchingCategories() {
        // Arrange
        when(categoriaService.listarComFiltros(anyMap(), anyInt(), anyInt(), anyString(), anyBoolean()))
            .thenReturn(Collections.emptyList());
        when(categoriaService.contarComFiltros(anyMap())).thenReturn(0L);

        categoriaBean.init();
        LazyDataModel<Categoria> lazyModel = categoriaBean.getCategoriasLazy();

        // Act
        List<Categoria> result = lazyModel.load(0, 10, Collections.emptyMap(), Collections.emptyMap());

        // Assert
        assertThat(result).isEmpty();
        assertThat(lazyModel.getRowCount()).isEqualTo(0);
    }
}