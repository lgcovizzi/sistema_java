package com.sistema.java.unit.bean;

import com.sistema.java.bean.DashboardBean;
import com.sistema.java.model.entity.Usuario;
import com.sistema.java.model.enums.Papel;
import com.sistema.java.service.AuthService;
import com.sistema.java.service.NoticiaService;
import com.sistema.java.service.ComentarioService;
import com.sistema.java.service.UsuarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Testes unitários para DashboardBean
 * Referência: Testes e Qualidade de Código - project_rules.md
 * Referência: Controle de Acesso - project_rules.md
 */
@ExtendWith(MockitoExtension.class)
class DashboardBeanTest {

    @Mock
    private AuthService authService;

    @Mock
    private NoticiaService noticiaService;

    @Mock
    private ComentarioService comentarioService;

    @Mock
    private UsuarioService usuarioService;

    @Mock
    private FacesContext facesContext;

    @InjectMocks
    private DashboardBean dashboardBean;

    private Usuario usuarioMock;
    private Usuario adminMock;
    private Usuario colaboradorMock;

    @BeforeEach
    void setUp() {
        // Criar usuário comum mock
        usuarioMock = new Usuario();
        usuarioMock.setId(1L);
        usuarioMock.setNome("João");
        usuarioMock.setSobrenome("Silva");
        usuarioMock.setEmail("joao@email.com");
        usuarioMock.setPapel(Papel.USUARIO);
        usuarioMock.setAtivo(true);
        usuarioMock.setDataCriacao(LocalDateTime.now());

        // Criar administrador mock
        adminMock = new Usuario();
        adminMock.setId(2L);
        adminMock.setNome("Admin");
        adminMock.setSobrenome("Sistema");
        adminMock.setEmail("admin@email.com");
        adminMock.setPapel(Papel.ADMINISTRADOR);
        adminMock.setAtivo(true);
        adminMock.setDataCriacao(LocalDateTime.now());

        // Criar colaborador mock
        colaboradorMock = new Usuario();
        colaboradorMock.setId(3L);
        colaboradorMock.setNome("Maria");
        colaboradorMock.setSobrenome("Santos");
        colaboradorMock.setEmail("maria@email.com");
        colaboradorMock.setPapel(Papel.COLABORADOR);
        colaboradorMock.setAtivo(true);
        colaboradorMock.setDataCriacao(LocalDateTime.now());
    }

    @Test
    void should_InitializeCorrectly_When_PostConstructCalled() {
        // Arrange
        when(authService.getUsuarioLogado()).thenReturn(usuarioMock);
        when(authService.isLogado()).thenReturn(true);

        // Act
        dashboardBean.init();

        // Assert
        assertThat(dashboardBean.getUsuarioLogado()).isEqualTo(usuarioMock);
        verify(authService).getUsuarioLogado();
    }

    @Test
    void should_LoadUserData_When_UserIsLoggedIn() {
        // Arrange
        when(authService.getUsuarioLogado()).thenReturn(usuarioMock);
        when(authService.isLogado()).thenReturn(true);

        // Act
        dashboardBean.init();
        dashboardBean.carregarDadosUsuario();

        // Assert
        assertThat(dashboardBean.getUsuarioLogado()).isEqualTo(usuarioMock);
        assertThat(dashboardBean.getNomeCompleto()).isEqualTo("João Silva");
        assertThat(dashboardBean.getPapelUsuario()).isEqualTo("USUARIO");
    }

