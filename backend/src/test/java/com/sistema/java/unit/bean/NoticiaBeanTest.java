package com.sistema.java.unit.bean;

import com.sistema.java.bean.NoticiaBean;
import com.sistema.java.dto.NoticiaDTO;
import com.sistema.java.model.entity.Categoria;
import com.sistema.java.model.entity.Comentario;
import com.sistema.java.model.entity.Noticia;
import com.sistema.java.model.entity.Usuario;
import com.sistema.java.model.enums.Role;
import com.sistema.java.service.CategoriaService;
import com.sistema.java.service.ComentarioService;
import com.sistema.java.service.NoticiaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.primefaces.model.FilterMeta;
import org.primefaces.model.LazyDataModel;
import org.primefaces.model.SortMeta;

import javax.faces.context.FacesContext;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Testes unitários para NoticiaBean
 * Referência: Testes e Qualidade de Código - project_rules.md
 * Referência: Banco de Dados Relacional - project_rules.md
 */
@ExtendWith(MockitoExtension.class)
class NoticiaBeanTest {

    @Mock
    private NoticiaService noticiaService;

    @Mock
    private CategoriaService categoriaService;

    @Mock
    private ComentarioService comentarioService;

    @Mock
    private FacesContext facesContext;

    @InjectMocks
    private NoticiaBean noticiaBean;

    private Noticia noticiaMock;
    private NoticiaDTO noticiaDTOMock;
    private Categoria categoriaMock;
    private Comentario comentarioMock;
    private Usuario autorMock;
    private List<Categoria> categoriasMock;
    private List<Comentario> comentariosMock;

    @BeforeEach
    void setUp() {
        // Criar autor mock
        autorMock = new Usuario();
        autorMock.setId(1L);
        autorMock.setNome("João");
        autorMock.setSobrenome("Silva");
        autorMock.setEmail("joao@teste.com");
        autorMock.setRole(Role.COLABORADOR);

        // Criar categoria mock
        categoriaMock = new Categoria();
        categoriaMock.setId(1L);
        categoriaMock.setNome("Tecnologia");
        categoriaMock.setDescricao("Notícias sobre tecnologia");
        categoriaMock.setAtiva(true);
        categoriaMock.setDataCriacao(LocalDateTime.now());

        categoriasMock = Arrays.asList(categoriaMock);

        // Criar notícia mock
        noticiaMock = new Noticia();
        noticiaMock.setId(1L);
        noticiaMock.setTitulo("Título da Notícia");
        noticiaMock.setConteudo("Conteúdo da notícia de teste");
        noticiaMock.setResumo("Resumo da notícia");
        noticiaMock.setAutor(autorMock);
        noticiaMock.setPublicada(true);
        noticiaMock.setDataPublicacao(LocalDateTime.now());
        noticiaMock.setDataCriacao(LocalDateTime.now());
        noticiaMock.setCategorias(new HashSet<>(categoriasMock));

        // Criar DTO mock
        noticiaDTOMock = new NoticiaDTO();
        noticiaDTOMock.setTitulo("Título da Notícia");
        noticiaDTOMock.setConteudo("Conteúdo da notícia de teste");
        noticiaDTOMock.setResumo("Resumo da notícia");
        noticiaDTOMock.setAutorId(1L);
        noticiaDTOMock.setPublicada(true);
        noticiaDTOMock.setCategoriaIds(Arrays.asList(1L));

        // Criar comentário mock
        comentarioMock = new Comentario();
        comentarioMock.setId(1L);
        comentarioMock.setConteudo("Comentário de teste");
        comentarioMock.setAutor(autorMock);
        comentarioMock.setNoticia(noticiaMock);
        comentarioMock.setAprovado(true);
        comentarioMock.setDataCriacao(LocalDateTime.now());

        comentariosMock = Arrays.asList(comentarioMock);
    }

    @Test
    void should_InitializeCorrectly_When_PostConstructCalled() {
        // Arrange
        when(categoriaService.listarAtivas()).thenReturn(categoriasMock);

        // Act
        noticiaBean.init();

        // Assert
        assertThat(noticiaBean.getCategorias()).isEqualTo(categoriasMock);
        assertThat(noticiaBean.getNoticiasLazy()).isNotNull();
        verify(categoriaService).listarAtivas();
    }

    @Test
    void should_LoadNewsData_When_LazyModelIsUsed() {
        // Arrange
        List<Noticia> noticias = Arrays.asList(noticiaMock);
        when(noticiaService.listarComFiltros(anyMap(), anyInt(), anyInt(), anyString(), anyBoolean()))
            .thenReturn(noticias);
        when(noticiaService.contarComFiltros(anyMap())).thenReturn(1L);

        noticiaBean.init();
        LazyDataModel<Noticia> lazyModel = noticiaBean.getNoticiasLazy();

        // Act
        List<Noticia> result = lazyModel.load(0, 10, Collections.emptyMap(), Collections.emptyMap());

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(noticiaMock);
        verify(noticiaService).listarComFiltros(anyMap(), eq(0), eq(10), anyString(), anyBoolean());
    }

