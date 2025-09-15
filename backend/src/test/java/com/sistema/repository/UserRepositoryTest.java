package com.sistema.repository;

import com.sistema.model.UserModel;
import com.sistema.model.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("repository-test")
@DisplayName("UserRepository Tests")
class UserRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserModelRepository userRepository;

    private UserModel user1;
    private UserModel user2;
    private UserModel user3;

    @BeforeEach
    void setUp() {
        user1 = new UserModel("João", "Silva", "joao.silva@example.com", "senha123", UserRole.USUARIO);
        user1.setEmailVerificado(true);
        user1.setDataVerificacaoEmail(LocalDateTime.now());
        
        user2 = new UserModel("Maria", "Santos", "maria.santos@example.com", "senha456", UserRole.ASSOCIADO);
        user2.setEmailVerificado(false);
        
        user3 = new UserModel("Pedro", "Oliveira", "pedro.oliveira@example.com", "senha789", UserRole.COLABORADOR);
        user3.setEmailVerificado(false);
    }

    @Test
    @DisplayName("Deve encontrar usuário por email")
    void shouldFindUserByEmail() {
        // Given
        entityManager.persistAndFlush(user1);
        
        // When
        Optional<UserModel> found = userRepository.findByEmail("joao.silva@example.com");
        
        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getNome()).isEqualTo("João");
        assertThat(found.get().getEmail()).isEqualTo("joao.silva@example.com");
    }

    @Test
    @DisplayName("Deve retornar vazio quando usuário não existe por email")
    void shouldReturnEmptyWhenUserNotFoundByEmail() {
        // When
        Optional<UserModel> found = userRepository.findByEmail("inexistente@example.com");
        
        // Then
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("Deve verificar se email existe")
    void shouldCheckIfEmailExists() {
        // Given
        entityManager.persistAndFlush(user1);
        
        // When
        boolean exists = userRepository.existsByEmail("joao.silva@example.com");
        boolean notExists = userRepository.existsByEmail("inexistente@example.com");
        
        // Then
        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }

    @Test
    @DisplayName("Deve encontrar usuários por role")
    void shouldFindUsersByRole() {
        // Given
        entityManager.persistAndFlush(user1); // USUARIO
        entityManager.persistAndFlush(user2); // ASSOCIADO
        entityManager.persistAndFlush(user3); // COLABORADOR
        
        // When
        List<UserModel> usuarios = userRepository.findByRole(UserRole.USUARIO);
        List<UserModel> associados = userRepository.findByRole(UserRole.ASSOCIADO);
        
        // Then
        assertThat(usuarios).hasSize(1);
        assertThat(usuarios.get(0).getNome()).isEqualTo("João");
        assertThat(associados).hasSize(1);
        assertThat(associados.get(0).getNome()).isEqualTo("Maria");
    }

    @Test
    @DisplayName("Deve encontrar usuários com email verificado")
    void shouldFindUsersWithVerifiedEmail() {
        // Given
        entityManager.persistAndFlush(user1); // verificado
        entityManager.persistAndFlush(user2); // não verificado
        entityManager.persistAndFlush(user3); // não verificado
        
        // When
        List<UserModel> verificados = userRepository.findByEmailVerificado(true);
        List<UserModel> naoVerificados = userRepository.findByEmailVerificado(false);
        
        // Then
        assertThat(verificados).hasSize(1);
        assertThat(verificados.get(0).getNome()).isEqualTo("João");
        assertThat(naoVerificados).hasSize(2);
    }

    @Test
    @DisplayName("Deve encontrar usuários não verificados criados antes de uma data")
    void shouldFindUnverifiedUsersCreatedBefore() {
        // Given
        entityManager.persistAndFlush(user1); // verificado
        entityManager.persistAndFlush(user2); // não verificado, recente
        
        // Persistir user3 primeiro e depois atualizar a data diretamente no banco
        entityManager.persistAndFlush(user3);
        
        // Atualizar a data de criação diretamente no banco de dados
        LocalDateTime dataAntiga = LocalDateTime.now().minusHours(25);
        entityManager.getEntityManager().createQuery("UPDATE UserModel u SET u.dataCriacao = :dataAntiga WHERE u.id = :id")
                .setParameter("dataAntiga", dataAntiga)
                .setParameter("id", user3.getId())
                .executeUpdate();
        entityManager.flush();
        entityManager.clear();
        
        LocalDateTime cutoffDate = LocalDateTime.now().minusHours(24);
        
        // When
        List<UserModel> usuariosExpirados = userRepository.findByEmailVerificadoFalseAndDataCriacaoBefore(cutoffDate);
        
        // Then
        assertThat(usuariosExpirados).hasSize(1);
        assertThat(usuariosExpirados.get(0).getNome()).isEqualTo("Pedro");
    }

    @Test
    @DisplayName("Deve contar usuários por role")
    void shouldCountUsersByRole() {
        // Given
        entityManager.persistAndFlush(user1); // USUARIO
        entityManager.persistAndFlush(user2); // ASSOCIADO
        entityManager.persistAndFlush(user3); // COLABORADOR
        
        // When
        long countUsuarios = userRepository.countByRole(UserRole.USUARIO);
        long countAssociados = userRepository.countByRole(UserRole.ASSOCIADO);
        long countDiretores = userRepository.countByRole(UserRole.DIRETOR);
        
        // Then
        assertThat(countUsuarios).isEqualTo(1);
        assertThat(countAssociados).isEqualTo(1);
        assertThat(countDiretores).isEqualTo(0);
    }

    @Test
    @DisplayName("Deve encontrar usuários por nome contendo texto")
    void shouldFindUsersByNomeContaining() {
        // Given
        entityManager.persistAndFlush(user1); // João
        entityManager.persistAndFlush(user2); // Maria
        entityManager.persistAndFlush(user3); // Pedro
        
        // When
        List<UserModel> usuariosComJoao = userRepository.findByNomeContainingIgnoreCase("joão");
        List<UserModel> usuariosComA = userRepository.findByNomeContainingIgnoreCase("a");
        
        // Then
        assertThat(usuariosComJoao).hasSize(1);
        assertThat(usuariosComJoao.get(0).getNome()).isEqualTo("João");
        assertThat(usuariosComA).hasSize(1); // Maria
        assertThat(usuariosComA.get(0).getNome()).isEqualTo("Maria");
    }

    @Test
    @DisplayName("Deve encontrar usuários por email contendo texto")
    void shouldFindUsersByEmailContaining() {
        // Given
        entityManager.persistAndFlush(user1); // joao.silva@example.com
        entityManager.persistAndFlush(user2); // maria.santos@example.com
        entityManager.persistAndFlush(user3); // pedro.oliveira@example.com
        
        // When
        List<UserModel> usuariosComSilva = userRepository.findByEmailContainingIgnoreCase("silva");
        List<UserModel> usuariosComExample = userRepository.findByEmailContainingIgnoreCase("example");
        
        // Then
        assertThat(usuariosComSilva).hasSize(1);
        assertThat(usuariosComSilva.get(0).getEmail()).isEqualTo("joao.silva@example.com");
        assertThat(usuariosComExample).hasSize(3); // Todos têm @example.com
    }

    @Test
    @DisplayName("Deve deletar usuários não verificados criados antes de uma data")
    void shouldDeleteUnverifiedUsersCreatedBefore() {
        // Given
        entityManager.persistAndFlush(user1); // verificado
        entityManager.persistAndFlush(user2); // não verificado, recente
        
        // Persistir user3 primeiro e depois atualizar a data diretamente no banco
        entityManager.persistAndFlush(user3);
        
        // Atualizar a data de criação diretamente no banco de dados
        LocalDateTime dataAntiga = LocalDateTime.now().minusHours(25);
        entityManager.getEntityManager().createQuery("UPDATE UserModel u SET u.dataCriacao = :dataAntiga WHERE u.id = :id")
                .setParameter("dataAntiga", dataAntiga)
                .setParameter("id", user3.getId())
                .executeUpdate();
        entityManager.flush();
        entityManager.clear();
        
        LocalDateTime cutoffDate = LocalDateTime.now().minusHours(24);
        
        // When
        int deletedCount = userRepository.deleteByEmailVerificadoFalseAndDataCriacaoBefore(cutoffDate);
        entityManager.flush();
        
        // Then
        assertThat(deletedCount).isEqualTo(1);
        
        List<UserModel> remainingUsers = userRepository.findAll();
        assertThat(remainingUsers).hasSize(2);
        assertThat(remainingUsers).extracting(UserModel::getNome)
            .containsExactlyInAnyOrder("João", "Maria");
    }

    @Test
    @DisplayName("Deve salvar e recuperar usuário com todos os campos")
    void shouldSaveAndRetrieveUserWithAllFields() {
        // Given
        UserModel user = new UserModel("Ana", "Costa", "ana.costa@example.com", "senha123", UserRole.FUNDADOR);
        user.setEmailVerificado(true);
        user.setDataVerificacaoEmail(LocalDateTime.now());
        
        // When
        UserModel savedUser = userRepository.save(user);
        Optional<UserModel> retrievedUser = userRepository.findById(savedUser.getId());
        
        // Then
        assertThat(retrievedUser).isPresent();
        UserModel found = retrievedUser.get();
        assertThat(found.getNome()).isEqualTo("Ana");
        assertThat(found.getSobrenome()).isEqualTo("Costa");
        assertThat(found.getEmail()).isEqualTo("ana.costa@example.com");
        assertThat(found.getRole()).isEqualTo(UserRole.FUNDADOR);
        assertThat(found.isEmailVerificado()).isTrue();
        assertThat(found.getDataCriacao()).isNotNull();
        assertThat(found.getDataVerificacaoEmail()).isNotNull();
    }
}