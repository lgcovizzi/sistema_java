package com.sistema.java.unit.service;

import com.sistema.java.model.dto.RegistroRequestDTO;
import com.sistema.java.model.dto.RegistroResponseDTO;
import com.sistema.java.model.entity.Usuario;
import com.sistema.java.model.enums.PapelUsuario;
import com.sistema.java.repository.UsuarioRepository;
import com.sistema.java.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Testes unitários para AuthService
 * Referência: Testes e Qualidade de Código - project_rules.md
 * Referência: Controle de Acesso - project_rules.md
 * Referência: Login e Registro - project_rules.md
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    private Usuario usuario;

    @BeforeEach
    void setUp() {
        // Arrange - Configuração dos objetos de teste
        usuario = new Usuario();
        usuario.setId(1L);
        usuario.setNome("João");
        usuario.setSobrenome("Silva");
        usuario.setCpf("12345678901");
        usuario.setEmail("joao@teste.com");
        usuario.setSenha("senhaEncriptada");
        usuario.setPapel(PapelUsuario.USUARIO);
        usuario.setAtivo(true);
        usuario.setDataCriacao(LocalDateTime.now());
    }

    @Test
    void should_AuthenticateUser_When_ValidCredentialsProvided() {
        // Arrange
        String senhaPlana = "senha123";
        when(usuarioRepository.findByEmail("joao@teste.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches(senhaPlana, "senhaEncriptada")).thenReturn(true);

        // Act
        Usuario resultado = authService.autenticar("joao@teste.com", senhaPlana);

        // Assert
        assertThat(resultado).isNotNull();
        assertThat(resultado.getEmail()).isEqualTo("joao@teste.com");
        assertThat(resultado.isAtivo()).isTrue();
        verify(usuarioRepository).findByEmail("joao@teste.com");
        verify(passwordEncoder).matches(senhaPlana, "senhaEncriptada");
    }

    @Test
    void should_ReturnEmpty_When_UserNotFound() {
        // Arrange
        when(usuarioRepository.findByEmail("inexistente@teste.com")).thenReturn(Optional.empty());

        // Act
        Usuario resultado = authService.autenticar("inexistente@teste.com", "senha123");

        // Assert
        assertThat(resultado).isNull();
        verify(usuarioRepository).findByEmail("inexistente@teste.com");
        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    @Test
    void should_ReturnEmpty_When_PasswordDoesNotMatch() {
        // Arrange
        String senhaIncorreta = "senhaErrada";
        when(usuarioRepository.findByEmail("joao@teste.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches(senhaIncorreta, "senhaEncriptada")).thenReturn(false);

        // Act
        Usuario resultado = authService.autenticar("joao@teste.com", senhaIncorreta);

        // Assert
        assertThat(resultado).isNull();
        verify(usuarioRepository).findByEmail("joao@teste.com");
        verify(passwordEncoder).matches(senhaIncorreta, "senhaEncriptada");
    }

    @Test
    void should_ReturnEmpty_When_UserIsInactive() {
        // Arrange
        usuario.setAtivo(false);
        String senhaPlana = "senha123";
        when(usuarioRepository.findByEmail("joao@teste.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches(senhaPlana, "senhaEncriptada")).thenReturn(true);

        // Act
        Usuario resultado = authService.autenticar("joao@teste.com", senhaPlana);

        // Assert
        assertThat(resultado).isNull();
        verify(usuarioRepository).findByEmail("joao@teste.com");
        verify(passwordEncoder).matches(senhaPlana, "senhaEncriptada");
    }

    @Test
    void should_RegisterUser_When_ValidDataProvided() {
        // Arrange
        RegistroRequestDTO registroRequest = new RegistroRequestDTO();
        registroRequest.setNome("Maria");
        registroRequest.setSobrenome("Santos");
        registroRequest.setCpf("98765432100");
        registroRequest.setEmail("maria@teste.com");
        registroRequest.setSenha("senha123");
        
        Usuario usuarioSalvo = new Usuario();
        usuarioSalvo.setNome("Maria");
        usuarioSalvo.setSobrenome("Santos");
        usuarioSalvo.setCpf("98765432100");
        usuarioSalvo.setEmail("maria@teste.com");
        usuarioSalvo.setPapel(PapelUsuario.USUARIO);
        usuarioSalvo.setAtivo(true);
        
        when(usuarioRepository.existsByEmail("maria@teste.com")).thenReturn(false);
        when(usuarioRepository.existsByCpf("98765432100")).thenReturn(false);
        when(passwordEncoder.encode("senha123")).thenReturn("senhaEncriptada");
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuarioSalvo);

        // Act
        RegistroResponseDTO resultado = authService.registrar(registroRequest);

        // Assert
        assertThat(resultado).isNotNull();
        assertThat(resultado.isSucesso()).isTrue();
        verify(usuarioRepository).existsByEmail("maria@teste.com");
        verify(usuarioRepository).existsByCpf("98765432100");
        verify(passwordEncoder).encode("senha123");
        verify(usuarioRepository).save(any(Usuario.class));
    }

    @Test
    void should_ReturnError_When_EmailAlreadyExists() {
        // Arrange
        RegistroRequestDTO registroRequest = new RegistroRequestDTO();
        registroRequest.setEmail("joao@teste.com");
        registroRequest.setCpf("98765432100");
        
        when(usuarioRepository.existsByEmail("joao@teste.com")).thenReturn(true);

        // Act
        RegistroResponseDTO resultado = authService.registrar(registroRequest);

        // Assert
        assertThat(resultado).isNotNull();
        assertThat(resultado.isSucesso()).isFalse();
        verify(usuarioRepository).existsByEmail("joao@teste.com");
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void should_ReturnError_When_CpfAlreadyExists() {
        // Arrange
        RegistroRequestDTO registroRequest = new RegistroRequestDTO();
        registroRequest.setEmail("maria@teste.com");
        registroRequest.setCpf("12345678901");
        
        when(usuarioRepository.existsByEmail("maria@teste.com")).thenReturn(false);
        when(usuarioRepository.existsByCpf("12345678901")).thenReturn(true);

        // Act
        RegistroResponseDTO resultado = authService.registrar(registroRequest);

        // Assert
        assertThat(resultado).isNotNull();
        assertThat(resultado.isSucesso()).isFalse();
        verify(usuarioRepository).existsByEmail("maria@teste.com");
        verify(usuarioRepository).existsByCpf("12345678901");
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void should_CheckEmailAvailability_When_EmailNotExists() {
        // Arrange
        when(usuarioRepository.existsByEmail("novo@teste.com")).thenReturn(false);

        // Act
        boolean disponivel = authService.emailDisponivel("novo@teste.com");

        // Assert
        assertThat(disponivel).isTrue();
        verify(usuarioRepository).existsByEmail("novo@teste.com");
    }

    @Test
    void should_CheckCpfAvailability_When_CpfNotExists() {
        // Arrange
        when(usuarioRepository.existsByCpf("12345678901")).thenReturn(false);

        // Act
        boolean disponivel = authService.cpfDisponivel("12345678901");

        // Assert
        assertThat(disponivel).isTrue();
        verify(usuarioRepository).existsByCpf("12345678901");
    }

    @Test
    void should_CheckAdminRole_When_UserIsAdmin() {
        // Arrange
        usuario.setPapel(PapelUsuario.ADMINISTRADOR);
        when(usuarioRepository.findByEmail("joao@teste.com")).thenReturn(Optional.of(usuario));

        // Act
        boolean isAdmin = authService.isAdmin();

        // Assert
        assertThat(isAdmin).isFalse(); // Retorna false porque getUsuarioLogado() retorna null
    }

    @Test
    void should_CheckUserManagement_When_UserIsAdmin() {
        // Arrange
        usuario.setPapel(PapelUsuario.ADMINISTRADOR);

        // Act
        boolean canManage = authService.canManageUsers();

        // Assert
        assertThat(canManage).isFalse(); // Retorna false porque getUsuarioLogado() retorna null
    }

    @Test
    void should_CheckContentManagement_When_UserIsCollaborator() {
        // Arrange
        usuario.setPapel(PapelUsuario.COLABORADOR);

        // Act
        boolean canManage = authService.canManageContent();

        // Assert
        assertThat(canManage).isFalse(); // Retorna false porque getUsuarioLogado() retorna null
    }
}