    @Test
    void should_LoadStatisticsForAdmin_When_UserIsAdmin() {
        // Arrange
        when(authService.getUsuarioLogado()).thenReturn(adminMock);
        when(authService.isLogado()).thenReturn(true);
        when(authService.hasRole("ADMINISTRADOR")).thenReturn(true);
        when(noticiaService.contarTotalNoticias()).thenReturn(50L);
        when(noticiaService.contarNoticiasPublicadas()).thenReturn(45L);
        when(comentarioService.contarTotalComentarios()).thenReturn(200L);
        when(comentarioService.contarComentariosPendentes()).thenReturn(15L);
        when(usuarioService.contarTotalUsuarios()).thenReturn(100L);
        when(usuarioService.contarUsuariosAtivos()).thenReturn(95L);

        // Act
        dashboardBean.init();
        dashboardBean.carregarEstatisticas();

        // Assert
        assertThat(dashboardBean.getTotalNoticias()).isEqualTo(50L);
        assertThat(dashboardBean.getNoticiasPublicadas()).isEqualTo(45L);
        assertThat(dashboardBean.getTotalComentarios()).isEqualTo(200L);
        assertThat(dashboardBean.getComentariosPendentes()).isEqualTo(15L);
        assertThat(dashboardBean.getTotalUsuarios()).isEqualTo(100L);
        assertThat(dashboardBean.getUsuariosAtivos()).isEqualTo(95L);
    }

    @Test
    void should_LoadLimitedStatisticsForCollaborator_When_UserIsCollaborator() {
        // Arrange
        when(authService.getUsuarioLogado()).thenReturn(colaboradorMock);
        when(authService.isLogado()).thenReturn(true);
        when(authService.hasRole("COLABORADOR")).thenReturn(true);
        when(authService.hasRole("ADMINISTRADOR")).thenReturn(false);
        when(noticiaService.contarNoticiasPorAutor(3L)).thenReturn(10L);
        when(comentarioService.contarComentariosPendentes()).thenReturn(5L);

        // Act
        dashboardBean.init();
        dashboardBean.carregarEstatisticas();

        // Assert
        assertThat(dashboardBean.getMinhasNoticias()).isEqualTo(10L);
        assertThat(dashboardBean.getComentariosPendentes()).isEqualTo(5L);
        // Estatísticas de admin não devem estar disponíveis
        assertThat(dashboardBean.getTotalUsuarios()).isNull();
    }

    @Test
    void should_LoadBasicDataForUser_When_UserIsRegularUser() {
        // Arrange
        when(authService.getUsuarioLogado()).thenReturn(usuarioMock);
        when(authService.isLogado()).thenReturn(true);
        when(authService.hasRole("ADMINISTRADOR")).thenReturn(false);
        when(authService.hasRole("COLABORADOR")).thenReturn(false);
        when(comentarioService.contarComentariosPorAutor(1L)).thenReturn(3L);

        // Act
        dashboardBean.init();
        dashboardBean.carregarEstatisticas();

        // Assert
        assertThat(dashboardBean.getMeusComentarios()).isEqualTo(3L);
        // Estatísticas administrativas não devem estar disponíveis
        assertThat(dashboardBean.getTotalNoticias()).isNull();
        assertThat(dashboardBean.getTotalUsuarios()).isNull();
    }

    @Test
    void should_LoadRecentNews_When_InitializingDashboard() {
        // Arrange
        when(authService.getUsuarioLogado()).thenReturn(usuarioMock);
        when(authService.isLogado()).thenReturn(true);
        when(noticiaService.listarRecentes(5)).thenReturn(Collections.emptyList());

        // Act
        dashboardBean.init();
        dashboardBean.carregarNoticiasRecentes();

        // Assert
        verify(noticiaService).listarRecentes(5);
        assertThat(dashboardBean.getNoticiasRecentes()).isNotNull();
    }

    @Test
    void should_LoadRecentComments_When_InitializingDashboard() {
        // Arrange
        when(authService.getUsuarioLogado()).thenReturn(adminMock);
        when(authService.isLogado()).thenReturn(true);
        when(authService.hasRole("ADMINISTRADOR")).thenReturn(true);
        when(comentarioService.listarRecentes(5)).thenReturn(Collections.emptyList());

        // Act
        dashboardBean.init();
        dashboardBean.carregarComentariosRecentes();

        // Assert
        verify(comentarioService).listarRecentes(5);
        assertThat(dashboardBean.getComentariosRecentes()).isNotNull();
    }

    @Test
    void should_CheckAdminAccess_When_UserIsAdmin() {
        // Arrange
        when(authService.getUsuarioLogado()).thenReturn(adminMock);
        when(authService.hasRole("ADMINISTRADOR")).thenReturn(true);

        // Act
        dashboardBean.init();
        boolean isAdmin = dashboardBean.isAdmin();

        // Assert
        assertThat(isAdmin).isTrue();
    }

