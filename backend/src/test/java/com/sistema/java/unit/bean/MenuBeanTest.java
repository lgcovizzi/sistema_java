package com.sistema.java.unit.bean;

import com.sistema.java.bean.MenuBean;
import com.sistema.java.model.entity.Usuario;
import com.sistema.java.model.enums.PapelUsuario;
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
        usuarioMock.setPapel(PapelUsuario.USUARIO);
        usuarioMock.setAtivo(true);
        usuarioMock.setDataCriacao(LocalDateTime.now());

        // Criar administrador mock
        adminMock = new Usuario();
        adminMock.setId(2L);
        adminMock.setNome("Admin");
        adminMock.setEmail("admin@email.com");
        adminMock.setPapel(PapelUsuario.ADMINISTRADOR);
        adminMock.setAtivo(true);
        adminMock.setDataCriacao(LocalDateTime.now());

        // Criar colaborador mock
        colaboradorMock = new Usuario();
        colaboradorMock.setId(3L);
        colaboradorMock.setNome("Maria");
        colaboradorMock.setEmail("maria@email.com");
        colaboradorMock.setPapel(PapelUsuario.COLABORADOR);
        colaboradorMock.setAtivo(true);
        colaboradorMock.setDataCriacao(LocalDateTime.now());

        // Criar associado mock
        associadoMock = new Usuario();
        associadoMock.setId(4L);
        associadoMock.setNome("Carlos");
        associadoMock.setEmail("carlos@email.com");
        associadoMock.setPapel(PapelUsuario.ASSOCIADO);
        associadoMock.setAtivo(true);
        associadoMock.setDataCriacao(LocalDateTime.now());

        // Criar parceiro mock
        parceiroMock = new Usuario();
        parceiroMock.setId(5L);
        parceiroMock.setNome("Ana");
        parceiroMock.setEmail("ana@email.com");
        parceiroMock.setPapel(PapelUsuario.PARCEIRO);
        parceiroMock.setAtivo(true);
        parceiroMock.setDataCriacao(LocalDateTime.now());

        // Criar fundador mock
        fundadorMock = new Usuario();
        fundadorMock.setId(6L);
        fundadorMock.setNome("Fundador");
        fundadorMock.setEmail("fundador@email.com");
        fundadorMock.setPapel(PapelUsuario.FUNDADOR);
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
        List<MenuBean.MenuItem> menuItems = menuBean.getMenuPrincipal();

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
        when(authService.hasRole(PapelUsuario.COLABORADOR)).thenReturn(true);
        when(authService.hasRole("ADMINISTRADOR")).thenReturn(false);

        // Act
        menuBean.init();
        List<MenuBean.MenuItem> menuItems = menuBean.getMenuPrincipal();

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
        List<MenuBean.MenuItem> menuItems = menuBean.getMenuPrincipal();

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
        List<MenuBean.MenuItem> menuItems = menuBean.getMenuPrincipal();

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
        List<MenuBean.MenuItem> menuItems = menuBean.getMenuPrincipal();

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
        List<MenuBean.MenuItem> menuItems = menuBean.getMenuPrincipal();

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
        List<MenuBean.MenuItem> menuItems = menuBean.getMenuPrincipal();

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
        int initialMenuSize = menuBean.getMenuPrincipal().size();

        // Simular mudança de papel para colaborador
        when(authService.hasRole("COLABORADOR")).thenReturn(true);

        // Act
        menuBean.atualizarMenu();

        // Assert
        assertThat(menuBean.getMenuPrincipal().size()).isGreaterThan(initialMenuSize);
    }

    @Test
    void should_ToggleMenuState_When_ToggleMethodIsCalled() {
        // Arrange
        menuBean.setMenuMobileAberto(false);

        // Act
        menuBean.alternarMenuMobile();

        // Assert
        assertThat(menuBean.isMenuMobileAberto()).isTrue();

        // Act again
        menuBean.alternarMenuMobile();

        // Assert
        assertThat(menuBean.isMenuMobileAberto()).isFalse();
    }

    @Test
    void should_SetActiveMenuItem_When_NavigatingToPage() {
        // Arrange
        when(authService.getUsuarioLogado()).thenReturn(adminMock);
        when(authService.isLoggedIn()).thenReturn(true);
        when(authService.hasRole(PapelUsuario.ADMINISTRADOR)).thenReturn(true);
        menuBean.init();

        // Act
        menuBean.atualizarMenus();

        // Assert
        assertThat(menuBean.getMenuPrincipal()).isNotEmpty();
    }

    @Test
    void should_CheckIfMenuItemIsActive_When_CheckingActiveState() {
        // Arrange
        when(authService.isLoggedIn()).thenReturn(true);
        when(authService.getUsuarioLogado()).thenReturn(usuarioMock);
        
        // Act
        menuBean.init();

        // Assert
        assertThat(menuBean.getMenuPrincipal()).isNotNull();
    }

    @Test
    void should_BuildMenuWithSubItems_When_UserHasPermissions() {
        // Arrange
        when(authService.getUsuarioLogado()).thenReturn(adminMock);
        when(authService.isLoggedIn()).thenReturn(true);
        when(authService.hasRole(PapelUsuario.ADMINISTRADOR)).thenReturn(true);

        // Act
        menuBean.init();
        List<MenuBean.MenuItem> menuItems = menuBean.getMenuPrincipal();

        // Assert
        MenuBean.MenuItem usuariosMenu = menuItems.stream()
            .filter(item -> item.getLabel().equals("Usuários"))
            .findFirst()
            .orElse(null);

        assertThat(usuariosMenu).isNotNull();
        assertThat(usuariosMenu.getSubmenu()).isNotEmpty();
        assertThat(usuariosMenu.getSubmenu().stream()
            .anyMatch(subItem -> subItem.getLabel().equals("Listar Usuários"))).isTrue();
        assertThat(usuariosMenu.getSubmenu().stream()
            .anyMatch(subItem -> subItem.getLabel().equals("Novo Usuário"))).isTrue();
    }

    @Test
    void should_SetCorrectUrls_When_BuildingMenuItems() {
        // Arrange
        when(authService.getUsuarioLogado()).thenReturn(adminMock);
        when(authService.isLoggedIn()).thenReturn(true);
        when(authService.hasRole(PapelUsuario.ADMINISTRADOR)).thenReturn(true);

        // Act
        menuBean.init();
        List<MenuBean.MenuItem> menuItems = menuBean.getMenuPrincipal();

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
        when(authService.isLoggedIn()).thenReturn(true);
        when(authService.hasRole(PapelUsuario.ADMINISTRADOR)).thenReturn(false);
        when(authService.hasRole(PapelUsuario.COLABORADOR)).thenReturn(false);

        // Act
        menuBean.init();
        List<MenuBean.MenuItem> menuItems = menuBean.getMenuPrincipal();

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
        when(authService.isLoggedIn()).thenReturn(false);

        // Act
        menuBean.init();

        // Assert
        assertThat(menuBean.getMenuPrincipal()).isEmpty();
        // Não deve lançar exceção
    }

    @Test
    void should_RefreshMenu_When_RefreshMethodIsCalled() {
        // Arrange
        when(authService.getUsuarioLogado()).thenReturn(usuarioMock);
        when(authService.isLoggedIn()).thenReturn(true);
        menuBean.init();
        int initialSize = menuBean.getMenuPrincipal().size();

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
        menuBean.setMenuMobileAberto(true);

        // Act
        menuBean.setMenuMobileAberto(false);

        // Assert
        assertThat(menuBean.isMenuMobileAberto()).isFalse();
    }

    @Test
    void should_ExpandMenu_When_ExpandMethodIsCalled() {
        // Arrange
        menuBean.setMenuMobileAberto(false);

        // Act
        menuBean.setMenuMobileAberto(true);

        // Assert
        assertThat(menuBean.isMenuMobileAberto()).isTrue();
    }

    @Test
    void should_CreateMenuItemWithAllProperties_When_BuildingMenu() {
        // Arrange
        when(authService.getUsuarioLogado()).thenReturn(adminMock);
        when(authService.isLoggedIn()).thenReturn(true);
        when(authService.hasRole(PapelUsuario.ADMINISTRADOR)).thenReturn(true);

        // Act
        menuBean.init();
        List<MenuBean.MenuItem> menuItems = menuBean.getMenuPrincipal();

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
        when(authService.isLoggedIn()).thenReturn(true);
        when(authService.hasRole(PapelUsuario.ADMINISTRADOR)).thenReturn(false);
        when(authService.hasRole(PapelUsuario.COLABORADOR)).thenReturn(false);
        when(authService.hasRole(PapelUsuario.ASSOCIADO)).thenReturn(false);
        when(authService.hasRole(PapelUsuario.PARCEIRO)).thenReturn(false);

        // Act
        menuBean.init();
        List<MenuBean.MenuItem> menuItems = menuBean.getMenuPrincipal();

        // Assert
        // Verificar que itens administrativos não estão presentes
        assertThat(menuItems.stream().noneMatch(item -> 
            item.getLabel().equals("Usuários") || 
            item.getLabel().equals("Configurações") ||
            item.getLabel().equals("Auditoria")
        )).isTrue();
    }
}