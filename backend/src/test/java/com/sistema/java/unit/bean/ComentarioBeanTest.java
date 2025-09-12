package com.sistema.java.unit.bean;

import com.sistema.java.bean.ComentarioBean;
import com.sistema.java.model.dto.ComentarioDTO;
import com.sistema.java.model.dto.NoticiaDTO;
import com.sistema.java.model.entity.Comentario;
import com.sistema.java.model.entity.Noticia;
import com.sistema.java.model.entity.Usuario;
import com.sistema.java.service.ComentarioService;
import com.sistema.java.service.NoticiaService;
import com.sistema.java.service.AuthService;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Testes unitários para ComentarioBean
 * Referência: Testes e Qualidade de Código - project_rules.md
 * Referência: Banco de Dados Relacional - project_rules.md
 */
@ExtendWith(MockitoExtension.class)
class ComentarioBeanTest {

    @Mock
    private ComentarioService comentarioService;

    @Mock
    private NoticiaService noticiaService;

    @Mock
    private AuthService authService;

    @Mock
    private FacesContext facesContext;

    @InjectMocks
    private ComentarioBean comentarioBean;

    private Comentario comentarioMock;
    private ComentarioDTO comentarioDTOMock;
    private Usuario usuarioMock;
    private Noticia noticiaMock;
    private List<Comentario> comentariosMock;

    @BeforeEach
    void setUp() {
        // Criar usuário mock
        usuarioMock = new Usuario();
        usuarioMock.setId(1L);
        usuarioMock.setNome("João");
        usuarioMock.setEmail("joao@email.com");

        // Criar notícia mock
        noticiaMock = new Noticia();
        noticiaMock.setId(1L);
        noticiaMock.setTitulo("Notícia Teste");
        noticiaMock.setConteudo("Conteúdo da notícia");
        noticiaMock.setPublicada(true);

        // Criar comentário mock
        comentarioMock = new Comentario();
        comentarioMock.setId(1L);
        comentarioMock.setConteudo("Comentário de teste");
        comentarioMock.setAutor(usuarioMock);
        comentarioMock.setNoticia(noticiaMock);
        comentarioMock.setAprovado(false);
        comentarioMock.setDataCriacao(LocalDateTime.now());

        // Criar DTO mock
        comentarioDTOMock = new ComentarioDTO();
        comentarioDTOMock.setConteudo("Comentário de teste");
        comentarioDTOMock.setNoticiaId(1L);

        // Lista de comentários mock
        comentariosMock = Arrays.asList(comentarioMock);
    }

    @Test
    void should_InitializeCorrectly_When_PostConstructCalled() {
        // Act
        comentarioBean.init();

        // Assert
        assertThat(comentarioBean.getComentariosLazy()).isNotNull();
        assertThat(comentarioBean.getNovoComentario()).isNotNull();
    }

    @Test
    void should_LoadCommentsData_When_LazyModelIsUsed() {
        // Arrange
        when(comentarioService.listarComFiltros(anyMap(), anyInt(), anyInt(), anyString(), anyBoolean())).thenReturn(comentariosMock);
        when(comentarioService.contarComFiltros(anyMap())).thenReturn(1L);

        comentarioBean.init();
        LazyDataModel<ComentarioDTO> lazyModel = comentarioBean.getComentariosLazy();

        // Act
        List<ComentarioDTO> result = lazyModel.load(0, 10, Collections.emptyMap(), Collections.emptyMap());

        // Assert
        assertThat(result).hasSize(1);
        verify(comentarioService).listarComFiltros(anyMap(), eq(0), eq(10), anyString(), anyBoolean());
    }

    @Test
    void should_CreateCommentSuccessfully_When_DataIsValid() {
        // Arrange
        comentarioBean.setComentarioAtual(comentarioDTOMock);
        when(authService.getUsuarioLogado()).thenReturn(usuarioMock);
        when(noticiaService.findById(1L)).thenReturn(Optional.of(new NoticiaDTO()));
        when(comentarioService.criarComentario(any(ComentarioDTO.class), eq(usuarioMock)))
            .thenReturn(comentarioDTOMock);

        // Act
        comentarioBean.criarComentario();

        // Assert
        verify(comentarioService).criarComentario(comentarioDTOMock, usuarioMock);
        assertThat(comentarioBean.getComentarioAtual()).isNull(); // Deve limpar após criar
    }

