package com.sistema.java.unit.bean;

import com.sistema.java.bean.AuthBean;
import com.sistema.java.dto.UsuarioDTO;
import com.sistema.java.model.entity.Usuario;
import com.sistema.java.model.enums.Role;
import com.sistema.java.service.AuthService;
import com.sistema.java.service.UsuarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import javax.faces.application.FacesMessage;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Testes unitários para AuthBean
 * Referência: Testes e Qualidade de Código - project_rules.md
 * Referência: Login e Registro - project_rules.md
 * Referência: Controle de Acesso - project_rules.md
 */
@ExtendWith(MockitoExtension.class)
class AuthBeanTest {

    @Mock
    private AuthService authService;

    @Mock
    private UsuarioService usuarioService;

    @Mock
    private FacesContext facesContext;

    @Mock
    private ExternalContext externalContext;

    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthBean authBean;

    private Usuario usuarioMock;
    private UsuarioDTO usuarioDTOMock;

    @BeforeEach
    void setUp() {
        // Configurar mocks básicos
        when(facesContext.getExternalContext()).thenReturn(externalContext);
        
        // Criar usuário mock
        usuarioMock = new Usuario();
        usuarioMock.setId(1L);
        usuarioMock.setNome("João");
        usuarioMock.setSobrenome("Silva");
        usuarioMock.setEmail("joao@teste.com");
        usuarioMock.setCpf("12345678901");
        usuarioMock.setSenha("senhaEncriptada");
        usuarioMock.setRole(Role.USUARIO);
        usuarioMock.setAtivo(true);
        usuarioMock.setDataCriacao(LocalDateTime.now());
        
        // Criar DTO mock
        usuarioDTOMock = new UsuarioDTO();
        usuarioDTOMock.setNome("João");
        usuarioDTOMock.setSobrenome("Silva");
        usuarioDTOMock.setEmail("joao@teste.com");
        usuarioDTOMock.setCpf("12345678901");
        usuarioDTOMock.setSenha("senha123");
        usuarioDTOMock.setDataNascimento(LocalDate.of(1990, 1, 1));
    }

    @Test
    void should_LoginSuccessfully_When_CredentialsAreValid() {
        // Arrange
        authBean.setEmail("joao@teste.com");
        authBean.setSenha("senha123");
        when(authService.autenticar("joao@teste.com", "senha123")).thenReturn(usuarioMock);

        // Act
        String result = authBean.login();

        // Assert
        assertThat(result).isEqualTo("/dashboard?faces-redirect=true");
        assertThat(authBean.getUsuarioLogado()).isEqualTo(usuarioMock);
        verify(authService).autenticar("joao@teste.com", "senha123");
    }

    @Test
    void should_RedirectToAdminDashboard_When_UserIsAdmin() {
        // Arrange
        usuarioMock.setRole(Role.ADMINISTRADOR);
        authBean.setEmail("admin@teste.com");
        authBean.setSenha("senha123");
        when(authService.autenticar("admin@teste.com", "senha123")).thenReturn(usuarioMock);

        // Act
        String result = authBean.login();

        // Assert
        assertThat(result).isEqualTo("/admin/dashboard?faces-redirect=true");
    }

    @Test
    void should_ReturnNull_When_CredentialsAreInvalid() {
        // Arrange
        authBean.setEmail("joao@teste.com");
        authBean.setSenha("senhaErrada");
        when(authService.autenticar("joao@teste.com", "senhaErrada")).thenReturn(null);

        // Act
        String result = authBean.login();

        // Assert
        assertThat(result).isNull();
        assertThat(authBean.getUsuarioLogado()).isNull();
    }

    @Test
    void should_RegisterUserSuccessfully_When_DataIsValid() {
        // Arrange
        authBean.setNovoUsuario(usuarioDTOMock);
        when(usuarioService.existePorEmail(usuarioDTOMock.getEmail())).thenReturn(false);
        when(usuarioService.existePorCpf(usuarioDTOMock.getCpf())).thenReturn(false);
        when(usuarioService.criarUsuario(any(UsuarioDTO.class))).thenReturn(usuarioMock);

        // Act
        String result = authBean.registrar();

        // Assert
        assertThat(result).isEqualTo("/login?faces-redirect=true");
        verify(usuarioService).criarUsuario(any(UsuarioDTO.class));
    }

    @Test
    void should_ReturnNull_When_EmailAlreadyExists() {
        // Arrange
        authBean.setNovoUsuario(usuarioDTOMock);
        when(usuarioService.existePorEmail(usuarioDTOMock.getEmail())).thenReturn(true);

        // Act
        String result = authBean.registrar();

        // Assert
        assertThat(result).isNull();
        verify(usuarioService, never()).criarUsuario(any(UsuarioDTO.class));
    }

