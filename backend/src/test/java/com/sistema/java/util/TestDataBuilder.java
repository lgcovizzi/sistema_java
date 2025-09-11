package com.sistema.java.util;

import com.sistema.java.model.entity.*;
import com.sistema.java.model.enums.PapelUsuario;
import com.sistema.java.model.enums.StatusComentario;
import com.sistema.java.model.dto.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Builder para criação de dados de teste
 * Referência: Testes e Qualidade de Código - project_rules.md
 * Referência: Padrões para Entidades JPA - project_rules.md
 */
public class TestDataBuilder {

    // ========== USUÁRIOS ==========
    
    public static Usuario criarUsuarioComum() {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setNome("João");
        usuario.setSobrenome("Silva");
        usuario.setCpf("12345678901");
        usuario.setEmail("joao@email.com");
        usuario.setSenha("$2a$10$encrypted.password");
        usuario.setTelefone("(11) 99999-9999");
        usuario.setDataNascimento(LocalDate.of(1990, 1, 1));
        usuario.setPapel(PapelUsuario.USUARIO);
        usuario.setAtivo(true);
        usuario.setDataCriacao(LocalDateTime.now().minusDays(30));
        usuario.setDataAtualizacao(LocalDateTime.now());
        return usuario;
    }

    public static Usuario criarAdministrador() {
        Usuario admin = new Usuario();
        admin.setId(2L);
        admin.setNome("Admin");
        admin.setSobrenome("Sistema");
        admin.setCpf("98765432100");
        admin.setEmail("admin@sistema.com");
        admin.setSenha("$2a$10$encrypted.admin.password");
        admin.setTelefone("(11) 88888-8888");
        admin.setDataNascimento(LocalDate.of(1985, 5, 15));
        admin.setPapel(PapelUsuario.ADMINISTRADOR);
        admin.setAtivo(true);
        admin.setDataCriacao(LocalDateTime.now().minusDays(365));
        admin.setDataAtualizacao(LocalDateTime.now());
        return admin;
    }

    public static Usuario criarColaborador() {
        Usuario colaborador = new Usuario();
        colaborador.setId(3L);
        colaborador.setNome("Maria");
        colaborador.setSobrenome("Santos");
        colaborador.setCpf("11122233344");
        colaborador.setEmail("maria@sistema.com");
        colaborador.setSenha("$2a$10$encrypted.colaborador.password");
        colaborador.setTelefone("(11) 77777-7777");
        colaborador.setDataNascimento(LocalDate.of(1988, 8, 20));
        colaborador.setPapel(PapelUsuario.COLABORADOR);
        colaborador.setAtivo(true);
        colaborador.setDataCriacao(LocalDateTime.now().minusDays(180));
        colaborador.setDataAtualizacao(LocalDateTime.now());
        return colaborador;
    }

    public static Usuario criarAssociado() {
        Usuario associado = new Usuario();
        associado.setId(4L);
        associado.setNome("Carlos");
        associado.setSobrenome("Oliveira");
        associado.setCpf("55566677788");
        associado.setEmail("carlos@email.com");
        associado.setSenha("$2a$10$encrypted.associado.password");
        associado.setTelefone("(11) 66666-6666");
        associado.setDataNascimento(LocalDate.of(1992, 12, 10));
        associado.setPapel(PapelUsuario.ASSOCIADO);
        associado.setAtivo(true);
        associado.setDataCriacao(LocalDateTime.now().minusDays(90));
        associado.setDataAtualizacao(LocalDateTime.now());
        return associado;
    }

    public static Usuario criarFundador() {
        Usuario fundador = new Usuario();
        fundador.setId(5L);
        fundador.setNome("Roberto");
        fundador.setSobrenome("Fundador");
        fundador.setCpf("99988877766");
        fundador.setEmail("fundador@sistema.com");
        fundador.setSenha("$2a$10$encrypted.fundador.password");
        fundador.setTelefone("(11) 55555-5555");
        fundador.setDataNascimento(LocalDate.of(1980, 3, 25));
        fundador.setPapel(PapelUsuario.FUNDADOR);
        fundador.setAtivo(true);
        fundador.setDataCriacao(LocalDateTime.now().minusDays(1000));
        fundador.setDataAtualizacao(LocalDateTime.now());
        return fundador;
    }