    @Test
    void should_CheckCollaboratorAccess_When_UserIsCollaborator() {
        // Arrange
        when(authService.getUsuarioLogado()).thenReturn(colaboradorMock);
        when(authService.hasRole("COLABORADOR")).thenReturn(true);

        // Act
        dashboardBean.init();
        boolean isCollaborator = dashboardBean.isColaborador();

        // Assert
        assertThat(isCollaborator).isTrue();
    }

    @Test
    void should_DenyAdminAccess_When_UserIsNotAdmin() {
        // Arrange
        when(authService.getUsuarioLogado()).thenReturn(usuarioMock);
        when(authService.hasRole("ADMINISTRADOR")).thenReturn(false);

        // Act
        dashboardBean.init();
        boolean isAdmin = dashboardBean.isAdmin();

        // Assert
        assertThat(isAdmin).isFalse();
    }

    @Test
    void should_RefreshData_When_RefreshMethodIsCalled() {
        // Arrange
        when(authService.getUsuarioLogado()).thenReturn(adminMock);
        when(authService.isLogado()).thenReturn(true);
        when(authService.hasRole("ADMINISTRADOR")).thenReturn(true);
        when(noticiaService.contarTotalNoticias()).thenReturn(55L);
        when(usuarioService.contarTotalUsuarios()).thenReturn(105L);

        // Act
        dashboardBean.init();
        dashboardBean.atualizarDados();

        // Assert
        verify(noticiaService, atLeast(1)).contarTotalNoticias();
        verify(usuarioService, atLeast(1)).contarTotalUsuarios();
    }

    @Test
    void should_HandleNullUser_When_UserNotLoggedIn() {
        // Arrange
        when(authService.getUsuarioLogado()).thenReturn(null);
        when(authService.isLogado()).thenReturn(false);

        // Act
        dashboardBean.init();

        // Assert
        assertThat(dashboardBean.getUsuarioLogado()).isNull();
        assertThat(dashboardBean.getNomeCompleto()).isNull();
    }

    @Test
    void should_ShowWelcomeMessage_When_UserLogsIn() {
        // Arrange
        when(authService.getUsuarioLogado()).thenReturn(usuarioMock);
        when(authService.isLogado()).thenReturn(true);

        // Act
        dashboardBean.init();
        String welcomeMessage = dashboardBean.getMensagemBoasVindas();

        // Assert
        assertThat(welcomeMessage).contains("João");
        assertThat(welcomeMessage).isNotEmpty();
    }

    @Test
    void should_ShowDifferentWelcomeMessage_When_UserIsAdmin() {
        // Arrange
        when(authService.getUsuarioLogado()).thenReturn(adminMock);
        when(authService.isLogado()).thenReturn(true);
        when(authService.hasRole("ADMINISTRADOR")).thenReturn(true);

        // Act
        dashboardBean.init();
        String welcomeMessage = dashboardBean.getMensagemBoasVindas();

        // Assert
        assertThat(welcomeMessage).contains("Admin");
        assertThat(welcomeMessage).contains("administrador");
    }

    @Test
    void should_CalculatePercentages_When_LoadingStatistics() {
        // Arrange
        when(authService.getUsuarioLogado()).thenReturn(adminMock);
        when(authService.isLogado()).thenReturn(true);
        when(authService.hasRole("ADMINISTRADOR")).thenReturn(true);
        when(noticiaService.contarTotalNoticias()).thenReturn(100L);
        when(noticiaService.contarNoticiasPublicadas()).thenReturn(80L);
        when(usuarioService.contarTotalUsuarios()).thenReturn(200L);
        when(usuarioService.contarUsuariosAtivos()).thenReturn(180L);

        // Act
        dashboardBean.init();
        dashboardBean.carregarEstatisticas();

        // Assert
        assertThat(dashboardBean.getPercentualNoticiasPublicadas()).isEqualTo(80.0);
        assertThat(dashboardBean.getPercentualUsuariosAtivos()).isEqualTo(90.0);
    }