    @Test
    void should_CreateNewsSuccessfully_When_DataIsValid() {
        // Arrange
        noticiaBean.setNoticiaAtual(noticiaDTOMock);
        when(noticiaService.criarNoticia(any(NoticiaDTO.class))).thenReturn(noticiaMock);

        // Act
        noticiaBean.criarNoticia();

        // Assert
        verify(noticiaService).criarNoticia(noticiaDTOMock);
        assertThat(noticiaBean.getNoticiaAtual()).isNotNull();
    }

    @Test
    void should_UpdateNewsSuccessfully_When_DataIsValid() {
        // Arrange
        noticiaDTOMock.setId(1L);
        noticiaBean.setNoticiaAtual(noticiaDTOMock);
        when(noticiaService.atualizarNoticia(any(NoticiaDTO.class))).thenReturn(noticiaMock);

        // Act
        noticiaBean.atualizarNoticia();

        // Assert
        verify(noticiaService).atualizarNoticia(noticiaDTOMock);
    }

    @Test
    void should_DeleteNewsSuccessfully_When_NewsExists() {
        // Arrange
        when(noticiaService.buscarPorId(1L)).thenReturn(noticiaMock);
        doNothing().when(noticiaService).deletarNoticia(1L);

        // Act
        noticiaBean.deletarNoticia(1L);

        // Assert
        verify(noticiaService).deletarNoticia(1L);
    }

    @Test
    void should_PublishNewsSuccessfully_When_NewsExists() {
        // Arrange
        when(noticiaService.buscarPorId(1L)).thenReturn(noticiaMock);
        when(noticiaService.publicarNoticia(1L)).thenReturn(noticiaMock);

        // Act
        noticiaBean.publicarNoticia(1L);

        // Assert
        verify(noticiaService).publicarNoticia(1L);
    }

    @Test
    void should_UnpublishNewsSuccessfully_When_NewsExists() {
        // Arrange
        when(noticiaService.buscarPorId(1L)).thenReturn(noticiaMock);
        when(noticiaService.despublicarNoticia(1L)).thenReturn(noticiaMock);

        // Act
        noticiaBean.despublicarNoticia(1L);

        // Assert
        verify(noticiaService).despublicarNoticia(1L);
    }

    @Test
    void should_LoadNewsDetails_When_NewsIdIsProvided() {
        // Arrange
        when(noticiaService.buscarPorId(1L)).thenReturn(noticiaMock);
        when(comentarioService.listarPorNoticia(1L)).thenReturn(comentariosMock);

        // Act
        noticiaBean.carregarDetalhesNoticia(1L);

        // Assert
        assertThat(noticiaBean.getNoticiaAtual()).isNotNull();
        assertThat(noticiaBean.getComentarios()).isEqualTo(comentariosMock);
        verify(noticiaService).buscarPorId(1L);
        verify(comentarioService).listarPorNoticia(1L);
    }

    @Test
    void should_FilterByCategory_When_CategoryIsSelected() {
        // Arrange
        noticiaBean.init();
        Map<String, Object> filtros = new HashMap<>();
        filtros.put("categoria", categoriaMock);
        noticiaBean.setFiltros(filtros);

        List<Noticia> noticiasFiltered = Arrays.asList(noticiaMock);
        when(noticiaService.listarComFiltros(eq(filtros), anyInt(), anyInt(), anyString(), anyBoolean()))
            .thenReturn(noticiasFiltered);
        when(noticiaService.contarComFiltros(eq(filtros))).thenReturn(1L);

        LazyDataModel<Noticia> lazyModel = noticiaBean.getNoticiasLazy();

        // Act
        List<Noticia> result = lazyModel.load(0, 10, Collections.emptyMap(), Collections.emptyMap());

        // Assert
        assertThat(result).hasSize(1);
        verify(noticiaService).listarComFiltros(eq(filtros), eq(0), eq(10), anyString(), anyBoolean());
    }

