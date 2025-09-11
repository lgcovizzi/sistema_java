package com.sistema.java.unit.bean;

import com.sistema.java.bean.MenuBean;
import com.sistema.java.model.entity.Usuario;
import com.sistema.java.model.enums.Papel;
import com.sistema.java.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Testes unitários para MenuBean
 * Referência: Testes e Qualidade de Código - project_rules.md
 * Referência: Controle de Acesso - project_rules.md
 */
@ExtendWith(MockitoExtension.class)
class MenuBeanTest {

    @Mock
    private AuthService authService;

    @InjectMocks
    private MenuBean menuBean;

    private Usuario usuarioMock;
    private Usuario adminMock;
    private Usuario colaboradorMock;
    private Usuario associadoMock;
    private Usuario parceiroMock;
    private Usuario fundadorMock;

    @BeforeEach
    void setUp() {
        // Criar usuário comum mock
        usuarioMock = new Usuario();
        usuarioMock.setId(1L);
        usuarioMock.setNome("João");
        usuarioMock.setEmail("joao@email.com");
        usuarioMock.setPapel(Papel.USUARIO);
        usuarioMock.setAtivo(true);
        usuarioMock.setDataCriacao(LocalDateTime.now());

        // Criar administrador mock
        adminMock = new Usuario();
        adminMock.setId(2L);
        adminMock.setNome("Admin");
        adminMock.setEmail("admin@email.com");
        adminMock.setPapel(Papel.ADMINISTRADOR);
        adminMock.setAtivo(true);
        adminMock.setDataCriacao(LocalDateTime.now());

        // Criar colaborador mock
        colaboradorMock = new Usuario();
        colaboradorMock.setId(3L);
        colaboradorMock.setNome("Maria");
        colaboradorMock.setEmail("maria@email.com");
        colaboradorMock.setPapel(Papel.COLABORADOR);
        colaboradorMock.setAtivo(true);
        colaboradorMock.setDataCriacao(LocalDateTime.now());

        // Criar associado mock
        associadoMock = new Usuario();
        associadoMock.setId(4L);
        associadoMock.setNome("Carlos");
        associadoMock.setEmail("carlos@email.com");
        associadoMock.setPapel(Papel.ASSOCIADO);
        associadoMock.setAtivo(true);
        associadoMock.setDataCriacao(LocalDateTime.now());

        // Criar parceiro mock
        parceiroMock = new Usuario();
        parceiroMock.setId(5L);
        parceiroMock.setNome("Ana");
        parceiroMock.setEmail("ana@email.com");
        parceiroMock.setPapel(Papel.PARCEIRO);
        parceiroMock.setAtivo(true);
        parceiroMock.setDataCriacao(LocalDateTime.now());

        // Criar fundador mock
        fundadorMock = new Usuario();
        fundadorMock.setId(6L);
        fundadorMock.setNome("Fundador");
        fundadorMock.setEmail("fundador@email.com");
        fundadorMock.setPapel(Papel.FUNDADOR);
        fundadorMock.setAtivo(true);
        fundadorMock.setDataCriacao(LocalDateTime.now());
    }