    public static Usuario criarParceiro() {
        Usuario parceiro = new Usuario();
        parceiro.setId(6L);
        parceiro.setNome("Ana");
        parceiro.setSobrenome("Parceira");
        parceiro.setCpf("44433322211");
        parceiro.setEmail("ana@parceiro.com");
        parceiro.setSenha("$2a$10$encrypted.parceiro.password");
        parceiro.setTelefone("(11) 44444-4444");
        parceiro.setDataNascimento(LocalDate.of(1987, 7, 8));
        parceiro.setPapel(PapelUsuario.PARCEIRO);
        parceiro.setAtivo(true);
        parceiro.setDataCriacao(LocalDateTime.now().minusDays(200));
        parceiro.setDataAtualizacao(LocalDateTime.now());
        return parceiro;
    }

    // ========== CATEGORIAS ==========
    
    public static Categoria criarCategoriaGeral() {
        Categoria categoria = new Categoria();
        categoria.setId(1L);
        categoria.setNome("Geral");
        categoria.setDescricao("Notícias gerais do sistema");
        categoria.setAtiva(true);
        categoria.setDataCriacao(LocalDateTime.now().minusDays(100));
        return categoria;
    }

    public static Categoria criarCategoriaTecnologia() {
        Categoria categoria = new Categoria();
        categoria.setId(2L);
        categoria.setNome("Tecnologia");
        categoria.setDescricao("Notícias sobre tecnologia e inovação");
        categoria.setAtiva(true);
        categoria.setDataCriacao(LocalDateTime.now().minusDays(80));
        return categoria;
    }

    public static Categoria criarCategoriaEventos() {
        Categoria categoria = new Categoria();
        categoria.setId(3L);
        categoria.setNome("Eventos");
        categoria.setDescricao("Informações sobre eventos e atividades");
        categoria.setAtiva(true);
        categoria.setDataCriacao(LocalDateTime.now().minusDays(60));
        return categoria;
    }

    public static Categoria criarCategoriaInativa() {
        Categoria categoria = new Categoria();
        categoria.setId(4L);
        categoria.setNome("Categoria Inativa");
        categoria.setDescricao("Categoria desativada para testes");
        categoria.setAtiva(false);
        categoria.setDataCriacao(LocalDateTime.now().minusDays(200));
        return categoria;
    }

    // ========== NOTÍCIAS ==========
    
    public static Noticia criarNoticiaPublicada() {
        Noticia noticia = new Noticia();
        noticia.setId(1L);
        noticia.setTitulo("Primeira Notícia do Sistema");
        noticia.setResumo("Esta é a primeira notícia publicada no sistema para testes.");
        noticia.setConteudo("Conteúdo completo da primeira notícia do sistema. " +
                           "Esta notícia serve para testar as funcionalidades básicas " +
                           "de criação, edição e visualização de notícias.");
        noticia.setAutor(criarColaborador());
        noticia.setPublicada(true);
        noticia.setDataPublicacao(LocalDateTime.now().minusDays(5));
        noticia.setDataCriacao(LocalDateTime.now().minusDays(7));
        noticia.setDataAtualizacao(LocalDateTime.now().minusDays(5));
        
        List<Categoria> categorias = new ArrayList<>();
        categorias.add(criarCategoriaGeral());
        categorias.add(criarCategoriaTecnologia());
        noticia.setCategorias(categorias);
        
        return noticia;
    }

    public static Noticia criarNoticiaRascunho() {
        Noticia noticia = new Noticia();
        noticia.setId(2L);
        noticia.setTitulo("Notícia em Rascunho");
        noticia.setResumo("Esta notícia ainda está sendo elaborada.");
        noticia.setConteudo("Conteúdo da notícia em rascunho. Ainda não foi publicada.");
        noticia.setAutor(criarColaborador());
        noticia.setPublicada(false);
        noticia.setDataPublicacao(null);
        noticia.setDataCriacao(LocalDateTime.now().minusDays(2));
        noticia.setDataAtualizacao(LocalDateTime.now().minusDays(1));
        
        List<Categoria> categorias = new ArrayList<>();
        categorias.add(criarCategoriaGeral());
        noticia.setCategorias(categorias);
        
        return noticia;
    }