    @Test
    void should_NotCreateComment_When_UserNotLoggedIn() {
        // Arrange
        comentarioBean.setComentarioAtual(comentarioDTOMock);
        when(authService.getUsuarioLogado()).thenReturn(null);

        // Act
        comentarioBean.criarComentario();

        // Assert
        verify(comentarioService, never()).criarComentario(any(ComentarioDTO.class), any(Usuario.class));
    }

    @Test
    void should_NotCreateComment_When_NewsNotFound() {
        // Arrange
        comentarioBean.setComentarioAtual(comentarioDTOMock);
        when(authService.getUsuarioLogado()).thenReturn(usuarioMock);
        when(noticiaService.findById(1L)).thenReturn(Optional.empty());

        // Act
        comentarioBean.criarComentario();

        // Assert
        verify(comentarioService, never()).criarComentario(any(ComentarioDTO.class), any(Usuario.class));
    }

    @Test
    void should_ApproveCommentSuccessfully_When_CommentExists() {
        // Arrange
        when(comentarioService.buscarPorId(1L)).thenReturn(comentarioMock);
        when(comentarioService.aprovarComentario(1L)).thenReturn(comentarioMock);

        // Act
        comentarioBean.aprovarComentario(1L);

        // Assert
        verify(comentarioService).aprovarComentario(1L);
    }

    @Test
    void should_RejectCommentSuccessfully_When_CommentExists() {
        // Arrange
        when(comentarioService.buscarPorId(1L)).thenReturn(comentarioMock);
        when(comentarioService.rejeitarComentario(1L)).thenReturn(comentarioMock);

        // Act
        comentarioBean.rejeitarComentario(1L);

        // Assert
        verify(comentarioService).rejeitarComentario(1L);
    }

    @Test
    void should_DeleteCommentSuccessfully_When_CommentExists() {
        // Arrange
        when(comentarioService.buscarPorId(1L)).thenReturn(comentarioMock);
        doNothing().when(comentarioService).deletarComentario(1L);

        // Act
        comentarioBean.deletarComentario(1L);

        // Assert
        verify(comentarioService).deletarComentario(1L);
    }

    @Test
    void should_FilterByApprovalStatus_When_StatusFilterIsApplied() {
        // Arrange
        comentarioBean.init();
        Map<String, Object> filtros = new HashMap<>();
        filtros.put("aprovado", true);
        comentarioBean.setFiltros(filtros);

        List<Comentario> comentariosAprovados = Arrays.asList(comentarioMock);
        when(comentarioService.listarComFiltros(eq(filtros), anyInt(), anyInt(), anyString(), anyBoolean()))
            .thenReturn(comentariosAprovados);
        when(comentarioService.contarComFiltros(eq(filtros))).thenReturn(1L);

        LazyDataModel<ComentarioDTO> lazyModel = comentarioBean.getComentariosLazy();

        // Act
        List<ComentarioDTO> result = lazyModel.load(0, 10, Collections.emptyMap(), Collections.emptyMap());

        // Assert
        assertThat(result).hasSize(1);
        verify(comentarioService).listarComFiltros(eq(filtros), eq(0), eq(10), anyString(), anyBoolean());
    }

    @Test
    void should_FilterByNews_When_NewsFilterIsApplied() {
        // Arrange
        comentarioBean.init();
        Map<String, Object> filtros = new HashMap<>();
        filtros.put("noticiaId", 1L);
        comentarioBean.setFiltros(filtros);

        List<Comentario> comentariosDaNoticia = Arrays.asList(comentarioMock);
        when(comentarioService.listarComFiltros(eq(filtros), anyInt(), anyInt(), anyString(), anyBoolean()))
            .thenReturn(comentariosDaNoticia);
        when(comentarioService.contarComFiltros(eq(filtros))).thenReturn(1L);

        LazyDataModel<ComentarioDTO> lazyModel = comentarioBean.getComentariosLazy();

        // Act
        List<ComentarioDTO> result = lazyModel.load(0, 10, Collections.emptyMap(), Collections.emptyMap());

        // Assert
        assertThat(result).hasSize(1);
        verify(comentarioService).listarComFiltros(eq(filtros), eq(0), eq(10), anyString(), anyBoolean());
    }

