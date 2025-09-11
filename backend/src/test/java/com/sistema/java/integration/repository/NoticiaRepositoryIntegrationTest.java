package com.sistema.java.integration.repository;

import com.sistema.java.model.entity.Categoria;
import com.sistema.java.model.entity.Noticia;
import com.sistema.java.model.entity.Usuario;
import com.sistema.java.model.enums.PapelUsuario;
import com.sistema.java.repository.NoticiaRepository;
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
 * Testes de integração para NoticiaRepository
 * Referência: Testes de Integração - project_rules.md
 * Referência: TestContainers - project_rules.md
 * Referência: Banco de Dados Relacional - project_rules.md
 */
@DataJpaTest
@Testcontainers
@ActiveProfiles("test")
class NoticiaRepositoryIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("sistema_java_test")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private NoticiaRepository noticiaRepository;

    private Usuario autor1;
    private Usuario autor2;
    private Categoria categoria1;
    private Categoria categoria2;
    private Noticia noticiaPublicada1;
    private Noticia noticiaPublicada2;
    private Noticia noticiaNaoPublicada;

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
        autor1.setPapel(PapelUsuario.COLABORADOR);

        autor2 = new Usuario();
        autor2.setNome("Maria");
        autor2.setSobrenome("Santos");
        autor2.setCpf("98765432109");
        autor2.setEmail("maria@teste.com");
        autor2.setSenha("senha456");
        autor2.setAtivo(true);
        autor2.setPapel(PapelUsuario.ADMINISTRADOR);

        categoria1 = new Categoria();
        categoria1.setNome("Tecnologia");
        categoria1.setDescricao("Notícias sobre tecnologia");
        categoria1.setAtiva(true);

        categoria2 = new Categoria();
        categoria2.setNome("Esportes");
        categoria2.setDescricao("Notícias sobre esportes");
        categoria2.setAtiva(true);

        entityManager.persistAndFlush(autor1);
        entityManager.persistAndFlush(autor2);
        entityManager.persistAndFlush(categoria1);
        entityManager.persistAndFlush(categoria2);

        noticiaPublicada1 = new Noticia();
        noticiaPublicada1.setTitulo("Primeira Notícia Publicada");
        noticiaPublicada1.setConteudo("Conteúdo da primeira notícia publicada");
        noticiaPublicada1.setResumo("Resumo da primeira notícia");
        noticiaPublicada1.setAutor(autor1);
        noticiaPublicada1.setPublicada(true);
        noticiaPublicada1.setDataPublicacao(LocalDateTime.now().minusDays(1));
        noticiaPublicada1.setCategorias(Set.of(categoria1));

        noticiaPublicada2 = new Noticia();
        noticiaPublicada2.setTitulo("Segunda Notícia Publicada");
        noticiaPublicada2.setConteudo("Conteúdo da segunda notícia publicada");
        noticiaPublicada2.setResumo("Resumo da segunda notícia");
        noticiaPublicada2.setAutor(autor2);
        noticiaPublicada2.setPublicada(true);
        noticiaPublicada2.setDataPublicacao(LocalDateTime.now().minusHours(12));
        noticiaPublicada2.setCategorias(Set.of(categoria1, categoria2));

        noticiaNaoPublicada = new Noticia();
        noticiaNaoPublicada.setTitulo("Notícia Não Publicada");
        noticiaNaoPublicada.setConteudo("Conteúdo da notícia não publicada");
        noticiaNaoPublicada.setResumo("Resumo da notícia não publicada");
        noticiaNaoPublicada.setAutor(autor1);
        noticiaNaoPublicada.setPublicada(false);
        noticiaNaoPublicada.setCategorias(Set.of(categoria2));

        entityManager.persistAndFlush(noticiaPublicada1);
        entityManager.persistAndFlush(noticiaPublicada2);
        entityManager.persistAndFlush(noticiaNaoPublicada);
    }

    @Test
    void should_FindPublishedNews_When_FilteringByPublishedStatus() {
        // Act
        List<Noticia> noticiasPublicadas = noticiaRepository.findByPublicadaTrue();

        // Assert
        assertThat(noticiasPublicadas).hasSize(2);
        assertThat(noticiasPublicadas)
            .extracting(Noticia::getTitulo)
            .containsExactlyInAnyOrder("Primeira Notícia Publicada", "Segunda Notícia Publicada");
    }

    @Test
    void should_FindUnpublishedNews_When_FilteringByUnpublishedStatus() {
        // Act
        List<Noticia> noticiasNaoPublicadas = noticiaRepository.findByPublicadaFalse();

        // Assert
        assertThat(noticiasNaoPublicadas).hasSize(1);
        assertThat(noticiasNaoPublicadas.get(0).getTitulo()).isEqualTo("Notícia Não Publicada");
    }

    @Test
    void should_FindNewsByAuthor_When_FilteringByAuthor() {
        // Act
        List<Noticia> noticiasPorAutor1 = noticiaRepository.findByAutor(autor1);
        List<Noticia> noticiasPorAutor2 = noticiaRepository.findByAutor(autor2);

        // Assert
        assertThat(noticiasPorAutor1).hasSize(2);
        assertThat(noticiasPorAutor1)
            .extracting(Noticia::getTitulo)
            .containsExactlyInAnyOrder("Primeira Notícia Publicada", "Notícia Não Publicada");

        assertThat(noticiasPorAutor2).hasSize(1);
        assertThat(noticiasPorAutor2.get(0).getTitulo()).isEqualTo("Segunda Notícia Publicada");
    }

    @Test
    void should_FindNewsByCategory_When_FilteringByCategory() {
        // Act
        List<Noticia> noticiasTecnologia = noticiaRepository.findByCategorias(categoria1);
        List<Noticia> noticiasEsportes = noticiaRepository.findByCategorias(categoria2);

        // Assert
        assertThat(noticiasTecnologia).hasSize(2);
        assertThat(noticiasTecnologia)
            .extracting(Noticia::getTitulo)
            .containsExactlyInAnyOrder("Primeira Notícia Publicada", "Segunda Notícia Publicada");

        assertThat(noticiasEsportes).hasSize(2);
        assertThat(noticiasEsportes)
            .extracting(Noticia::getTitulo)
            .containsExactlyInAnyOrder("Segunda Notícia Publicada", "Notícia Não Publicada");
    }

    @Test
    void should_FindNewsByTitleContaining_When_SearchingByPartialTitle() {
        // Act
        List<Noticia> resultados = noticiaRepository.findByTituloContainingIgnoreCase("primeira");

        // Assert
        assertThat(resultados).hasSize(1);
        assertThat(resultados.get(0).getTitulo()).isEqualTo("Primeira Notícia Publicada");
    }

    @Test
    void should_FindNewsByContentContaining_When_SearchingByPartialContent() {
        // Act
        List<Noticia> resultados = noticiaRepository.findByConteudoContainingIgnoreCase("segunda");

        // Assert
        assertThat(resultados).hasSize(1);
        assertThat(resultados.get(0).getTitulo()).isEqualTo("Segunda Notícia Publicada");
    }

    @Test
    void should_FindPublishedNewsOrderedByDate_When_OrderingByPublicationDate() {
        // Act
        List<Noticia> noticias = noticiaRepository.findByPublicadaTrueOrderByDataPublicacaoDesc();

        // Assert
        assertThat(noticias).hasSize(2);
        assertThat(noticias.get(0).getTitulo()).isEqualTo("Segunda Notícia Publicada");
        assertThat(noticias.get(1).getTitulo()).isEqualTo("Primeira Notícia Publicada");
    }

    @Test
    void should_FindNewsWithPagination_When_UsingPageable() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 2);

        // Act
        Page<Noticia> paginaNoticias = noticiaRepository.findByPublicadaTrue(pageable);

        // Assert
        assertThat(paginaNoticias.getContent()).hasSize(2);
        assertThat(paginaNoticias.getTotalElements()).isEqualTo(2);
        assertThat(paginaNoticias.getTotalPages()).isEqualTo(1);
    }

    @Test
    void should_CountPublishedNews_When_CountingByStatus() {
        // Act
        long countPublicadas = noticiaRepository.countByPublicadaTrue();
        long countNaoPublicadas = noticiaRepository.countByPublicadaFalse();

        // Assert
        assertThat(countPublicadas).isEqualTo(2);
        assertThat(countNaoPublicadas).isEqualTo(1);
    }

    @Test
    void should_CountNewsByAuthor_When_CountingByAuthor() {
        // Act
        long countAutor1 = noticiaRepository.countByAutor(autor1);
        long countAutor2 = noticiaRepository.countByAutor(autor2);

        // Assert
        assertThat(countAutor1).isEqualTo(2);
        assertThat(countAutor2).isEqualTo(1);
    }

    @Test
    void should_FindNewsByDateRange_When_FilteringByPublicationDateRange() {
        // Arrange
        LocalDateTime dataInicio = LocalDateTime.now().minusDays(2);
        LocalDateTime dataFim = LocalDateTime.now().minusHours(6);

        // Act
        List<Noticia> noticias = noticiaRepository.findByDataPublicacaoBetween(dataInicio, dataFim);

        // Assert
        assertThat(noticias).hasSize(2);
    }

    @Test
    void should_FindRecentNews_When_FilteringByRecentDate() {
        // Arrange
        LocalDateTime ontem = LocalDateTime.now().minusDays(1);

        // Act
        List<Noticia> noticiasRecentes = noticiaRepository.findByDataPublicacaoAfter(ontem);

        // Assert
        assertThat(noticiasRecentes).hasSize(1);
        assertThat(noticiasRecentes.get(0).getTitulo()).isEqualTo("Segunda Notícia Publicada");
    }

    @Test
    void should_FindNewsCreatedAfter_When_FilteringByCreationDate() {
        // Arrange
        LocalDateTime ontem = LocalDateTime.now().minusDays(1);

        // Act
        List<Noticia> noticiasRecentes = noticiaRepository.findByDataCriacaoAfter(ontem);

        // Assert
        assertThat(noticiasRecentes).hasSize(3); // Todas foram criadas hoje
    }

    @Test
    void should_UpdateNewsStatus_When_ChangingPublishedFlag() {
        // Arrange
        Noticia noticia = noticiaRepository.findByTituloContainingIgnoreCase("não publicada").get(0);
        
        // Act
        noticia.setPublicada(true);
        noticia.setDataPublicacao(LocalDateTime.now());
        Noticia noticiaAtualizada = noticiaRepository.save(noticia);
        entityManager.flush();
        entityManager.clear();

        // Assert
        Noticia noticiaVerificada = noticiaRepository.findById(noticiaAtualizada.getId()).orElseThrow();
        assertThat(noticiaVerificada.isPublicada()).isTrue();
        assertThat(noticiaVerificada.getDataPublicacao()).isNotNull();
    }

    @Test
    void should_DeleteNews_When_NewsExists() {
        // Arrange
        Noticia noticia = noticiaRepository.findByTituloContainingIgnoreCase("primeira").get(0);
        Long noticiaId = noticia.getId();

        // Act
        noticiaRepository.delete(noticia);
        entityManager.flush();

        // Assert
        Optional<Noticia> noticiaDeletada = noticiaRepository.findById(noticiaId);
        assertThat(noticiaDeletada).isEmpty();
    }

    @Test
    void should_FindNewsWithCategories_When_EagerLoadingCategories() {
        // Act
        List<Noticia> noticias = noticiaRepository.findAllWithCategorias();

        // Assert
        assertThat(noticias).hasSize(3);
        noticias.forEach(noticia -> {
            assertThat(noticia.getCategorias()).isNotEmpty();
        });
    }

    @Test
    void should_FindMostRecentPublishedNews_When_LimitingResults() {
        // Act
        List<Noticia> noticiasRecentes = noticiaRepository.findTop5ByPublicadaTrueOrderByDataPublicacaoDesc();

        // Assert
        assertThat(noticiasRecentes).hasSize(2); // Só temos 2 publicadas
        assertThat(noticiasRecentes.get(0).getTitulo()).isEqualTo("Segunda Notícia Publicada");
    }

    @Test
    void should_SearchNewsFullText_When_SearchingByMultipleFields() {
        // Act
        List<Noticia> resultados = noticiaRepository.searchByTituloOrConteudo("segunda");

        // Assert
        assertThat(resultados).hasSize(1);
        assertThat(resultados.get(0).getTitulo()).isEqualTo("Segunda Notícia Publicada");
    }

    @Test
    void should_FindNewsByAuthorAndStatus_When_FilteringByMultipleCriteria() {
        // Act
        List<Noticia> noticias = noticiaRepository.findByAutorAndPublicada(autor1, true);

        // Assert
        assertThat(noticias).hasSize(1);
        assertThat(noticias.get(0).getTitulo()).isEqualTo("Primeira Notícia Publicada");
    }

    @Test
    void should_FindNewsByCategoryAndStatus_When_FilteringByMultipleCriteria() {
        // Act
        List<Noticia> noticias = noticiaRepository.findByCategoriasAndPublicada(categoria1, true);

        // Assert
        assertThat(noticias).hasSize(2);
        assertThat(noticias)
            .extracting(Noticia::getTitulo)
            .containsExactlyInAnyOrder("Primeira Notícia Publicada", "Segunda Notícia Publicada");
    }
}