    public static Noticia criarNoticiaEventos() {
        Noticia noticia = new Noticia();
        noticia.setId(3L);
        noticia.setTitulo("Próximo Evento do Sistema");
        noticia.setResumo("Informações sobre o próximo evento organizado.");
        noticia.setConteudo("Detalhes completos sobre o próximo evento. " +
                           "Data, horário, local e programação completa.");
        noticia.setAutor(criarAdministrador());
        noticia.setPublicada(true);
        noticia.setDataPublicacao(LocalDateTime.now().minusDays(1));
        noticia.setDataCriacao(LocalDateTime.now().minusDays(3));
        noticia.setDataAtualizacao(LocalDateTime.now().minusDays(1));
        
        List<Categoria> categorias = new ArrayList<>();
        categorias.add(criarCategoriaEventos());
        noticia.setCategorias(categorias);
        
        return noticia;
    }

    // ========== COMENTÁRIOS ==========
    
    public static Comentario criarComentarioAprovado() {
        Comentario comentario = new Comentario();
        comentario.setId(1L);
        comentario.setConteudo("Excelente notícia! Muito informativa.");
        comentario.setAutor(criarUsuarioComum());
        comentario.setNoticia(criarNoticiaPublicada());
        comentario.setAprovado(true);
        comentario.setDataCriacao(LocalDateTime.now().minusDays(3));
        return comentario;
    }

    public static Comentario criarComentarioPendente() {
        Comentario comentario = new Comentario();
        comentario.setId(2L);
        comentario.setConteudo("Comentário aguardando aprovação.");
        comentario.setAutor(criarUsuarioComum());
        comentario.setNoticia(criarNoticiaPublicada());
        comentario.setAprovado(false);
        comentario.setDataCriacao(LocalDateTime.now().minusDays(1));
        return comentario;
    }

    public static Comentario criarComentarioAssociado() {
        Comentario comentario = new Comentario();
        comentario.setId(3L);
        comentario.setConteudo("Como associado, gostaria de parabenizar pela iniciativa.");
        comentario.setAutor(criarAssociado());
        comentario.setNoticia(criarNoticiaEventos());
        comentario.setAprovado(true);
        comentario.setDataCriacao(LocalDateTime.now().minusHours(12));
        return comentario;
    }

    // ========== DTOs ==========
    
    public static LoginDTO criarLoginDTO() {
        LoginDTO loginDTO = new LoginDTO();
        loginDTO.setEmail("joao@email.com");
        loginDTO.setSenha("senha123");
        loginDTO.setLembrarMe(false);
        return loginDTO;
    }

    public static RegistroRequestDTO criarRegistroDTO() {
        RegistroRequestDTO registroDTO = new RegistroRequestDTO();
        registroDTO.setNome("Novo");
        registroDTO.setSobrenome("Usuario");
        registroDTO.setCpf("12312312312");
        registroDTO.setEmail("novo@email.com");
        registroDTO.setSenha("senha123");
        registroDTO.setConfirmarSenha("senha123");
        registroDTO.setTelefone("(11) 99999-9999");
        registroDTO.setDataNascimento(LocalDate.of(1995, 6, 15));
        registroDTO.setAceitarTermos(true);
        return registroDTO;
    }

    public static NoticiaDTO criarNoticiaDTO() {
        NoticiaDTO noticiaDTO = new NoticiaDTO();
        noticiaDTO.setTitulo("Nova Notícia via DTO");
        noticiaDTO.setResumo("Resumo da nova notícia");
        noticiaDTO.setConteudo("Conteúdo completo da nova notícia criada via DTO");
        noticiaDTO.setPublicada(false);
        
        // List<Long> categoriaIds = new ArrayList<>();
        // categoriaIds.add(1L);
        // categoriaIds.add(2L);
        // noticiaDTO.setCategoriaIds(categoriaIds); // Método não existe
        
        return noticiaDTO;
    }