    @Test
    void should_ReturnNull_When_CpfAlreadyExists() {
        // Arrange
        authBean.setNovoUsuario(usuarioDTOMock);
        when(usuarioService.existePorEmail(usuarioDTOMock.getEmail())).thenReturn(false);
        when(usuarioService.existePorCpf(usuarioDTOMock.getCpf())).thenReturn(true);

        // Act
        String result = authBean.registrar();

        // Assert
        assertThat(result).isNull();
        verify(usuarioService, never()).criarUsuario(any(UsuarioDTO.class));
    }

    @Test
    void should_LogoutSuccessfully_When_UserIsLoggedIn() {
        // Arrange
        authBean.setUsuarioLogado(usuarioMock);

        // Act
        String result = authBean.logout();

        // Assert
        assertThat(result).isEqualTo("/index?faces-redirect=true");
        assertThat(authBean.getUsuarioLogado()).isNull();
        verify(externalContext).invalidateSession();
    }

    @Test
    void should_InitiatePasswordReset_When_EmailExists() {
        // Arrange
        authBean.setEmailRecuperacao("joao@teste.com");
        when(usuarioService.buscarPorEmail("joao@teste.com")).thenReturn(usuarioMock);
        when(authService.solicitarRecuperacaoSenha("joao@teste.com")).thenReturn(true);

        // Act
        authBean.solicitarRecuperacaoSenha();

        // Assert
        verify(authService).solicitarRecuperacaoSenha("joao@teste.com");
    }

    @Test
    void should_ResetPassword_When_TokenIsValid() {
        // Arrange
        authBean.setTokenRecuperacao("validToken");
        authBean.setNovaSenha("novaSenha123");
        authBean.setConfirmaNovaSenha("novaSenha123");
        when(authService.executarRecuperacaoSenha("validToken", "novaSenha123")).thenReturn(true);

        // Act
        String result = authBean.executarRecuperacaoSenha();

        // Assert
        assertThat(result).isEqualTo("/login?faces-redirect=true");
        verify(authService).executarRecuperacaoSenha("validToken", "novaSenha123");
    }

    @Test
    void should_ReturnNull_When_PasswordsDoNotMatch() {
        // Arrange
        authBean.setTokenRecuperacao("validToken");
        authBean.setNovaSenha("novaSenha123");
        authBean.setConfirmaNovaSenha("senhasDiferentes");

        // Act
        String result = authBean.executarRecuperacaoSenha();

        // Assert
        assertThat(result).isNull();
        verify(authService, never()).executarRecuperacaoSenha(anyString(), anyString());
    }

    @Test
    void should_ToggleRegistrationForm_When_Called() {
        // Arrange
        boolean initialState = authBean.isMostrandoFormularioRegistro();

        // Act
        authBean.alternarFormularioRegistro();

        // Assert
        assertThat(authBean.isMostrandoFormularioRegistro()).isNotEqualTo(initialState);
    }

    @Test
    void should_ClearFields_When_Called() {
        // Arrange
        authBean.setEmail("test@test.com");
        authBean.setSenha("password");
        authBean.setNovoUsuario(usuarioDTOMock);

        // Act
        authBean.limparCampos();

        // Assert
        assertThat(authBean.getEmail()).isNull();
        assertThat(authBean.getSenha()).isNull();
        assertThat(authBean.getNovoUsuario()).isNotNull();
        assertThat(authBean.getNovoUsuario().getEmail()).isNull();
    }

    @Test
    void should_ValidateMinimumAge_When_RegisteringUser() {
        // Arrange
        usuarioDTOMock.setDataNascimento(LocalDate.now().minusYears(15)); // Menor que 16 anos
        authBean.setNovoUsuario(usuarioDTOMock);
        when(usuarioService.existePorEmail(usuarioDTOMock.getEmail())).thenReturn(false);
        when(usuarioService.existePorCpf(usuarioDTOMock.getCpf())).thenReturn(false);

        // Act
        String result = authBean.registrar();

        // Assert
        assertThat(result).isNull();
        verify(usuarioService, never()).criarUsuario(any(UsuarioDTO.class));
    }

    @Test
    void should_SetDefaultRole_When_RegisteringUser() {
        // Arrange
        authBean.setNovoUsuario(usuarioDTOMock);
        when(usuarioService.existePorEmail(usuarioDTOMock.getEmail())).thenReturn(false);
        when(usuarioService.existePorCpf(usuarioDTOMock.getCpf())).thenReturn(false);
        when(usuarioService.criarUsuario(any(UsuarioDTO.class))).thenReturn(usuarioMock);

        // Act
        authBean.registrar();

        // Assert
        verify(usuarioService).criarUsuario(argThat(dto -> 
            dto.getRole() == null || dto.getRole() == Role.USUARIO
        ));
    }
}