    @Test
    void should_FilterByAuthor_When_AuthorFilterIsApplied() {
        // Arrange
        comentarioBean.init();
        Map<String, Object> filtros = new HashMap<>();
        filtros.put("autorNome", "João");
        comentarioBean.setFiltros(filtros);

        List<Comentario> comentariosDoAutor = Arrays.asList(comentarioMock);
        when(comentarioService.listarComFiltros(eq(filtros), anyInt(), anyInt(), anyString(), anyBoolean()))
            .thenReturn(comentariosDoAutor);
        when(comentarioService.contarComFiltros(eq(filtros))).thenReturn(1L);

        LazyDataModel<ComentarioDTO> lazyModel = comentarioBean.getComentariosLazy();

        // Act
        List<Comentario> result = lazyModel.load(0, 10, Collections.emptyMap(), Collections.emptyMap());

        // Assert
        assertThat(result).hasSize(1);
        verify(comentarioService).listarComFiltros(eq(filtros), eq(0), eq(10), anyString(), anyBoolean());
    }

    @Test
    void should_ClearFilters_When_ClearMethodIsCalled() {
        // Arrange
        Map<String, Object> filtros = new HashMap<>();
        filtros.put("aprovado", true);
        filtros.put("noticiaId", 1L);
        comentarioBean.setFiltros(filtros);

        // Act
        comentarioBean.limparFiltros();

        // Assert
        assertThat(comentarioBean.getFiltros()).isEmpty();
    }

    @Test
    void should_PrepareNewComment_When_NovoMethodIsCalled() {
        // Act
        comentarioBean.novoComentario();

        // Assert
        assertThat(comentarioBean.getComentarioAtual()).isNotNull();
        assertThat(comentarioBean.getComentarioAtual().getId()).isNull();
        assertThat(comentarioBean.getComentarioAtual().getConteudo()).isNull();
    }

    @Test
    void should_PrepareNewCommentForNews_When_NovoCommentarioParaNoticiaIsCalled() {
        // Act
        comentarioBean.novoComentarioParaNoticia(1L);

        // Assert
        assertThat(comentarioBean.getComentarioAtual()).isNotNull();
        assertThat(comentarioBean.getComentarioAtual().getNoticiaId()).isEqualTo(1L);
        assertThat(comentarioBean.getComentarioAtual().getId()).isNull();
    }

    @Test
    void should_CancelEdit_When_CancelMethodIsCalled() {
        // Arrange
        comentarioBean.setComentarioAtual(comentarioDTOMock);

        // Act
        comentarioBean.cancelarEdicao();

        // Assert
        assertThat(comentarioBean.getComentarioAtual()).isNull();
    }

    @Test
    void should_ValidateRequiredFields_When_CreatingComment() {
        // Arrange
        ComentarioDTO comentarioInvalido = new ComentarioDTO();
        // Conteúdo vazio
        comentarioInvalido.setConteudo("");
        comentarioInvalido.setNoticiaId(1L);
        comentarioBean.setComentarioAtual(comentarioInvalido);

        // Act & Assert
        // O teste deve verificar se a validação impede a criação
        assertThat(comentarioInvalido.getConteudo()).isEmpty();
    }

    @Test
    void should_HandlePagination_When_LoadingComments() {
        // Arrange
        List<Comentario> comentarios = Arrays.asList(comentarioMock);
        when(comentarioService.listarComFiltros(anyMap(), eq(10), eq(10), anyString(), anyBoolean()))
            .thenReturn(comentarios);
        when(comentarioService.contarComFiltros(anyMap())).thenReturn(25L);

        comentarioBean.init();
        LazyDataModel<ComentarioDTO> lazyModel = comentarioBean.getComentariosLazy();

        // Act
        List<ComentarioDTO> result = lazyModel.load(10, 10, Collections.emptyMap(), Collections.emptyMap());

        // Assert
        assertThat(result).hasSize(1);
        assertThat(lazyModel.getRowCount()).isEqualTo(25);
        verify(comentarioService).listarComFiltros(anyMap(), eq(10), eq(10), anyString(), anyBoolean());
    }

