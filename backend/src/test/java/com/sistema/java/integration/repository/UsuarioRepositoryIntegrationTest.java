package com.sistema.java.integration.repository;

import com.sistema.java.model.entity.Usuario;
import com.sistema.java.model.enums.PapelUsuario;
import com.sistema.java.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes de integração para UsuarioRepository
 * Referência: Testes de Integração - project_rules.md
 * Referência: TestContainers - project_rules.md
 * Referência: Banco de Dados Relacional - project_rules.md
 */
@DataJpaTest
@Testcontainers
@ActiveProfiles("test")
class UsuarioRepositoryIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("sistema_java_test")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UsuarioRepository usuarioRepository;

    private Usuario usuario1;
    private Usuario usuario2;
    private Usuario usuarioInativo;

    @BeforeEach
    void setUp() {
        // Arrange - Configuração dos dados de teste
        usuario1 = new Usuario();
        usuario1.setNome("João");
        usuario1.setSobrenome("Silva");
        usuario1.setCpf("12345678901");
        usuario1.setEmail("joao@teste.com");
        usuario1.setSenha("senha123");
        usuario1.setTelefone("11999999999");
        usuario1.setDataNascimento(LocalDate.of(1990, 1, 1));
        usuario1.setAtivo(true);
        usuario1.setPapel(PapelUsuario.USUARIO);

        usuario2 = new Usuario();
        usuario2.setNome("Maria");
        usuario2.setSobrenome("Santos");
        usuario2.setCpf("98765432109");
        usuario2.setEmail("maria@teste.com");
        usuario2.setSenha("senha456");
        usuario2.setTelefone("11888888888");
        usuario2.setDataNascimento(LocalDate.of(1985, 5, 15));
        usuario2.setAtivo(true);
        usuario2.setPapel(PapelUsuario.ADMINISTRADOR);

        usuarioInativo = new Usuario();
        usuarioInativo.setNome("Pedro");
        usuarioInativo.setSobrenome("Costa");
        usuarioInativo.setCpf("11122233344");
        usuarioInativo.setEmail("pedro@teste.com");
        usuarioInativo.setSenha("senha789");
        usuarioInativo.setAtivo(false);
        usuarioInativo.setPapel(PapelUsuario.USUARIO);

        entityManager.persistAndFlush(usuario1);
        entityManager.persistAndFlush(usuario2);
        entityManager.persistAndFlush(usuarioInativo);
    }

    @Test
    void should_FindUserByEmail_When_EmailExists() {
        // Act
        Optional<Usuario> resultado = usuarioRepository.findByEmail("joao@teste.com");

        // Assert
        assertThat(resultado).isPresent();
        assertThat(resultado.get().getNome()).isEqualTo("João");
        assertThat(resultado.get().getEmail()).isEqualTo("joao@teste.com");
    }

    @Test
    void should_ReturnEmpty_When_EmailDoesNotExist() {
        // Act
        Optional<Usuario> resultado = usuarioRepository.findByEmail("inexistente@teste.com");

        // Assert
        assertThat(resultado).isEmpty();
    }

    @Test
    void should_FindUserByCpf_When_CpfExists() {
        // Act
        Optional<Usuario> resultado = usuarioRepository.findByCpf("12345678901");

        // Assert
        assertThat(resultado).isPresent();
        assertThat(resultado.get().getNome()).isEqualTo("João");
        assertThat(resultado.get().getCpf()).isEqualTo("12345678901");
    }

    @Test
    void should_ReturnEmpty_When_CpfDoesNotExist() {
        // Act
        Optional<Usuario> resultado = usuarioRepository.findByCpf("00000000000");

        // Assert
        assertThat(resultado).isEmpty();
    }

    @Test
    void should_FindActiveUsers_When_FilteringByActiveStatus() {
        // Act
        List<Usuario> usuariosAtivos = usuarioRepository.findByAtivoTrue();

        // Assert
        assertThat(usuariosAtivos).hasSize(2);
        assertThat(usuariosAtivos)
            .extracting(Usuario::getNome)
            .containsExactlyInAnyOrder("João", "Maria");
    }

    @Test
    void should_FindInactiveUsers_When_FilteringByInactiveStatus() {
        // Act
        List<Usuario> usuariosInativos = usuarioRepository.findByAtivoFalse();

        // Assert
        assertThat(usuariosInativos).hasSize(1);
        assertThat(usuariosInativos.get(0).getNome()).isEqualTo("Pedro");
    }

    @Test
    void should_FindUsersByRole_When_FilteringByPapel() {
        // Act
        List<Usuario> administradores = usuarioRepository.findByPapel(PapelUsuario.ADMINISTRADOR);
        List<Usuario> usuarios = usuarioRepository.findByPapel(PapelUsuario.USUARIO);

        // Assert
        assertThat(administradores).hasSize(1);
        assertThat(administradores.get(0).getNome()).isEqualTo("Maria");
        
        assertThat(usuarios).hasSize(2);
        assertThat(usuarios)
            .extracting(Usuario::getNome)
            .containsExactlyInAnyOrder("João", "Pedro");
    }

    @Test
    void should_CheckEmailExists_When_EmailIsInDatabase() {
        // Act
        boolean emailExists = usuarioRepository.existsByEmail("joao@teste.com");
        boolean emailNotExists = usuarioRepository.existsByEmail("naoexiste@teste.com");

        // Assert
        assertThat(emailExists).isTrue();
        assertThat(emailNotExists).isFalse();
    }

    @Test
    void should_CheckCpfExists_When_CpfIsInDatabase() {
        // Act
        boolean cpfExists = usuarioRepository.existsByCpf("12345678901");
        boolean cpfNotExists = usuarioRepository.existsByCpf("00000000000");

        // Assert
        assertThat(cpfExists).isTrue();
        assertThat(cpfNotExists).isFalse();
    }

    @Test
    void should_FindUsersByNameContaining_When_SearchingByPartialName() {
        // Act
        List<Usuario> resultados = usuarioRepository.findByNomeContainingIgnoreCase("jo");

        // Assert
        assertThat(resultados).hasSize(1);
        assertThat(resultados.get(0).getNome()).isEqualTo("João");
    }

    @Test
    void should_FindUsersByEmailContaining_When_SearchingByPartialEmail() {
        // Act
        List<Usuario> resultados = usuarioRepository.findByEmailContainingIgnoreCase("teste");

        // Assert
        assertThat(resultados).hasSize(3);
        assertThat(resultados)
            .extracting(Usuario::getEmail)
            .allMatch(email -> email.contains("teste"));
    }

    @Test
    void should_CountActiveUsers_When_CountingByStatus() {
        // Act
        long countAtivos = usuarioRepository.countByAtivoTrue();
        long countInativos = usuarioRepository.countByAtivoFalse();

        // Assert
        assertThat(countAtivos).isEqualTo(2);
        assertThat(countInativos).isEqualTo(1);
    }

    @Test
    void should_CountUsersByRole_When_CountingByPapel() {
        // Act
        long countAdministradores = usuarioRepository.countByPapel(PapelUsuario.ADMINISTRADOR);
        long countUsuarios = usuarioRepository.countByPapel(PapelUsuario.USUARIO);

        // Assert
        assertThat(countAdministradores).isEqualTo(1);
        assertThat(countUsuarios).isEqualTo(2);
    }

    @Test
    void should_FindUsersByDateRange_When_FilteringByBirthDate() {
        // Arrange
        LocalDate dataInicio = LocalDate.of(1980, 1, 1);
        LocalDate dataFim = LocalDate.of(1990, 12, 31);

        // Act
        List<Usuario> resultados = usuarioRepository.findByDataNascimentoBetween(dataInicio, dataFim);

        // Assert
        assertThat(resultados).hasSize(2);
        assertThat(resultados)
            .extracting(Usuario::getNome)
            .containsExactlyInAnyOrder("João", "Maria");
    }

    @Test
    void should_FindUsersCreatedAfter_When_FilteringByCreationDate() {
        // Arrange
        LocalDate ontem = LocalDate.now().minusDays(1);

        // Act
        List<Usuario> resultados = usuarioRepository.findByDataCriacaoAfter(ontem.atStartOfDay());

        // Assert
        assertThat(resultados).hasSize(3); // Todos os usuários foram criados hoje
    }

    @Test
    void should_UpdateUserStatus_When_ChangingActiveFlag() {
        // Arrange
        Usuario usuario = usuarioRepository.findByEmail("joao@teste.com").orElseThrow();
        
        // Act
        usuario.setAtivo(false);
        Usuario usuarioAtualizado = usuarioRepository.save(usuario);
        entityManager.flush();
        entityManager.clear();

        // Assert
        Usuario usuarioVerificado = usuarioRepository.findById(usuarioAtualizado.getId()).orElseThrow();
        assertThat(usuarioVerificado.isAtivo()).isFalse();
    }

    @Test
    void should_DeleteUser_When_UserExists() {
        // Arrange
        Usuario usuario = usuarioRepository.findByEmail("joao@teste.com").orElseThrow();
        Long usuarioId = usuario.getId();

        // Act
        usuarioRepository.delete(usuario);
        entityManager.flush();

        // Assert
        Optional<Usuario> usuarioDeletado = usuarioRepository.findById(usuarioId);
        assertThat(usuarioDeletado).isEmpty();
    }

    @Test
    void should_FindUsersWithAvatar_When_FilteringByAvatarPresence() {
        // Arrange
        usuario1.setAvatar("avatar1.jpg");
        entityManager.merge(usuario1);
        entityManager.flush();

        // Act
        List<Usuario> usuariosComAvatar = usuarioRepository.findByAvatarIsNotNull();
        List<Usuario> usuariosSemAvatar = usuarioRepository.findByAvatarIsNull();

        // Assert
        assertThat(usuariosComAvatar).hasSize(1);
        assertThat(usuariosComAvatar.get(0).getNome()).isEqualTo("João");
        
        assertThat(usuariosSemAvatar).hasSize(2);
    }
}