    @Test
    void should_InitializeCorrectly_When_PostConstructCalled() {
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
    void should_BuildAdminMenu_When_UserIsAdmin() {
        // Arrange
        when(authService.getUsuarioLogado()).thenReturn(adminMock);
        when(authService.isLogado()).thenReturn(true);
        when(authService.hasRole("ADMINISTRADOR")).thenReturn(true);

        // Act
        menuBean.init();
        List<MenuBean.MenuItem> menuItems = menuBean.getMenuItems();

        // Assert
        assertThat(menuItems).isNotEmpty();
        assertThat(menuItems.stream().anyMatch(item -> item.getLabel().equals("Dashboard"))).isTrue();
        assertThat(menuItems.stream().anyMatch(item -> item.getLabel().equals("Usuários"))).isTrue();
        assertThat(menuItems.stream().anyMatch(item -> item.getLabel().equals("Notícias"))).isTrue();
        assertThat(menuItems.stream().anyMatch(item -> item.getLabel().equals("Categorias"))).isTrue();
        assertThat(menuItems.stream().anyMatch(item -> item.getLabel().equals("Comentários"))).isTrue();
        assertThat(menuItems.stream().anyMatch(item -> item.getLabel().equals("Configurações"))).isTrue();
    }

    @Test
    void should_BuildCollaboratorMenu_When_UserIsCollaborator() {
        // Arrange
        when(authService.getUsuarioLogado()).thenReturn(colaboradorMock);
        when(authService.isLogado()).thenReturn(true);
        when(authService.hasRole("COLABORADOR")).thenReturn(true);
        when(authService.hasRole("ADMINISTRADOR")).thenReturn(false);

        // Act
        menuBean.init();
        List<MenuBean.MenuItem> menuItems = menuBean.getMenuItems();

        // Assert
        assertThat(menuItems).isNotEmpty();
        assertThat(menuItems.stream().anyMatch(item -> item.getLabel().equals("Dashboard"))).isTrue();
        assertThat(menuItems.stream().anyMatch(item -> item.getLabel().equals("Notícias"))).isTrue();
        assertThat(menuItems.stream().anyMatch(item -> item.getLabel().equals("Comentários"))).isTrue();
        // Colaborador não deve ter acesso a usuários e configurações
        assertThat(menuItems.stream().anyMatch(item -> item.getLabel().equals("Usuários"))).isFalse();
        assertThat(menuItems.stream().anyMatch(item -> item.getLabel().equals("Configurações"))).isFalse();
    }

    @Test
    void should_BuildUserMenu_When_UserIsRegularUser() {
        // Arrange
        when(authService.getUsuarioLogado()).thenReturn(usuarioMock);
        when(authService.isLogado()).thenReturn(true);
        when(authService.hasRole("ADMINISTRADOR")).thenReturn(false);
        when(authService.hasRole("COLABORADOR")).thenReturn(false);
        when(authService.hasRole("ASSOCIADO")).thenReturn(false);
        when(authService.hasRole("PARCEIRO")).thenReturn(false);

        // Act
        menuBean.init();
        List<MenuBean.MenuItem> menuItems = menuBean.getMenuItems();

        // Assert
        assertThat(menuItems).isNotEmpty();
        assertThat(menuItems.stream().anyMatch(item -> item.getLabel().equals("Dashboard"))).isTrue();
        assertThat(menuItems.stream().anyMatch(item -> item.getLabel().equals("Notícias"))).isTrue();
        assertThat(menuItems.stream().anyMatch(item -> item.getLabel().equals("Perfil"))).isTrue();
        // Usuário comum não deve ter acesso administrativo
        assertThat(menuItems.stream().anyMatch(item -> item.getLabel().equals("Usuários"))).isFalse();
        assertThat(menuItems.stream().anyMatch(item -> item.getLabel().equals("Categorias"))).isFalse();
    }

    @Test
    void should_BuildAssociadoMenu_When_UserIsAssociado() {
        // Arrange
        when(authService.getUsuarioLogado()).thenReturn(associadoMock);
        when(authService.isLogado()).thenReturn(true);
        when(authService.hasRole("ASSOCIADO")).thenReturn(true);
        when(authService.hasRole("ADMINISTRADOR")).thenReturn(false);
        when(authService.hasRole("COLABORADOR")).thenReturn(false);

        // Act
        menuBean.init();
        List<MenuBean.MenuItem> menuItems = menuBean.getMenuItems();

        // Assert
        assertThat(menuItems).isNotEmpty();
        assertThat(menuItems.stream().anyMatch(item -> item.getLabel().equals("Dashboard"))).isTrue();
        assertThat(menuItems.stream().anyMatch(item -> item.getLabel().equals("Área do Associado"))).isTrue();
        assertThat(menuItems.stream().anyMatch(item -> item.getLabel().equals("Benefícios"))).isTrue();
    }

    @Test
    void should_BuildParceiroMenu_When_UserIsParceiro() {
        // Arrange
        when(authService.getUsuarioLogado()).thenReturn(parceiroMock);
        when(authService.isLogado()).thenReturn(true);
        when(authService.hasRole("PARCEIRO")).thenReturn(true);
        when(authService.hasRole("ADMINISTRADOR")).thenReturn(false);
        when(authService.hasRole("COLABORADOR")).thenReturn(false);

        // Act
        menuBean.init();
        List<MenuBean.MenuItem> menuItems = menuBean.getMenuItems();

        // Assert
        assertThat(menuItems).isNotEmpty();
        assertThat(menuItems.stream().anyMatch(item -> item.getLabel().equals("Dashboard"))).isTrue();
        assertThat(menuItems.stream().anyMatch(item -> item.getLabel().equals("Área do Parceiro"))).isTrue();
        assertThat(menuItems.stream().anyMatch(item -> item.getLabel().equals("Relatórios"))).isTrue();
    }

    @Test
    void should_BuildFundadorMenu_When_UserIsFundador() {
        // Arrange
        when(authService.getUsuarioLogado()).thenReturn(fundadorMock);
        when(authService.isLogado()).thenReturn(true);
        when(authService.hasRole("FUNDADOR")).thenReturn(true);

        // Act
        menuBean.init();
        List<MenuBean.MenuItem> menuItems = menuBean.getMenuItems();

        // Assert
        assertThat(menuItems).isNotEmpty();
        assertThat(menuItems.stream().anyMatch(item -> item.getLabel().equals("Dashboard"))).isTrue();
        assertThat(menuItems.stream().anyMatch(item -> item.getLabel().equals("Usuários"))).isTrue();
        assertThat(menuItems.stream().anyMatch(item -> item.getLabel().equals("Configurações Avançadas"))).isTrue();
        // Fundador deve ter acesso total
        assertThat(menuItems.stream().anyMatch(item -> item.getLabel().equals("Auditoria"))).isTrue();
    }

    @Test
    void should_BuildEmptyMenu_When_UserNotLoggedIn() {
        // Arrange
        when(authService.getUsuarioLogado()).thenReturn(null);
        when(authService.isLogado()).thenReturn(false);

        // Act
        menuBean.init();
        List<MenuBean.MenuItem> menuItems = menuBean.getMenuItems();

        // Assert
        assertThat(menuItems).isEmpty();
    }

    @Test
    void should_UpdateMenuItems_When_UserRoleChanges() {
        // Arrange
        when(authService.getUsuarioLogado()).thenReturn(usuarioMock);
        when(authService.isLogado()).thenReturn(true);
        when(authService.hasRole("ADMINISTRADOR")).thenReturn(false);
        when(authService.hasRole("COLABORADOR")).thenReturn(false);

        menuBean.init();
        int initialMenuSize = menuBean.getMenuItems().size();

        // Simular mudança de papel para colaborador
        when(authService.hasRole("COLABORADOR")).thenReturn(true);

        // Act
        menuBean.atualizarMenu();

        // Assert
        assertThat(menuBean.getMenuItems().size()).isGreaterThan(initialMenuSize);
    }

    @Test
    void should_ToggleMenuState_When_ToggleMethodIsCalled() {
        // Arrange
        menuBean.setMenuCollapsed(false);

        // Act
        menuBean.toggleMenu();

        // Assert
        assertThat(menuBean.isMenuCollapsed()).isTrue();

        // Act again
        menuBean.toggleMenu();

        // Assert
        assertThat(menuBean.isMenuCollapsed()).isFalse();
    }

    @Test
    void should_SetActiveMenuItem_When_NavigatingToPage() {
        // Arrange
        when(authService.getUsuarioLogado()).thenReturn(adminMock);
        when(authService.isLogado()).thenReturn(true);
        when(authService.hasRole("ADMINISTRADOR")).thenReturn(true);
        menuBean.init();

        // Act
        menuBean.setActiveMenuItem("dashboard");

        // Assert
        assertThat(menuBean.getActiveMenuItem()).isEqualTo("dashboard");
    }

    @Test
    void should_CheckIfMenuItemIsActive_When_CheckingActiveState() {
        // Arrange
        menuBean.setActiveMenuItem("noticias");

        // Act & Assert
        assertThat(menuBean.isMenuItemActive("noticias")).isTrue();
        assertThat(menuBean.isMenuItemActive("usuarios")).isFalse();
    }

    @Test
    void should_BuildMenuWithSubItems_When_UserHasPermissions() {
        // Arrange
        when(authService.getUsuarioLogado()).thenReturn(adminMock);
        when(authService.isLogado()).thenReturn(true);
        when(authService.hasRole("ADMINISTRADOR")).thenReturn(true);

        // Act
        menuBean.init();
        List<MenuBean.MenuItem> menuItems = menuBean.getMenuItems();

        // Assert
        MenuBean.MenuItem usuariosMenu = menuItems.stream()
            .filter(item -> item.getLabel().equals("Usuários"))
            .findFirst()
            .orElse(null);

        assertThat(usuariosMenu).isNotNull();
        assertThat(usuariosMenu.getSubItems()).isNotEmpty();
        assertThat(usuariosMenu.getSubItems().stream()
            .anyMatch(subItem -> subItem.getLabel().equals("Listar Usuários"))).isTrue();
        assertThat(usuariosMenu.getSubItems().stream()
            .anyMatch(subItem -> subItem.getLabel().equals("Novo Usuário"))).isTrue();
    }

    @Test
    void should_SetCorrectUrls_When_BuildingMenuItems() {
        // Arrange
        when(authService.getUsuarioLogado()).thenReturn(adminMock);
        when(authService.isLogado()).thenReturn(true);
        when(authService.hasRole("ADMINISTRADOR")).thenReturn(true);

        // Act
        menuBean.init();
        List<MenuBean.MenuItem> menuItems = menuBean.getMenuItems();

        // Assert
        MenuBean.MenuItem dashboardMenu = menuItems.stream()
            .filter(item -> item.getLabel().equals("Dashboard"))
            .findFirst()
            .orElse(null);

        assertThat(dashboardMenu).isNotNull();
        assertThat(dashboardMenu.getUrl()).isEqualTo("/dashboard.xhtml");
    }

    @Test
    void should_SetCorrectIcons_When_BuildingMenuItems() {
        // Arrange
        when(authService.getUsuarioLogado()).thenReturn(usuarioMock);
        when(authService.isLogado()).thenReturn(true);
        when(authService.hasRole("ADMINISTRADOR")).thenReturn(false);
        when(authService.hasRole("COLABORADOR")).thenReturn(false);

        // Act
        menuBean.init();
        List<MenuBean.MenuItem> menuItems = menuBean.getMenuItems();

        // Assert
        MenuBean.MenuItem dashboardMenu = menuItems.stream()
            .filter(item -> item.getLabel().equals("Dashboard"))
            .findFirst()
            .orElse(null);

        assertThat(dashboardMenu).isNotNull();
        assertThat(dashboardMenu.getIcon()).isNotEmpty();
    }

    @Test
    void should_HandleNullUser_When_BuildingMenu() {
        // Arrange
        when(authService.getUsuarioLogado()).thenReturn(null);
        when(authService.isLogado()).thenReturn(false);

        // Act
        menuBean.init();

        // Assert
        assertThat(menuBean.getMenuItems()).isEmpty();
        // Não deve lançar exceção
    }

    @Test
    void should_RefreshMenu_When_RefreshMethodIsCalled() {
        // Arrange
        when(authService.getUsuarioLogado()).thenReturn(usuarioMock);
        when(authService.isLogado()).thenReturn(true);
        menuBean.init();
        int initialSize = menuBean.getMenuItems().size();

        // Simular mudança de permissões
        when(authService.hasRole("COLABORADOR")).thenReturn(true);

        // Act
        menuBean.atualizarMenu();

        // Assert
        verify(authService, atLeast(2)).getUsuarioLogado();
    }

    @Test
    void should_CollapseMenu_When_CollapseMethodIsCalled() {
        // Arrange
        menuBean.setMenuCollapsed(false);

        // Act
        menuBean.collapseMenu();

        // Assert
        assertThat(menuBean.isMenuCollapsed()).isTrue();
    }

    @Test
    void should_ExpandMenu_When_ExpandMethodIsCalled() {
        // Arrange
        menuBean.setMenuCollapsed(true);

        // Act
        menuBean.expandMenu();

        // Assert
        assertThat(menuBean.isMenuCollapsed()).isFalse();
    }

    @Test
    void should_CreateMenuItemWithAllProperties_When_BuildingMenu() {
        // Arrange
        when(authService.getUsuarioLogado()).thenReturn(adminMock);
        when(authService.isLogado()).thenReturn(true);
        when(authService.hasRole("ADMINISTRADOR")).thenReturn(true);

        // Act
        menuBean.init();
        List<MenuBean.MenuItem> menuItems = menuBean.getMenuItems();

        // Assert
        MenuBean.MenuItem firstItem = menuItems.get(0);
        assertThat(firstItem.getLabel()).isNotNull();
        assertThat(firstItem.getUrl()).isNotNull();
        assertThat(firstItem.getIcon()).isNotNull();
        assertThat(firstItem.isVisible()).isTrue();
    }

    @Test
    void should_FilterMenuItemsByPermission_When_UserHasLimitedAccess() {
        // Arrange
        when(authService.getUsuarioLogado()).thenReturn(usuarioMock);
        when(authService.isLogado()).thenReturn(true);
        when(authService.hasRole("ADMINISTRADOR")).thenReturn(false);
        when(authService.hasRole("COLABORADOR")).thenReturn(false);
        when(authService.hasRole("ASSOCIADO")).thenReturn(false);
        when(authService.hasRole("PARCEIRO")).thenReturn(false);

        // Act
        menuBean.init();
        List<MenuBean.MenuItem> menuItems = menuBean.getMenuItems();

        // Assert
        // Verificar que itens administrativos não estão presentes
        assertThat(menuItems.stream().noneMatch(item -> 
            item.getLabel().equals("Usuários") || 
            item.getLabel().equals("Configurações") ||
            item.getLabel().equals("Auditoria")
        )).isTrue();
    }
}