    @Test
    void should_HandleZeroDivision_When_CalculatingPercentages() {
        // Arrange
        when(authService.getUsuarioLogado()).thenReturn(adminMock);
        when(authService.isLogado()).thenReturn(true);
        when(authService.hasRole("ADMINISTRADOR")).thenReturn(true);
        when(noticiaService.contarTotalNoticias()).thenReturn(0L);
        when(noticiaService.contarNoticiasPublicadas()).thenReturn(0L);

        // Act
        dashboardBean.init();
        dashboardBean.carregarEstatisticas();

        // Assert
        assertThat(dashboardBean.getPercentualNoticiasPublicadas()).isEqualTo(0.0);
    }

    @Test
    void should_LoadQuickActions_When_UserHasPermissions() {
        // Arrange
        when(authService.getUsuarioLogado()).thenReturn(colaboradorMock);
        when(authService.isLogado()).thenReturn(true);
        when(authService.hasRole("COLABORADOR")).thenReturn(true);

        // Act
        dashboardBean.init();
        List<String> quickActions = dashboardBean.getAcoesRapidas();

        // Assert
        assertThat(quickActions).isNotNull();
        assertThat(quickActions).contains("Criar Notícia");
        assertThat(quickActions).contains("Moderar Comentários");
    }

    @Test
    void should_LoadLimitedQuickActions_When_UserIsRegularUser() {
        // Arrange
        when(authService.getUsuarioLogado()).thenReturn(usuarioMock);
        when(authService.isLogado()).thenReturn(true);
        when(authService.hasRole("ADMINISTRADOR")).thenReturn(false);
        when(authService.hasRole("COLABORADOR")).thenReturn(false);

        // Act
        dashboardBean.init();
        List<String> quickActions = dashboardBean.getAcoesRapidas();

        // Assert
        assertThat(quickActions).isNotNull();
        assertThat(quickActions).contains("Ver Perfil");
        assertThat(quickActions).doesNotContain("Criar Notícia");
        assertThat(quickActions).doesNotContain("Gerenciar Usuários");
    }

    @Test
    void should_ShowSystemAlerts_When_ThereArePendingItems() {
        // Arrange
        when(authService.getUsuarioLogado()).thenReturn(adminMock);
        when(authService.isLogado()).thenReturn(true);
        when(authService.hasRole("ADMINISTRADOR")).thenReturn(true);
        when(comentarioService.contarComentariosPendentes()).thenReturn(10L);
        when(usuarioService.contarUsuariosInativos()).thenReturn(5L);

        // Act
        dashboardBean.init();
        dashboardBean.carregarAlertas();

        // Assert
        assertThat(dashboardBean.getAlertas()).isNotEmpty();
        assertThat(dashboardBean.hasAlertas()).isTrue();
    }

    @Test
    void should_NotShowSystemAlerts_When_NoPendingItems() {
        // Arrange
        when(authService.getUsuarioLogado()).thenReturn(adminMock);
        when(authService.isLogado()).thenReturn(true);
        when(authService.hasRole("ADMINISTRADOR")).thenReturn(true);
        when(comentarioService.contarComentariosPendentes()).thenReturn(0L);
        when(usuarioService.contarUsuariosInativos()).thenReturn(0L);

        // Act
        dashboardBean.init();
        dashboardBean.carregarAlertas();

        // Assert
        assertThat(dashboardBean.hasAlertas()).isFalse();
    }

    @Test
    void should_AddSuccessMessage_When_OperationSucceeds() {
        // Act
        dashboardBean.adicionarMensagem(FacesMessage.SEVERITY_INFO, "Sucesso", "Operação realizada com sucesso");

        // Assert
        // Verificar se a mensagem foi adicionada (mock do FacesContext seria necessário)
        // Este teste verifica se o método não lança exceção
    }

    @Test
    void should_AddErrorMessage_When_OperationFails() {
        // Act
        dashboardBean.adicionarMensagem(FacesMessage.SEVERITY_ERROR, "Erro", "Operação falhou");

        // Assert
        // Verificar se a mensagem de erro foi adicionada
        // Este teste verifica se o método não lança exceção
    }
}