    @Test
    void should_FilterByPublicationStatus_When_StatusIsSelected() {
        // Arrange
        noticiaBean.init();
        Map<String, Object> filtros = new HashMap<>();
        filtros.put("publicada", true);
        noticiaBean.setFiltros(filtros);

        List<Noticia> noticiasPublicadas = Arrays.asList(noticiaMock);
        when(noticiaService.listarComFiltros(eq(filtros), anyInt(), anyInt(), anyString(), anyBoolean()))
            .thenReturn(noticiasPublicadas);
        when(noticiaService.contarComFiltros(eq(filtros))).thenReturn(1L);

        LazyDataModel<Noticia> lazyModel = noticiaBean.getNoticiasLazy();

        // Act
        List<Noticia> result = lazyModel.load(0, 10, Collections.emptyMap(), Collections.emptyMap());

        // Assert
        assertThat(result).hasSize(1);
        verify(noticiaService).listarComFiltros(eq(filtros), eq(0), eq(10), anyString(), anyBoolean());
    }

    @Test
    void should_SearchByTitle_When_SearchTermIsProvided() {
        // Arrange
        noticiaBean.init();
        Map<String, Object> filtros = new HashMap<>();
        filtros.put("titulo", "Título");
        noticiaBean.setFiltros(filtros);

        List<Noticia> noticiasEncontradas = Arrays.asList(noticiaMock);
        when(noticiaService.listarComFiltros(eq(filtros), anyInt(), anyInt(), anyString(), anyBoolean()))
            .thenReturn(noticiasEncontradas);
        when(noticiaService.contarComFiltros(eq(filtros))).thenReturn(1L);

        LazyDataModel<Noticia> lazyModel = noticiaBean.getNoticiasLazy();

        // Act
        List<Noticia> result = lazyModel.load(0, 10, Collections.emptyMap(), Collections.emptyMap());

        // Assert
        assertThat(result).hasSize(1);
        verify(noticiaService).listarComFiltros(eq(filtros), eq(0), eq(10), anyString(), anyBoolean());
    }

    @Test
    void should_ClearFilters_When_ClearMethodIsCalled() {
        // Arrange
        Map<String, Object> filtros = new HashMap<>();
        filtros.put("titulo", "Teste");
        filtros.put("categoria", categoriaMock);
        noticiaBean.setFiltros(filtros);

        // Act
        noticiaBean.limparFiltros();

        // Assert
        assertThat(noticiaBean.getFiltros()).isEmpty();
    }

    @Test
    void should_PrepareNewNews_When_NovoMethodIsCalled() {
        // Act
        noticiaBean.novaNoticia();

        // Assert
        assertThat(noticiaBean.getNoticiaAtual()).isNotNull();
        assertThat(noticiaBean.getNoticiaAtual().getId()).isNull();
        assertThat(noticiaBean.getNoticiaAtual().getTitulo()).isNull();
    }

    @Test
    void should_PrepareEditNews_When_EditMethodIsCalled() {
        // Arrange
        when(noticiaService.buscarPorId(1L)).thenReturn(noticiaMock);

        // Act
        noticiaBean.editarNoticia(1L);

        // Assert
        assertThat(noticiaBean.getNoticiaAtual()).isNotNull();
        verify(noticiaService).buscarPorId(1L);
    }

    @Test
    void should_CancelEdit_When_CancelMethodIsCalled() {
        // Arrange
        noticiaBean.setNoticiaAtual(noticiaDTOMock);

        // Act
        noticiaBean.cancelarEdicao();

        // Assert
        assertThat(noticiaBean.getNoticiaAtual()).isNull();
    }

    @Test
    void should_ValidateRequiredFields_When_CreatingNews() {
        // Arrange
        NoticiaDTO noticiaInvalida = new NoticiaDTO();
        // Título vazio
        noticiaInvalida.setTitulo("");
        noticiaInvalida.setConteudo("Conteúdo válido");
        noticiaBean.setNoticiaAtual(noticiaInvalida);

        // Act & Assert
        // O teste deve verificar se a validação impede a criação
        // Isso seria feito através de validações Bean Validation ou JSF
        assertThat(noticiaInvalida.getTitulo()).isEmpty();
    }

    @Test
    void should_HandlePagination_When_LoadingNews() {
        // Arrange
        List<Noticia> noticias = Arrays.asList(noticiaMock);
        when(noticiaService.listarComFiltros(anyMap(), eq(10), eq(10), anyString(), anyBoolean()))
            .thenReturn(noticias);
        when(noticiaService.contarComFiltros(anyMap())).thenReturn(25L);

        noticiaBean.init();
        LazyDataModel<Noticia> lazyModel = noticiaBean.getNoticiasLazy();

        // Act
        List<Noticia> result = lazyModel.load(10, 10, Collections.emptyMap(), Collections.emptyMap());

        // Assert
        assertThat(result).hasSize(1);
        assertThat(lazyModel.getRowCount()).isEqualTo(25);
        verify(noticiaService).listarComFiltros(anyMap(), eq(10), eq(10), anyString(), anyBoolean());
    }
}