    public static CategoriaDTO criarCategoriaDTO() {
        CategoriaDTO categoriaDTO = new CategoriaDTO();
        categoriaDTO.setNome("Nova Categoria");
        categoriaDTO.setDescricao("Descrição da nova categoria");
        categoriaDTO.setAtiva(true);
        return categoriaDTO;
    }

    public static ComentarioDTO criarComentarioDTO() {
        ComentarioDTO comentarioDTO = new ComentarioDTO();
        comentarioDTO.setConteudo("Novo comentário via DTO");
        // comentarioDTO.setNoticiaId(1L); // Método não existe
        return comentarioDTO;
    }

    // ========== LISTAS PARA TESTES ==========
    
    public static List<Usuario> criarListaUsuarios() {
        List<Usuario> usuarios = new ArrayList<>();
        usuarios.add(criarUsuarioComum());
        usuarios.add(criarAdministrador());
        usuarios.add(criarColaborador());
        usuarios.add(criarAssociado());
        usuarios.add(criarFundador());
        usuarios.add(criarParceiro());
        return usuarios;
    }

    public static List<Categoria> criarListaCategorias() {
        List<Categoria> categorias = new ArrayList<>();
        categorias.add(criarCategoriaGeral());
        categorias.add(criarCategoriaTecnologia());
        categorias.add(criarCategoriaEventos());
        return categorias;
    }

    public static List<Noticia> criarListaNoticias() {
        List<Noticia> noticias = new ArrayList<>();
        noticias.add(criarNoticiaPublicada());
        noticias.add(criarNoticiaRascunho());
        noticias.add(criarNoticiaEventos());
        return noticias;
    }

    public static List<Comentario> criarListaComentarios() {
        List<Comentario> comentarios = new ArrayList<>();
        comentarios.add(criarComentarioAprovado());
        comentarios.add(criarComentarioPendente());
        comentarios.add(criarComentarioAssociado());
        return comentarios;
    }

    // ========== MÉTODOS UTILITÁRIOS ==========
    
    public static Usuario criarUsuarioComId(Long id) {
        Usuario usuario = criarUsuarioComum();
        usuario.setId(id);
        usuario.setEmail("usuario" + id + "@email.com");
        usuario.setCpf(String.format("%011d", id));
        return usuario;
    }

    public static Categoria criarCategoriaComId(Long id) {
        Categoria categoria = criarCategoriaGeral();
        categoria.setId(id);
        categoria.setNome("Categoria " + id);
        return categoria;
    }

    public static Noticia criarNoticiaComId(Long id) {
        Noticia noticia = criarNoticiaPublicada();
        noticia.setId(id);
        noticia.setTitulo("Notícia " + id);
        return noticia;
    }

    public static Comentario criarComentarioComId(Long id) {
        Comentario comentario = criarComentarioAprovado();
        comentario.setId(id);
        comentario.setConteudo("Comentário " + id);
        return comentario;
    }

    // ========== BUILDERS FLUENTES ==========
    
    public static class UsuarioBuilder {
        private Usuario usuario = new Usuario();
        
        public UsuarioBuilder comId(Long id) {
            usuario.setId(id);
            return this;
        }
        
        public UsuarioBuilder comNome(String nome) {
            usuario.setNome(nome);
            return this;
        }
        
        public UsuarioBuilder comEmail(String email) {
            usuario.setEmail(email);
            return this;
        }
        
        public UsuarioBuilder comPapel(PapelUsuario papel) {
            usuario.setPapel(papel);
            return this;
        }
        
        public UsuarioBuilder ativo(boolean ativo) {
            usuario.setAtivo(ativo);
            return this;
        }
        
        public Usuario build() {
            if (usuario.getDataCriacao() == null) {
                usuario.setDataCriacao(LocalDateTime.now());
            }
            if (usuario.getDataAtualizacao() == null) {
                usuario.setDataAtualizacao(LocalDateTime.now());
            }
            return usuario;
        }
    }
    
    public static UsuarioBuilder usuario() {
        return new UsuarioBuilder();
    }
}