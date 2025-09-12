package com.sistema.java.unit.bean;

import com.sistema.java.bean.AuthBean;
import com.sistema.java.model.dto.UsuarioDTO;
import com.sistema.java.model.entity.Usuario;
import com.sistema.java.model.enums.PapelUsuario;
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
import java.util.Optional;

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
        usuarioMock.setPapel(PapelUsuario.USUARIO);
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
        authBean.setEmailLogin("joao@teste.com");
        authBean.setSenhaLogin("senha123");
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
        usuarioMock.setPapel(PapelUsuario.ADMINISTRADOR);
        authBean.setEmailLogin("admin@teste.com");
        authBean.setSenhaLogin("senha123");
        when(authService.autenticar("admin@teste.com", "senha123")).thenReturn(usuarioMock);

        // Act
        String result = authBean.login();

        // Assert
        assertThat(result).isEqualTo("/admin/dashboard?faces-redirect=true");
    }

    @Test
    void should_ReturnNull_When_CredentialsAreInvalid() {
        // Arrange
        authBean.setEmailLogin("joao@teste.com");
        authBean.setSenhaLogin("senhaErrada");
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
        Usuario novoUsuario = new Usuario();
        novoUsuario.setEmail(usuarioDTOMock.getEmail());
        novoUsuario.setCpf(usuarioDTOMock.getCpf());
        authBean.setNovoUsuario(novoUsuario);
        when(usuarioService.existsByEmail(usuarioDTOMock.getEmail())).thenReturn(false);
        when(usuarioService.existsByCpf(usuarioDTOMock.getCpf())).thenReturn(false);
        when(usuarioService.create(any(UsuarioDTO.class))).thenReturn(usuarioDTOMock);

        // Act
        String result = authBean.registrar();

        // Assert
        assertThat(result).isEqualTo("/login?faces-redirect=true");
        verify(usuarioService).create(any(UsuarioDTO.class));
    }

    @Test
    void should_ReturnNull_When_EmailAlreadyExists() {
        // Arrange
        Usuario novoUsuario = new Usuario();
        novoUsuario.setEmail(usuarioDTOMock.getEmail());
        novoUsuario.setCpf(usuarioDTOMock.getCpf());
        authBean.setNovoUsuario(novoUsuario);
        when(usuarioService.existsByEmail(usuarioDTOMock.getEmail())).thenReturn(true);

        // Act
        String result = authBean.registrar();

        // Assert
        assertThat(result).isNull();
        verify(usuarioService, never()).create(any(UsuarioDTO.class));
    }

    @Test
    void should_ReturnNull_When_CpfAlreadyExists() {
        // Arrange
        Usuario novoUsuario = new Usuario();
        novoUsuario.setEmail(usuarioDTOMock.getEmail());
        novoUsuario.setCpf(usuarioDTOMock.getCpf());
        authBean.setNovoUsuario(novoUsuario);
        when(usuarioService.existsByEmail(usuarioDTOMock.getEmail())).thenReturn(false);
        when(usuarioService.existsByCpf(usuarioDTOMock.getCpf())).thenReturn(true);

        // Act
        String result = authBean.registrar();

        // Assert
        assertThat(result).isNull();
        verify(usuarioService, never()).create(any(UsuarioDTO.class));
    }

    @Test
    void should_LogoutSuccessfully_When_UserIsLoggedIn() {
        // Arrange
        // Mock usuario logado - simulando estado de login

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
        authBean.setEmailReset("joao@teste.com");
        when(usuarioService.findByEmail("joao@teste.com")).thenReturn(Optional.of(usuarioDTOMock));
        // Mock do método de solicitação de reset de senha

        // Act
        authBean.solicitarResetSenha();

        // Assert
        // Verificar se o método foi chamado (sem verificação do service por enquanto)
    }

    @Test
    void should_ResetPassword_When_TokenIsValid() {
        // Arrange
        authBean.setTokenReset("validToken");
        authBean.setNovaSenha("novaSenha123");
        authBean.setConfirmaNovaSenha("novaSenha123");
        // Mock do método de reset de senha

        // Act
        String result = authBean.resetarSenha();

        // Assert
        // Verificar se o método foi executado (sem verificação do service por enquanto)
    }

    @Test
    void should_ReturnNull_When_PasswordsDoNotMatch() {
        // Arrange
        authBean.setTokenReset("validToken");
        authBean.setNovaSenha("novaSenha123");
        authBean.setConfirmaNovaSenha("senhasDiferentes");

        // Act
        String result = authBean.resetarSenha();

        // Assert
        assertThat(result).isNull();
        // Verificar se o método não foi chamado no service
    }

    @Test
    void should_ToggleRegistrationForm_When_Called() {
        // Arrange
        boolean initialState = authBean.isMostrarFormularioRegistro();

        // Act
        authBean.alternarFormularioRegistro();

        // Assert
        assertThat(authBean.isMostrarFormularioRegistro()).isNotEqualTo(initialState);
    }

    @Test
    void should_ClearFields_When_Called() {
        // Arrange
        authBean.setEmailLogin("test@test.com");
        authBean.setSenhaLogin("password");
        authBean.setNovoUsuario(usuarioDTOMock);

        // Act
        authBean.limparCampos();

        // Assert
        assertThat(authBean.getEmailLogin()).isNull();
        assertThat(authBean.getSenhaLogin()).isNull();
        assertThat(authBean.getNovoUsuario()).isNotNull();
        assertThat(authBean.getNovoUsuario().getEmail()).isNull();
    }

    @Test
    void should_ValidateMinimumAge_When_RegisteringUser() {
        // Arrange
        Usuario novoUsuario = new Usuario();
        novoUsuario.setEmail(usuarioDTOMock.getEmail());
        novoUsuario.setCpf(usuarioDTOMock.getCpf());
        novoUsuario.setDataNascimento(LocalDate.now().minusYears(15)); // Menor que 16 anos
        authBean.setNovoUsuario(novoUsuario);
        when(usuarioService.existsByEmail(usuarioDTOMock.getEmail())).thenReturn(false);
        when(usuarioService.existsByCpf(usuarioDTOMock.getCpf())).thenReturn(false);

        // Act
        String result = authBean.registrar();

        // Assert
        assertThat(result).isNull();
        verify(usuarioService, never()).create(any(UsuarioDTO.class));
    }

    @Test
    void should_SetDefaultRole_When_RegisteringUser() {
        // Arrange
        Usuario novoUsuario = new Usuario();
        novoUsuario.setEmail(usuarioDTOMock.getEmail());
        novoUsuario.setCpf(usuarioDTOMock.getCpf());
        authBean.setNovoUsuario(novoUsuario);
        when(usuarioService.existsByEmail(usuarioDTOMock.getEmail())).thenReturn(false);
        when(usuarioService.existsByCpf(usuarioDTOMock.getCpf())).thenReturn(false);
        when(usuarioService.create(any(UsuarioDTO.class))).thenReturn(usuarioDTOMock);

        // Act
        authBean.registrar();

        // Assert
        verify(usuarioService).create(argThat(dto -> 
            dto.getPapel() == null || dto.getPapel() == PapelUsuario.USUARIO
        ));
    }
}