package com.sistema.java.unit.service;

import com.sistema.java.model.dto.UsuarioDTO;
import com.sistema.java.model.entity.Usuario;
import com.sistema.java.model.enums.PapelUsuario;
import com.sistema.java.repository.UsuarioRepository;
import com.sistema.java.service.UsuarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Testes unitários para UsuarioService
 * 
 * @author Sistema Java
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Testes do UsuarioService")
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UsuarioService usuarioService;

    private Usuario usuario;
    private UsuarioDTO usuarioDTO;

    @BeforeEach
    void setUp() {
        usuario = new Usuario();
        usuario.setId(1L);
        usuario.setNome("João");
        usuario.setSobrenome("Silva");
        usuario.setEmail("joao@email.com");
        usuario.setCpf("12345678901");
        usuario.setSenha("senhaEncriptada");
        usuario.setPapel(PapelUsuario.USUARIO);
        usuario.setAtivo(true);
        usuario.setEmailVerificado(false);
        usuario.setDataCriacao(LocalDateTime.now());

        usuarioDTO = new UsuarioDTO();
        usuarioDTO.setNome("João");
        usuarioDTO.setSobrenome("Silva");
        usuarioDTO.setEmail("joao@email.com");
        usuarioDTO.setCpf("12345678901");
        usuarioDTO.setSenha("senha123");
    }

    @Test
    @DisplayName("Deve criar usuário com sucesso")
    void should_CreateUser_When_ValidData() {
        // Arrange
        when(usuarioRepository.existsByEmail(anyString())).thenReturn(false);
        when(usuarioRepository.existsByCpf(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("senhaEncriptada");
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuario);

        // Act
        UsuarioDTO resultado = usuarioService.create(usuarioDTO);

        // Assert
        assertThat(resultado).isNotNull();
        assertThat(resultado.getNome()).isEqualTo("João");
        assertThat(resultado.getEmail()).isEqualTo("joao@email.com");
        verify(usuarioRepository).save(any(Usuario.class));
    }

    @Test
    @DisplayName("Deve lançar exceção quando email já existe")
    void should_ThrowException_When_EmailAlreadyExists() {
        // Arrange
        when(usuarioRepository.existsByEmail(anyString())).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> usuarioService.create(usuarioDTO))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Email já está em uso");
    }

    @Test
    @DisplayName("Deve lançar exceção quando CPF já existe")
    void should_ThrowException_When_CpfAlreadyExists() {
        // Arrange
        when(usuarioRepository.existsByEmail(anyString())).thenReturn(false);
        when(usuarioRepository.existsByCpf(anyString())).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> usuarioService.criarUsuario(usuarioDTO))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("CPF já está em uso");
    }

    @Test
    @DisplayName("Deve buscar usuário por ID com sucesso")
    void should_FindUser_When_ValidId() {
        // Arrange
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));

        // Act
        Optional<UsuarioDTO> resultado = usuarioService.findById(1L);

        // Assert
        assertThat(resultado).isPresent();
        assertThat(resultado.get().getNome()).isEqualTo("João");
    }

    @Test
    @DisplayName("Deve retornar vazio quando usuário não existe")
    void should_ReturnEmpty_When_UserNotFound() {
        // Arrange
        when(usuarioRepository.findById(1L)).thenReturn(Optional.empty());

        // Act
        Optional<UsuarioDTO> resultado = usuarioService.findById(1L);

        // Assert
        assertThat(resultado).isEmpty();
    }

    @Test
    @DisplayName("Deve listar usuários com paginação")
    void should_ListUsers_When_ValidPageable() {
        // Arrange
        List<Usuario> usuarios = Arrays.asList(usuario);
        Page<Usuario> page = new PageImpl<>(usuarios, PageRequest.of(0, 10), 1);
        when(usuarioRepository.findAll(any(Pageable.class))).thenReturn(page);

        // Act
        Page<UsuarioDTO> resultado = usuarioService.findAll(PageRequest.of(0, 10));

        // Assert
        assertThat(resultado.getContent()).hasSize(1);
        assertThat(resultado.getContent().get(0).getNome()).isEqualTo("João");
    }

    @Test
    @DisplayName("Deve atualizar usuário com sucesso")
    void should_UpdateUser_When_ValidData() {
        // Arrange
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuario);

        usuarioDTO.setId(1L);
        usuarioDTO.setNome("João Atualizado");

        // Act
        UsuarioDTO resultado = usuarioService.update(1L, usuarioDTO);

        // Assert
        assertThat(resultado.getNome()).isEqualTo("João Atualizado");
        verify(usuarioRepository).save(any(Usuario.class));
    }

    @Test
    @DisplayName("Deve verificar se CPF existe")
    void should_CheckCpfExists_When_ValidFormat() {
        // Arrange
        when(usuarioRepository.existsByCpf("12345678901")).thenReturn(true);
        
        // Act
        boolean resultado = usuarioService.existsByCpf("12345678901");

        // Assert
        assertThat(resultado).isTrue();
    }

    @Test
    @DisplayName("Deve verificar se email existe")
    void should_CheckEmailExists_When_ValidFormat() {
        // Arrange
        when(usuarioRepository.existsByEmail("teste@email.com")).thenReturn(true);
        
        // Act
        boolean resultado = usuarioService.existsByEmail("teste@email.com");

        // Assert
        assertThat(resultado).isTrue();
    }

    @Test
    @DisplayName("Deve desativar usuário com sucesso")
    void should_DeactivateUser_When_ValidId() {
        // Arrange
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuario);

        // Act
        usuarioService.desativar(1L);

        // Assert
        verify(usuarioRepository).save(any(Usuario.class));
    }

    @Test
    @DisplayName("Deve contar usuários ativos")
    void should_CountActiveUsers_When_Called() {
        // Arrange
        when(usuarioRepository.countByAtivo(true)).thenReturn(5L);

        // Act
        long resultado = usuarioService.countAtivos();

        // Assert
        assertThat(resultado).isEqualTo(5L);
    }

    @Test
    @DisplayName("Deve contar usuários por papel")
    void should_CountUsersByRole_When_ValidRole() {
        // Arrange
        when(usuarioRepository.countByPapel(PapelUsuario.ADMINISTRADOR)).thenReturn(2L);

        // Act
        long resultado = usuarioService.countByPapel(PapelUsuario.ADMINISTRADOR);

        // Assert
        assertThat(resultado).isEqualTo(2L);
    }
}