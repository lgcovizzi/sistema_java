package com.sistema.java.integration.repository;

import com.sistema.java.model.entity.Categoria;
import com.sistema.java.model.entity.Comentario;
import com.sistema.java.model.entity.Noticia;
import com.sistema.java.model.entity.Usuario;
import com.sistema.java.model.enums.PapelUsuario;
import com.sistema.java.repository.ComentarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes de integração para ComentarioRepository
 * Referência: Testes de Integração - project_rules.md
 * Referência: TestContainers - project_rules.md
 * Referência: Banco de Dados Relacional - project_rules.md
 */
@DataJpaTest
@Testcontainers
@ActiveProfiles("test")
class ComentarioRepositoryIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("sistema_java_test")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ComentarioRepository comentarioRepository;

    private Usuario autor1;
    private Usuario autor2;
    private Categoria categoria;
    private Noticia noticia1;
    private Noticia noticia2;
    private Comentario comentarioAprovado1;
    private Comentario comentarioAprovado2;
    private Comentario comentarioPendente;
    private Comentario comentarioRejeitado;

    @BeforeEach
    void setUp() {
        // Arrange - Configuração dos dados de teste
        autor1 = new Usuario();
        autor1.setNome("João");
        autor1.setSobrenome("Silva");
        autor1.setCpf("12345678901");
        autor1.setEmail("joao@teste.com");
        autor1.setSenha("senha123");
        autor1.setAtivo(true);
        autor1.setPapel(PapelUsuario.USUARIO);

        autor2 = new Usuario();
        autor2.setNome("Maria");
        autor2.setSobrenome("Santos");
        autor2.setCpf("98765432109");
        autor2.setEmail("maria@teste.com");
        autor2.setSenha("senha456");
        autor2.setAtivo(true);
        autor2.setPapel(PapelUsuario.COLABORADOR);

        categoria = new Categoria();
        categoria.setNome("Tecnologia");
        categoria.setDescricao("Notícias sobre tecnologia");
        categoria.setAtiva(true);

        entityManager.persistAndFlush(autor1);
        entityManager.persistAndFlush(autor2);
        entityManager.persistAndFlush(categoria);

        noticia1 = new Noticia();
        noticia1.setTitulo("Primeira Notícia");
        noticia1.setConteudo("Conteúdo da primeira notícia");
        noticia1.setResumo("Resumo da primeira notícia");
        noticia1.setAutor(autor2);
        noticia1.setPublicada(true);
        noticia1.setDataPublicacao(LocalDateTime.now().minusDays(1));
        noticia1.setCategorias(Set.of(categoria));

        noticia2 = new Noticia();
        noticia2.setTitulo("Segunda Notícia");
        noticia2.setConteudo("Conteúdo da segunda notícia");
        noticia2.setResumo("Resumo da segunda notícia");
        noticia2.setAutor(autor2);
        noticia2.setPublicada(true);
        noticia2.setDataPublicacao(LocalDateTime.now().minusHours(12));
        noticia2.setCategorias(Set.of(categoria));

        entityManager.persistAndFlush(noticia1);
        entityManager.persistAndFlush(noticia2);

        comentarioAprovado1 = new Comentario();
        comentarioAprovado1.setConteudo("Excelente artigo! Muito informativo.");
        comentarioAprovado1.setAutor(autor1);
        comentarioAprovado1.setNoticia(noticia1);
        comentarioAprovado1.setAprovado(true);

        comentarioAprovado2 = new Comentario();
        comentarioAprovado2.setConteudo("Concordo plenamente com o autor.");
        comentarioAprovado2.setAutor(autor1);
        comentarioAprovado2.setNoticia(noticia1);
        comentarioAprovado2.setAprovado(true);

        comentarioPendente = new Comentario();
        comentarioPendente.setConteudo("Comentário aguardando aprovação.");
        comentarioPendente.setAutor(autor1);
        comentarioPendente.setNoticia(noticia2);
        comentarioPendente.setAprovado(false);

        comentarioRejeitado = new Comentario();
        comentarioRejeitado.setConteudo("Comentário rejeitado por moderação.");
        comentarioRejeitado.setAutor(autor1);
        comentarioRejeitado.setNoticia(noticia2);
        comentarioRejeitado.setAprovado(false);

        entityManager.persistAndFlush(comentarioAprovado1);
        entityManager.persistAndFlush(comentarioAprovado2);
        entityManager.persistAndFlush(comentarioPendente);
        entityManager.persistAndFlush(comentarioRejeitado);
    }

    @Test
    void should_FindApprovedComments_When_FilteringByApprovedStatus() {
        // Act
        List<Comentario> comentariosAprovados = comentarioRepository.findByAprovadoTrue();

        // Assert
        assertThat(comentariosAprovados).hasSize(2);
        assertThat(comentariosAprovados)
            .extracting(Comentario::getConteudo)
            .containsExactlyInAnyOrder(
                "Excelente artigo! Muito informativo.",
                "Concordo plenamente com o autor."
            );
    }

    @Test
    void should_FindPendingComments_When_FilteringByPendingStatus() {
        // Act
        List<Comentario> comentariosPendentes = comentarioRepository.findByAprovadoFalse();

        // Assert
        assertThat(comentariosPendentes).hasSize(2);
        assertThat(comentariosPendentes)
            .extracting(Comentario::getConteudo)
            .containsExactlyInAnyOrder(
                "Comentário aguardando aprovação.",
                "Comentário rejeitado por moderação."
            );
    }

    @Test
    void should_FindCommentsByNews_When_FilteringByNoticia() {
        // Act
        List<Comentario> comentariosNoticia1 = comentarioRepository.findByNoticia(noticia1);
        List<Comentario> comentariosNoticia2 = comentarioRepository.findByNoticia(noticia2);

        // Assert
        assertThat(comentariosNoticia1).hasSize(2);
        assertThat(comentariosNoticia1)
            .extracting(Comentario::getConteudo)
            .containsExactlyInAnyOrder(
                "Excelente artigo! Muito informativo.",
                "Concordo plenamente com o autor."
            );

        assertThat(comentariosNoticia2).hasSize(2);
        assertThat(comentariosNoticia2)
            .extracting(Comentario::getConteudo)
            .containsExactlyInAnyOrder(
                "Comentário aguardando aprovação.",
                "Comentário rejeitado por moderação."
            );
    }

    @Test
    void should_FindCommentsByAuthor_When_FilteringByAutor() {
        // Act
        List<Comentario> comentariosPorAutor1 = comentarioRepository.findByAutor(autor1);

        // Assert
        assertThat(comentariosPorAutor1).hasSize(4); // Todos os comentários são do autor1
        assertThat(comentariosPorAutor1)
            .allMatch(comentario -> comentario.getAutor().equals(autor1));
    }

    @Test
    void should_FindApprovedCommentsByNews_When_FilteringByNewsAndStatus() {
        // Act
        List<Comentario> comentariosAprovadosNoticia1 = comentarioRepository.findByNoticiaAndAprovadoTrue(noticia1);
        List<Comentario> comentariosAprovadosNoticia2 = comentarioRepository.findByNoticiaAndAprovadoTrue(noticia2);

        // Assert
        assertThat(comentariosAprovadosNoticia1).hasSize(2);
        assertThat(comentariosAprovadosNoticia2).hasSize(0);
    }

    @Test
    void should_FindCommentsByContentContaining_When_SearchingByPartialContent() {
        // Act
        List<Comentario> resultados = comentarioRepository.findByConteudoContainingIgnoreCase("excelente");

        // Assert
        assertThat(resultados).hasSize(1);
        assertThat(resultados.get(0).getConteudo()).isEqualTo("Excelente artigo! Muito informativo.");
    }

    @Test
    void should_CountCommentsByNews_When_CountingByNoticia() {
        // Act
        long countNoticia1 = comentarioRepository.countByNoticia(noticia1);
        long countNoticia2 = comentarioRepository.countByNoticia(noticia2);

        // Assert
        assertThat(countNoticia1).isEqualTo(2);
        assertThat(countNoticia2).isEqualTo(2);
    }

    @Test
    void should_CountApprovedCommentsByNews_When_CountingByNewsAndStatus() {
        // Act
        long countAprovadosNoticia1 = comentarioRepository.countByNoticiaAndAprovadoTrue(noticia1);
        long countAprovadosNoticia2 = comentarioRepository.countByNoticiaAndAprovadoTrue(noticia2);

        // Assert
        assertThat(countAprovadosNoticia1).isEqualTo(2);
        assertThat(countAprovadosNoticia2).isEqualTo(0);
    }

    @Test
    void should_CountCommentsByAuthor_When_CountingByAutor() {
        // Act
        long countAutor1 = comentarioRepository.countByAutor(autor1);

        // Assert
        assertThat(countAutor1).isEqualTo(4);
    }

    @Test
    void should_FindCommentsWithPagination_When_UsingPageable() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 2);

        // Act
        Page<Comentario> paginaComentarios = comentarioRepository.findByAprovadoTrue(pageable);

        // Assert
        assertThat(paginaComentarios.getContent()).hasSize(2);
        assertThat(paginaComentarios.getTotalElements()).isEqualTo(2);
        assertThat(paginaComentarios.getTotalPages()).isEqualTo(1);
    }

    @Test
    void should_FindCommentsOrderedByDate_When_OrderingByCreationDate() {
        // Act
        List<Comentario> comentariosOrdenados = comentarioRepository.findByNoticiaOrderByDataCriacaoDesc(noticia1);

        // Assert
        assertThat(comentariosOrdenados).hasSize(2);
        // Verificar se estão ordenados por data (mais recente primeiro)
        assertThat(comentariosOrdenados.get(0).getDataCriacao())
            .isAfterOrEqualTo(comentariosOrdenados.get(1).getDataCriacao());
    }

    @Test
    void should_FindRecentComments_When_FilteringByRecentDate() {
        // Arrange
        LocalDateTime ontem = LocalDateTime.now().minusDays(1);

        // Act
        List<Comentario> comentariosRecentes = comentarioRepository.findByDataCriacaoAfter(ontem);

        // Assert
        assertThat(comentariosRecentes).hasSize(4); // Todos foram criados hoje
    }

    @Test
    void should_FindCommentsByDateRange_When_FilteringByDateRange() {
        // Arrange
        LocalDateTime dataInicio = LocalDateTime.now().minusDays(1);
        LocalDateTime dataFim = LocalDateTime.now().plusDays(1);

        // Act
        List<Comentario> comentarios = comentarioRepository.findByDataCriacaoBetween(dataInicio, dataFim);

        // Assert
        assertThat(comentarios).hasSize(4);
    }

    @Test
    void should_UpdateCommentStatus_When_ChangingApprovedFlag() {
        // Arrange
        Comentario comentario = comentarioRepository.findByConteudoContainingIgnoreCase("aguardando").get(0);
        
        // Act
        comentario.setAprovado(true);
        Comentario comentarioAtualizado = comentarioRepository.save(comentario);
        entityManager.flush();
        entityManager.clear();

        // Assert
        Comentario comentarioVerificado = comentarioRepository.findById(comentarioAtualizado.getId()).orElseThrow();
        assertThat(comentarioVerificado.isAprovado()).isTrue();
    }

    @Test
    void should_UpdateCommentContent_When_ChangingContent() {
        // Arrange
        Comentario comentario = comentarioRepository.findByConteudoContainingIgnoreCase("excelente").get(0);
        String novoConteudo = "Comentário editado pelo usuário";
        
        // Act
        comentario.setConteudo(novoConteudo);
        Comentario comentarioAtualizado = comentarioRepository.save(comentario);
        entityManager.flush();
        entityManager.clear();

        // Assert
        Comentario comentarioVerificado = comentarioRepository.findById(comentarioAtualizado.getId()).orElseThrow();
        assertThat(comentarioVerificado.getConteudo()).isEqualTo(novoConteudo);
    }

    @Test
    void should_DeleteComment_When_CommentExists() {
        // Arrange
        Comentario comentario = comentarioRepository.findByConteudoContainingIgnoreCase("rejeitado").get(0);
        Long comentarioId = comentario.getId();

        // Act
        comentarioRepository.delete(comentario);
        entityManager.flush();

        // Assert
        Optional<Comentario> comentarioDeletado = comentarioRepository.findById(comentarioId);
        assertThat(comentarioDeletado).isEmpty();
    }

    @Test
    void should_FindCommentsByAuthorAndNews_When_FilteringByMultipleCriteria() {
        // Act
        List<Comentario> comentarios = comentarioRepository.findByAutorAndNoticia(autor1, noticia1);

        // Assert
        assertThat(comentarios).hasSize(2);
        assertThat(comentarios)
            .allMatch(comentario -> comentario.getAutor().equals(autor1) && 
                                   comentario.getNoticia().equals(noticia1));
    }

    @Test
    void should_FindCommentsByAuthorAndStatus_When_FilteringByAuthorAndApproval() {
        // Act
        List<Comentario> comentariosAprovados = comentarioRepository.findByAutorAndAprovadoTrue(autor1);
        List<Comentario> comentariosPendentes = comentarioRepository.findByAutorAndAprovadoFalse(autor1);

        // Assert
        assertThat(comentariosAprovados).hasSize(2);
        assertThat(comentariosPendentes).hasSize(2);
    }

    @Test
    void should_FindMostRecentComments_When_LimitingResults() {
        // Act
        List<Comentario> comentariosRecentes = comentarioRepository.findTop10ByOrderByDataCriacaoDesc();

        // Assert
        assertThat(comentariosRecentes).hasSize(4); // Temos apenas 4 comentários
        // Verificar se estão ordenados por data (mais recente primeiro)
        for (int i = 0; i < comentariosRecentes.size() - 1; i++) {
            assertThat(comentariosRecentes.get(i).getDataCriacao())
                .isAfterOrEqualTo(comentariosRecentes.get(i + 1).getDataCriacao());
        }
    }

    @Test
    void should_FindCommentsWithAuthorInfo_When_EagerLoadingAuthor() {
        // Act
        List<Comentario> comentarios = comentarioRepository.findAllWithAutor();

        // Assert
        assertThat(comentarios).hasSize(4);
        comentarios.forEach(comentario -> {
            assertThat(comentario.getAutor()).isNotNull();
            assertThat(comentario.getAutor().getNome()).isNotNull();
        });
    }

    @Test
    void should_FindCommentsWithNewsInfo_When_EagerLoadingNews() {
        // Act
        List<Comentario> comentarios = comentarioRepository.findAllWithNoticia();

        // Assert
        assertThat(comentarios).hasSize(4);
        comentarios.forEach(comentario -> {
            assertThat(comentario.getNoticia()).isNotNull();
            assertThat(comentario.getNoticia().getTitulo()).isNotNull();
        });
    }

    @Test
    void should_SearchCommentsFullText_When_SearchingByContent() {
        // Act
        List<Comentario> resultados = comentarioRepository.searchByConteudo("artigo");

        // Assert
        assertThat(resultados).hasSize(1);
        assertThat(resultados.get(0).getConteudo()).contains("artigo");
    }

    @Test
    void should_FindCommentsRequiringModeration_When_FilteringPendingComments() {
        // Act
        List<Comentario> comentariosParaModeração = comentarioRepository.findCommentsRequiringModeration();

        // Assert
        assertThat(comentariosParaModeração).hasSize(2); // Comentários não aprovados
        assertThat(comentariosParaModeração)
            .allMatch(comentario -> !comentario.isAprovado());
    }
}