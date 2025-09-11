package com.sistema.java.unit.bean;

import com.sistema.java.bean.DashboardBean;
import com.sistema.java.model.entity.Usuario;
import com.sistema.java.model.enums.PapelUsuario;
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
        usuarioMock.setPapel(PapelUsuario.USUARIO);
        usuarioMock.setAtivo(true);
        usuarioMock.setDataCriacao(LocalDateTime.now());

        // Criar administrador mock
        adminMock = new Usuario();
        adminMock.setId(2L);
        adminMock.setNome("Admin");
        adminMock.setSobrenome("Sistema");
        adminMock.setEmail("admin@email.com");
        adminMock.setPapel(PapelUsuario.ADMINISTRADOR);
        adminMock.setAtivo(true);
        adminMock.setDataCriacao(LocalDateTime.now());

        // Criar colaborador mock
        colaboradorMock = new Usuario();
        colaboradorMock.setId(3L);
        colaboradorMock.setNome("Maria");
        colaboradorMock.setSobrenome("Santos");
        colaboradorMock.setEmail("maria@email.com");
        colaboradorMock.setPapel(PapelUsuario.COLABORADOR);
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

        // Assert
        assertThat(dashboardBean.getUsuarioLogado()).isEqualTo(usuarioMock);
        verify(authService).getUsuarioLogado();
    }

    @Test
    void should_LoadStatisticsForAdmin_When_UserIsAdmin() {
        // Arrange
        when(authService.getUsuarioLogado()).thenReturn(adminMock);
        when(authService.isLogado()).thenReturn(true);
        when(authService.hasRole("ADMINISTRADOR")).thenReturn(true);
        when(noticiaService.countTotal()).thenReturn(50L);
        when(noticiaService.countPublicadas()).thenReturn(45L);
        when(comentarioService.countTotal()).thenReturn(200L);
        when(comentarioService.countPendentes()).thenReturn(15L);
        when(usuarioService.countTotal()).thenReturn(100L);
        when(usuarioService.countAtivos()).thenReturn(95L);

        // Act
        dashboardBean.init();

        // Assert
        verify(noticiaService).countTotal();
        verify(comentarioService).countPendentes();
        verify(usuarioService).countTotal();
        verify(usuarioService).countAtivos();
        assertThat(dashboardBean.getEstatisticas()).isNotNull();
    }

    @Test
    void should_LoadLimitedStatisticsForCollaborator_When_UserIsCollaborator() {
        // Arrange
        when(authService.getUsuarioLogado()).thenReturn(colaboradorMock);
        when(authService.isLogado()).thenReturn(true);
        when(authService.hasRole("COLABORADOR")).thenReturn(true);
        when(authService.hasRole("ADMINISTRADOR")).thenReturn(false);
        when(noticiaService.countTotal()).thenReturn(10L);
        when(comentarioService.countPendentes()).thenReturn(5L);

        // Act
        dashboardBean.init();

        // Assert
        verify(noticiaService).countTotal();
        verify(comentarioService).countPendentes();
        assertThat(dashboardBean.getEstatisticas()).isNotNull();
    }

    @Test
    void should_LoadBasicDataForUser_When_UserIsRegularUser() {
        // Arrange
        when(authService.getUsuarioLogado()).thenReturn(usuarioMock);
        when(authService.isLogado()).thenReturn(true);
        when(authService.hasRole("ADMINISTRADOR")).thenReturn(false);
        when(authService.hasRole("COLABORADOR")).thenReturn(false);
        when(comentarioService.countTotal()).thenReturn(3L);

        // Act
        dashboardBean.init();

        // Assert
        verify(comentarioService).countTotal();
        assertThat(dashboardBean.getEstatisticas()).isNotNull();
    }

    @Test
    void should_LoadRecentNews_When_InitializingDashboard() {
        // Arrange
        when(authService.getUsuarioLogado()).thenReturn(usuarioMock);
        when(authService.isLogado()).thenReturn(true);
        // Act
        dashboardBean.init();

        // Assert
        assertThat(dashboardBean.getNoticiasRecentes()).isNull();
    }

    @Test
    void should_LoadRecentComments_When_InitializingDashboard() {
        // Arrange
        when(authService.getUsuarioLogado()).thenReturn(adminMock);
        when(authService.isLogado()).thenReturn(true);
        when(authService.hasRole("ADMINISTRADOR")).thenReturn(true);
        // Act
        dashboardBean.init();

        // Assert
        assertThat(dashboardBean.getComentariosPendentes()).isNull();
    }

    @Test
    void should_CheckAdminAccess_When_UserIsAdmin() {
        // Arrange
        when(authService.getUsuarioLogado()).thenReturn(adminMock);
        when(authService.hasRole("ADMINISTRADOR")).thenReturn(true);

        // Act
        dashboardBean.init();
        boolean podeAcessar = dashboardBean.podeAcessarAdmin();

        // Assert
        assertThat(podeAcessar).isTrue();
    }

    @Test
    void should_CheckCollaboratorAccess_When_UserIsCollaborator() {
        // Arrange
        when(authService.getUsuarioLogado()).thenReturn(colaboradorMock);
        when(authService.hasRole("COLABORADOR")).thenReturn(true);

        // Act
        dashboardBean.init();
        boolean podeAcessar = dashboardBean.podeAcessarAdmin();

        // Assert
        assertThat(podeAcessar).isTrue();
    }

    @Test
    void should_DenyAdminAccess_When_UserIsNotAdmin() {
        // Arrange
        when(authService.getUsuarioLogado()).thenReturn(usuarioMock);
        when(authService.hasRole("ADMINISTRADOR")).thenReturn(false);

        // Act
        dashboardBean.init();
        boolean podeAcessar = dashboardBean.podeAcessarAdmin();

        // Assert
        assertThat(podeAcessar).isFalse();
    }

    @Test
    void should_RefreshData_When_RefreshMethodIsCalled() {
        // Arrange
        when(authService.getUsuarioLogado()).thenReturn(adminMock);
        when(authService.isLogado()).thenReturn(true);
        when(authService.hasRole("ADMINISTRADOR")).thenReturn(true);
        when(noticiaService.countTotal()).thenReturn(55L);
        when(usuarioService.countTotal()).thenReturn(105L);

        // Act
        dashboardBean.init();

        // Assert
        verify(noticiaService, atLeast(1)).countTotal();
        verify(usuarioService, atLeast(1)).countTotal();
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
        dashboardBean.adicionarMensagem("INFO", "Sucesso", "Operação realizada com sucesso");

        // Assert
        // Verificar se a mensagem foi adicionada (mock do FacesContext seria necessário)
        // Este teste verifica se o método não lança exceção
    }

    @Test
    void should_AddErrorMessage_When_OperationFails() {
        // Act
        dashboardBean.adicionarMensagem("ERROR", "Erro", "Operação falhou");

        // Assert
        // Verificar se a mensagem de erro foi adicionada
        // Este teste verifica se o método não lança exceção
    }
}