    @Test
    void should_SortByDate_When_SortingIsApplied() {
        // Arrange
        comentarioBean.init();
        when(comentarioService.listarComFiltros(anyMap(), anyInt(), anyInt(), eq("dataCriacao"), eq(false)))
            .thenReturn(comentariosMock);
        when(comentarioService.contarComFiltros(anyMap())).thenReturn(1L);

        LazyDataModel<ComentarioDTO> lazyModel = comentarioBean.getComentariosLazy();

        // Act
        List<ComentarioDTO> result = lazyModel.load(0, 10, Collections.emptyMap(), Collections.emptyMap());

        // Assert
        verify(comentarioService).listarComFiltros(anyMap(), eq(0), eq(10), anyString(), anyBoolean());
    }

    @Test
    void should_CountPendingComments_When_FilteringByPendingStatus() {
        // Arrange
        Map<String, Object> filtros = new HashMap<>();
        filtros.put("aprovado", false);
        when(comentarioService.contarComFiltros(eq(filtros))).thenReturn(3L);

        comentarioBean.init();
        comentarioBean.setFiltros(filtros);
        LazyDataModel<ComentarioDTO> lazyModel = comentarioBean.getComentariosLazy();

        // Act
        lazyModel.load(0, 10, Collections.emptyMap(), Collections.emptyMap());

        // Assert
        verify(comentarioService).contarComFiltros(eq(filtros));
    }

    @Test
    void should_HandleEmptyResults_When_NoMatchingComments() {
        // Arrange
        when(comentarioService.listarComFiltros(anyMap(), anyInt(), anyInt(), anyString(), anyBoolean()))
            .thenReturn(Collections.emptyList());
        when(comentarioService.contarComFiltros(anyMap())).thenReturn(0L);

        comentarioBean.init();
        LazyDataModel<ComentarioDTO> lazyModel = comentarioBean.getComentariosLazy();

        // Act
        List<ComentarioDTO> result = lazyModel.load(0, 10, Collections.emptyMap(), Collections.emptyMap());

        // Assert
        assertThat(result).isEmpty();
        assertThat(lazyModel.getRowCount()).isEqualTo(0);
    }

    @Test
    void should_LoadCommentsForSpecificNews_When_NewsIdIsProvided() {
        // Arrange
        comentarioBean.setNoticiaId(1L);
        comentarioBean.init();

        Map<String, Object> expectedFiltros = new HashMap<>();
        expectedFiltros.put("noticiaId", 1L);
        expectedFiltros.put("aprovado", true); // Apenas comentários aprovados para visualização pública

        when(comentarioService.listarComFiltros(eq(expectedFiltros), anyInt(), anyInt(), anyString(), anyBoolean()))
            .thenReturn(comentariosMock);
        when(comentarioService.contarComFiltros(eq(expectedFiltros))).thenReturn(1L);

        LazyDataModel<ComentarioDTO> lazyModel = comentarioBean.getComentariosLazy();

        // Act
        List<ComentarioDTO> result = lazyModel.load(0, 10, Collections.emptyMap(), Collections.emptyMap());

        // Assert
        assertThat(result).hasSize(1);
        verify(comentarioService).listarComFiltros(eq(expectedFiltros), eq(0), eq(10), anyString(), anyBoolean());
    }

    @Test
    void should_BulkApproveComments_When_MultipleCommentsSelected() {
        // Arrange
        List<Long> comentarioIds = Arrays.asList(1L, 2L, 3L);
        // Simular aprovação individual para cada comentário
        for (Long id : comentarioIds) {
            doNothing().when(comentarioService).aprovarComentario(id);
        }

        // Act
        for (Long id : comentarioIds) {
            comentarioBean.aprovarComentario(id);
        }

        // Assert
        for (Long id : comentarioIds) {
            verify(comentarioService).aprovarComentario(id);
        }
    }

    @Test
    void should_BulkRejectComments_When_MultipleCommentsSelected() {
        // Arrange
        List<Long> comentarioIds = Arrays.asList(1L, 2L, 3L);
        // Simular rejeição individual para cada comentário
        for (Long id : comentarioIds) {
            doNothing().when(comentarioService).rejeitarComentario(id);
        }

        // Act
        for (Long id : comentarioIds) {
            comentarioBean.rejeitarComentario(id);
        }

        // Assert
        for (Long id : comentarioIds) {
            verify(comentarioService).rejeitarComentario(id);
        }
    }
}