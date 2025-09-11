package com.sistema.java.unit.service;

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
        Optional<Usuario> resultado = authService.autenticar("joao@teste.com", senhaPlana);

        // Assert
        assertThat(resultado).isPresent();
        assertThat(resultado.get().getEmail()).isEqualTo("joao@teste.com");
        assertThat(resultado.get().isAtivo()).isTrue();
        verify(usuarioRepository).findByEmail("joao@teste.com");
        verify(passwordEncoder).matches(senhaPlana, "senhaEncriptada");
    }

    @Test
    void should_ReturnEmpty_When_UserNotFound() {
        // Arrange
        when(usuarioRepository.findByEmail("inexistente@teste.com")).thenReturn(Optional.empty());

        // Act
        Optional<Usuario> resultado = authService.autenticar("inexistente@teste.com", "senha123");

        // Assert
        assertThat(resultado).isEmpty();
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
        Optional<Usuario> resultado = authService.autenticar("joao@teste.com", senhaIncorreta);

        // Assert
        assertThat(resultado).isEmpty();
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
        Optional<Usuario> resultado = authService.autenticar("joao@teste.com", senhaPlana);

        // Assert
        assertThat(resultado).isEmpty();
        verify(usuarioRepository).findByEmail("joao@teste.com");
        verify(passwordEncoder).matches(senhaPlana, "senhaEncriptada");
    }

    @Test
    void should_RegisterUser_When_ValidDataProvided() {
        // Arrange
        Usuario novoUsuario = new Usuario();
        novoUsuario.setNome("Maria");
        novoUsuario.setSobrenome("Santos");
        novoUsuario.setCpf("98765432100");
        novoUsuario.setEmail("maria@teste.com");
        novoUsuario.setSenha("senha123");
        
        when(usuarioRepository.existsByEmail("maria@teste.com")).thenReturn(false);
        when(usuarioRepository.existsByCpf("98765432100")).thenReturn(false);
        when(passwordEncoder.encode("senha123")).thenReturn("senhaEncriptada");
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(novoUsuario);

        // Act
        Usuario resultado = authService.registrar(novoUsuario);

        // Assert
        assertThat(resultado).isNotNull();
        assertThat(resultado.getPapel()).isEqualTo(PapelUsuario.USUARIO);
        assertThat(resultado.isAtivo()).isTrue();
        verify(usuarioRepository).existsByEmail("maria@teste.com");
        verify(usuarioRepository).existsByCpf("98765432100");
        verify(passwordEncoder).encode("senha123");
        verify(usuarioRepository).save(any(Usuario.class));
    }

    @Test
    void should_ThrowException_When_EmailAlreadyExists() {
        // Arrange
        Usuario novoUsuario = new Usuario();
        novoUsuario.setEmail("joao@teste.com");
        novoUsuario.setCpf("98765432100");
        
        when(usuarioRepository.existsByEmail("joao@teste.com")).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> authService.registrar(novoUsuario))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Email já está em uso");
        
        verify(usuarioRepository).existsByEmail("joao@teste.com");
        verify(usuarioRepository, never()).existsByCpf(anyString());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void should_ThrowException_When_CpfAlreadyExists() {
        // Arrange
        Usuario novoUsuario = new Usuario();
        novoUsuario.setEmail("maria@teste.com");
        novoUsuario.setCpf("12345678901");
        
        when(usuarioRepository.existsByEmail("maria@teste.com")).thenReturn(false);
        when(usuarioRepository.existsByCpf("12345678901")).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> authService.registrar(novoUsuario))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("CPF já está em uso");
        
        verify(usuarioRepository).existsByEmail("maria@teste.com");
        verify(usuarioRepository).existsByCpf("12345678901");
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void should_ChangePassword_When_ValidCurrentPasswordProvided() {
        // Arrange
        String senhaAtual = "senhaAtual";
        String novaSenha = "novaSenha123";
        String novaSenhaEncriptada = "novaSenhaEncriptada";
        
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches(senhaAtual, "senhaEncriptada")).thenReturn(true);
        when(passwordEncoder.encode(novaSenha)).thenReturn(novaSenhaEncriptada);
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuario);

        // Act
        Usuario resultado = authService.alterarSenha(1L, senhaAtual, novaSenha);

        // Assert
        assertThat(resultado).isNotNull();
        verify(usuarioRepository).findById(1L);
        verify(passwordEncoder).matches(senhaAtual, "senhaEncriptada");
        verify(passwordEncoder).encode(novaSenha);
        verify(usuarioRepository).save(usuario);
    }

    @Test
    void should_ThrowException_When_CurrentPasswordIsIncorrect() {
        // Arrange
        String senhaIncorreta = "senhaErrada";
        String novaSenha = "novaSenha123";
        
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches(senhaIncorreta, "senhaEncriptada")).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> authService.alterarSenha(1L, senhaIncorreta, novaSenha))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Senha atual incorreta");
        
        verify(usuarioRepository).findById(1L);
        verify(passwordEncoder).matches(senhaIncorreta, "senhaEncriptada");
        verify(passwordEncoder, never()).encode(anyString());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void should_ValidateUserRole_When_CheckingPermissions() {
        // Arrange
        usuario.setPapel(PapelUsuario.ADMINISTRADOR);

        // Act
        boolean isAdmin = authService.temPermissao(usuario, PapelUsuario.ADMINISTRADOR);
        boolean isColaborador = authService.temPermissao(usuario, PapelUsuario.COLABORADOR);
        boolean isUsuario = authService.temPermissao(usuario, PapelUsuario.USUARIO);

        // Assert
        assertThat(isAdmin).isTrue();
        assertThat(isColaborador).isTrue(); // Admin tem permissão de colaborador
        assertThat(isUsuario).isTrue(); // Admin tem permissão de usuário
    }

    @Test
    void should_ResetPassword_When_ValidEmailProvided() {
        // Arrange
        String novaSenhaTemporaria = "tempPassword123";
        String novaSenhaEncriptada = "tempPasswordEncriptada";
        
        when(usuarioRepository.findByEmail("joao@teste.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.encode(anyString())).thenReturn(novaSenhaEncriptada);
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuario);

        // Act
        String senhaTemporaria = authService.redefinirSenha("joao@teste.com");

        // Assert
        assertThat(senhaTemporaria).isNotNull();
        assertThat(senhaTemporaria).hasSize(8); // Senha temporária de 8 caracteres
        verify(usuarioRepository).findByEmail("joao@teste.com");
        verify(passwordEncoder).encode(anyString());
        verify(usuarioRepository).save(usuario);
    }

    @Test
    void should_ThrowException_When_ResettingPasswordForNonExistentUser() {
        // Arrange
        when(usuarioRepository.findByEmail("inexistente@teste.com")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> authService.redefinirSenha("inexistente@teste.com"))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Usuário não encontrado");
        
        verify(usuarioRepository).findByEmail("inexistente@teste.com");
        verify(passwordEncoder, never()).encode(anyString());
        verify(usuarioRepository, never()).save(any());
    }
}