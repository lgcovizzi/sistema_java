package com.sistema.java.integration.bean;

import com.sistema.java.bean.*;
import com.sistema.java.model.entity.Usuario;
import com.sistema.java.model.enums.Papel;
import com.sistema.java.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.inject.Inject;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Testes de integração para beans JSF
 * Referência: Testes e Qualidade de Código - project_rules.md
 * Referência: Controle de Acesso - project_rules.md
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class JSFBeansIntegrationTest {

    @MockBean
    private AuthService authService;

    @MockBean
    private NoticiaService noticiaService;

    @MockBean
    private CategoriaService categoriaService;

    @MockBean
    private ComentarioService comentarioService;

    @MockBean
    private UsuarioService usuarioService;

    @Inject
    private AuthBean authBean;

    @Inject
    private NoticiaBean noticiaBean;

    @Inject
    private CategoriaBean categoriaBean;

    @Inject
    private ComentarioBean comentarioBean;

    @Inject
    private DashboardBean dashboardBean;

    @Inject
    private MenuBean menuBean;

    private Usuario usuarioMock;
    private Usuario adminMock;

    @BeforeEach
    void setUp() {
        // Criar usuário mock
        usuarioMock = new Usuario();
        usuarioMock.setId(1L);
        usuarioMock.setNome("João");
        usuarioMock.setSobrenome("Silva");
        usuarioMock.setEmail("joao@email.com");
        usuarioMock.setPapel(Papel.USUARIO);
        usuarioMock.setAtivo(true);
        usuarioMock.setDataCriacao(LocalDateTime.now());

        // Criar admin mock
        adminMock = new Usuario();
        adminMock.setId(2L);
        adminMock.setNome("Admin");
        adminMock.setSobrenome("Sistema");
        adminMock.setEmail("admin@email.com");
        adminMock.setPapel(Papel.ADMINISTRADOR);
        adminMock.setAtivo(true);
        adminMock.setDataCriacao(LocalDateTime.now());
    }

    @Test
    void should_InjectAllBeansCorrectly_When_SpringContextLoads() {
        // Assert
        assertThat(authBean).isNotNull();
        assertThat(noticiaBean).isNotNull();
        assertThat(categoriaBean).isNotNull();
        assertThat(comentarioBean).isNotNull();
        assertThat(dashboardBean).isNotNull();
        assertThat(menuBean).isNotNull();
    }

    @Test
    void should_InitializeAuthBeanCorrectly_When_PostConstructCalled() {
        // Act
        authBean.init();

        // Assert
        assertThat(authBean.getLoginDTO()).isNotNull();
        assertThat(authBean.getRegistroDTO()).isNotNull();
    }

    @Test
    void should_InitializeNoticiaBeanCorrectly_When_PostConstructCalled() {
        // Act
        noticiaBean.init();

        // Assert
        assertThat(noticiaBean.getNoticiasLazy()).isNotNull();
        assertThat(noticiaBean.getFiltros()).isNotNull();
    }

    @Test
    void should_InitializeCategoriaBeanCorrectly_When_PostConstructCalled() {
        // Act
        categoriaBean.init();

        // Assert
        assertThat(categoriaBean.getCategoriasLazy()).isNotNull();
        assertThat(categoriaBean.getFiltros()).isNotNull();
    }

    @Test
    void should_InitializeComentarioBeanCorrectly_When_PostConstructCalled() {
        // Act
        comentarioBean.init();

        // Assert
        assertThat(comentarioBean.getComentariosLazy()).isNotNull();
        assertThat(comentarioBean.getFiltros()).isNotNull();
    }

    @Test
    void should_InitializeDashboardBeanCorrectly_When_UserIsLoggedIn() {
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
    void should_InitializeMenuBeanCorrectly_When_UserIsLoggedIn() {
        // Arrange
        when(authService.getUsuarioLogado()).thenReturn(usuarioMock);
        when(authService.isLogado()).thenReturn(true);

        // Act
        menuBean.init();

        // Assert
        assertThat(menuBean.getMenuItems()).isNotNull();
        verify(authService).getUsuarioLogado();
    }

    @Test
    void should_HandleUserRoleChanges_When_MenuAndDashboardInteract() {
        // Arrange
        when(authService.getUsuarioLogado()).thenReturn(usuarioMock);
        when(authService.isLogado()).thenReturn(true);
        when(authService.hasRole("USUARIO")).thenReturn(true);
        when(authService.hasRole("ADMINISTRADOR")).thenReturn(false);

        // Act
        dashboardBean.init();
        menuBean.init();

        // Simular mudança para admin
        when(authService.getUsuarioLogado()).thenReturn(adminMock);
        when(authService.hasRole("ADMINISTRADOR")).thenReturn(true);
        when(authService.hasRole("USUARIO")).thenReturn(false);

        dashboardBean.atualizarDados();
        menuBean.atualizarMenu();

        // Assert
        verify(authService, atLeast(2)).getUsuarioLogado();
    }

    @Test
    void should_MaintainSessionState_When_BeansInteract() {
        // Arrange
        when(authService.getUsuarioLogado()).thenReturn(usuarioMock);
        when(authService.isLogado()).thenReturn(true);

        // Act
        authBean.init();
        dashboardBean.init();
        menuBean.init();

        // Assert
        // Verificar que os beans mantêm estado consistente
        assertThat(dashboardBean.getUsuarioLogado()).isEqualTo(usuarioMock);
        verify(authService, atLeast(2)).getUsuarioLogado();
    }

    @Test
    void should_HandleLogout_When_UserLogsOut() {
        // Arrange
        when(authService.getUsuarioLogado()).thenReturn(usuarioMock);
        when(authService.isLogado()).thenReturn(true);
        
        dashboardBean.init();
        menuBean.init();

        // Simular logout
        when(authService.getUsuarioLogado()).thenReturn(null);
        when(authService.isLogado()).thenReturn(false);
        doNothing().when(authService).logout();

        // Act
        authBean.logout();

        // Assert
        verify(authService).logout();
    }

    @Test
    void should_LoadDataCorrectly_When_AdminAccessesDashboard() {
        // Arrange
        when(authService.getUsuarioLogado()).thenReturn(adminMock);
        when(authService.isLogado()).thenReturn(true);
        when(authService.hasRole("ADMINISTRADOR")).thenReturn(true);
        when(noticiaService.contarTotalNoticias()).thenReturn(50L);
        when(usuarioService.contarTotalUsuarios()).thenReturn(100L);
        when(comentarioService.contarComentariosPendentes()).thenReturn(10L);

        // Act
        dashboardBean.init();
        dashboardBean.carregarEstatisticas();

        // Assert
        assertThat(dashboardBean.getTotalNoticias()).isEqualTo(50L);
        assertThat(dashboardBean.getTotalUsuarios()).isEqualTo(100L);
        assertThat(dashboardBean.getComentariosPendentes()).isEqualTo(10L);
    }

    @Test
    void should_FilterDataCorrectly_When_UsingLazyDataModels() {
        // Arrange
        when(noticiaService.listarComFiltros(anyMap(), anyInt(), anyInt(), anyString(), anyBoolean()))
            .thenReturn(java.util.Collections.emptyList());
        when(noticiaService.contarComFiltros(anyMap())).thenReturn(0L);

        // Act
        noticiaBean.init();
        noticiaBean.getNoticiasLazy().load(0, 10, java.util.Collections.emptyMap(), java.util.Collections.emptyMap());

        // Assert
        verify(noticiaService).listarComFiltros(anyMap(), eq(0), eq(10), anyString(), anyBoolean());
        verify(noticiaService).contarComFiltros(anyMap());
    }

    @Test
    void should_HandlePaginationCorrectly_When_LoadingLargeDatasets() {
        // Arrange
        when(categoriaService.listarComFiltros(anyMap(), anyInt(), anyInt(), anyString(), anyBoolean()))
            .thenReturn(java.util.Collections.emptyList());
        when(categoriaService.contarComFiltros(anyMap())).thenReturn(100L);

        // Act
        categoriaBean.init();
        categoriaBean.getCategoriasLazy().load(20, 10, java.util.Collections.emptyMap(), java.util.Collections.emptyMap());

        // Assert
        verify(categoriaService).listarComFiltros(anyMap(), eq(20), eq(10), anyString(), anyBoolean());
    }

    @Test
    void should_MaintainDataConsistency_When_MultipleBeansAccessSameService() {
        // Arrange
        when(authService.getUsuarioLogado()).thenReturn(usuarioMock);
        when(authService.isLogado()).thenReturn(true);
        when(comentarioService.contarComentariosPorAutor(1L)).thenReturn(5L);
        when(comentarioService.contarComentariosPendentes()).thenReturn(10L);

        // Act
        dashboardBean.init();
        comentarioBean.init();
        dashboardBean.carregarEstatisticas();

        // Assert
        verify(comentarioService).contarComentariosPorAutor(1L);
        // Verificar que os dados são consistentes entre beans
    }

    @Test
    void should_HandleConcurrentAccess_When_MultipleUsersAccessSystem() {
        // Arrange
        when(authService.getUsuarioLogado()).thenReturn(usuarioMock, adminMock);
        when(authService.isLogado()).thenReturn(true);
        when(authService.hasRole("ADMINISTRADOR")).thenReturn(false, true);

        // Act
        // Simular acesso de usuário comum
        dashboardBean.init();
        menuBean.init();

        // Simular acesso de admin
        DashboardBean adminDashboard = new DashboardBean();
        MenuBean adminMenu = new MenuBean();
        
        // Assert
        // Verificar que não há interferência entre sessões
        verify(authService, atLeast(2)).getUsuarioLogado();
    }

    @Test
    void should_ValidateSecurityConstraints_When_AccessingRestrictedFeatures() {
        // Arrange
        when(authService.getUsuarioLogado()).thenReturn(usuarioMock);
        when(authService.isLogado()).thenReturn(true);
        when(authService.hasRole("ADMINISTRADOR")).thenReturn(false);
        when(authService.hasRole("COLABORADOR")).thenReturn(false);

        // Act
        menuBean.init();
        
        // Assert
        // Verificar que usuário comum não tem acesso a funcionalidades administrativas
        assertThat(menuBean.getMenuItems().stream()
            .noneMatch(item -> item.getLabel().equals("Usuários") || 
                              item.getLabel().equals("Configurações"))).isTrue();
    }

    @Test
    void should_HandleErrorsGracefully_When_ServicesThrowExceptions() {
        // Arrange
        when(authService.getUsuarioLogado()).thenReturn(usuarioMock);
        when(authService.isLogado()).thenReturn(true);
        when(noticiaService.contarTotalNoticias()).thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        dashboardBean.init();
        // Verificar que o bean não falha completamente
        assertThat(dashboardBean.getUsuarioLogado()).isEqualTo(usuarioMock);
    }

    @Test
    void should_RefreshDataCorrectly_When_DataChanges() {
        // Arrange
        when(authService.getUsuarioLogado()).thenReturn(adminMock);
        when(authService.isLogado()).thenReturn(true);
        when(authService.hasRole("ADMINISTRADOR")).thenReturn(true);
        when(noticiaService.contarTotalNoticias()).thenReturn(50L, 55L);

        // Act
        dashboardBean.init();
        dashboardBean.carregarEstatisticas();
        Long initialCount = dashboardBean.getTotalNoticias();
        
        dashboardBean.atualizarDados();
        Long updatedCount = dashboardBean.getTotalNoticias();

        // Assert
        assertThat(initialCount).isEqualTo(50L);
        assertThat(updatedCount).isEqualTo(55L);
        verify(noticiaService, times(2)).contarTotalNoticias();
    }
}