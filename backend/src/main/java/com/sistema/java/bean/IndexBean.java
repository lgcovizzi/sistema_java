package com.sistema.java.bean;

import com.sistema.java.model.dto.CategoriaDTO;
import com.sistema.java.model.dto.NoticiaDTO;
import com.sistema.java.service.CategoriaService;
import com.sistema.java.service.NoticiaService;
import com.sistema.java.service.UsuarioService;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Managed Bean para a página inicial do sistema.
 * Responsável por fornecer dados para a home page.
 */
@Named("indexBean")
@RequestScoped
public class IndexBean implements Serializable {
    
    private static final Logger logger = LoggerFactory.getLogger(IndexBean.class);
    
    @Inject
    private NoticiaService noticiaService;
    
    @Inject
    private CategoriaService categoriaService;
    
    @Inject
    private UsuarioService usuarioService;
    
    // Propriedades para a página inicial
    private List<NoticiaDTO> noticiasRecentes;
    private List<NoticiaDTO> noticiasMaisComentadas;
    private List<CategoriaDTO> categoriasAtivas;
    private Map<String, Long> estatisticas;
    private String emailNewsletter;
    private boolean inscricaoSucesso;
    private String mensagemInscricao;
    
    // Configurações da página
    private static final int LIMITE_NOTICIAS_RECENTES = 6;
    private static final int LIMITE_NOTICIAS_COMENTADAS = 3;
    private static final int LIMITE_CATEGORIAS = 8;
    
    @PostConstruct
    public void init() {
        try {
            carregarDados();
        } catch (Exception e) {
            logger.error("Erro ao inicializar IndexBean", e);
        }
    }
    
    /**
     * Carrega todos os dados necessários para a página inicial
     */
    private void carregarDados() {
        carregarNoticiasRecentes();
        carregarNoticiasMaisComentadas();
        carregarCategoriasAtivas();
        carregarEstatisticas();
    }
    
    /**
     * Carrega as notícias mais recentes publicadas
     */
    private void carregarNoticiasRecentes() {
        try {
            this.noticiasRecentes = noticiaService.findRecentes(LIMITE_NOTICIAS_RECENTES);
            logger.debug("Carregadas {} notícias recentes", noticiasRecentes.size());
        } catch (Exception e) {
            logger.error("Erro ao carregar notícias recentes", e);
            this.noticiasRecentes = new ArrayList<>();
        }
    }
    
    /**
     * Carrega as notícias com mais comentários
     */
    private void carregarNoticiasMaisComentadas() {
        try {
            // TODO: Implementar busca de notícias mais comentadas
            this.noticiasMaisComentadas = noticiaService.findRecentes(LIMITE_NOTICIAS_COMENTADAS);
            logger.debug("Carregadas {} notícias mais comentadas", noticiasMaisComentadas.size());
        } catch (Exception e) {
            logger.error("Erro ao carregar notícias mais comentadas", e);
            this.noticiasMaisComentadas = new ArrayList<>();
        }
    }
    
    /**
     * Carrega as categorias ativas
     */
    private void carregarCategoriasAtivas() {
        try {
            this.categoriasAtivas = categoriaService.buscarAtivas();
            
            // Limita o número de categorias exibidas
            if (categoriasAtivas.size() > LIMITE_CATEGORIAS) {
                this.categoriasAtivas = categoriasAtivas.subList(0, LIMITE_CATEGORIAS);
            }
            
            logger.debug("Carregadas {} categorias ativas", categoriasAtivas.size());
        } catch (Exception e) {
            logger.error("Erro ao carregar categorias ativas", e);
            this.categoriasAtivas = new ArrayList<>();
        }
    }
    
    /**
     * Carrega estatísticas gerais do sistema
     */
    private void carregarEstatisticas() {
        try {
            // TODO: Implementar contadores específicos
            this.estatisticas = Map.of(
                "totalNoticias", 0L,
                "totalCategorias", 0L,
                "totalUsuarios", 0L,
                "noticiasEsteAno", 0L
            );
            
            logger.debug("Estatísticas carregadas: {}", estatisticas);
        } catch (Exception e) {
            logger.error("Erro ao carregar estatísticas", e);
            this.estatisticas = Map.of(
                "totalNoticias", 0L,
                "totalCategorias", 0L,
                "totalUsuarios", 0L,
                "noticiasEsteAno", 0L
            );
        }
    }
    
    /**
     * Processa a inscrição na newsletter
     */
    public void inscreverNewsletter() {
        try {
            if (emailNewsletter == null || emailNewsletter.trim().isEmpty()) {
                this.mensagemInscricao = "Por favor, informe um e-mail válido.";
                this.inscricaoSucesso = false;
                return;
            }
            
            // Validação básica de e-mail
            if (!emailNewsletter.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
                this.mensagemInscricao = "Por favor, informe um e-mail válido.";
                this.inscricaoSucesso = false;
                return;
            }
            
            // Aqui seria implementada a lógica de inscrição na newsletter
            // Por exemplo, salvar em uma tabela de newsletter ou enviar para um serviço externo
            
            this.mensagemInscricao = "Inscrição realizada com sucesso! Obrigado por se inscrever.";
            this.inscricaoSucesso = true;
            this.emailNewsletter = ""; // Limpa o campo
            
            logger.info("Nova inscrição na newsletter: {}", emailNewsletter);
            
        } catch (Exception e) {
            logger.error("Erro ao processar inscrição na newsletter", e);
            this.mensagemInscricao = "Erro ao processar inscrição. Tente novamente.";
            this.inscricaoSucesso = false;
        }
    }
    
    /**
     * Recarrega os dados da página
     */
    public void recarregarDados() {
        try {
            carregarDados();
            logger.info("Dados da página inicial recarregados");
        } catch (Exception e) {
            logger.error("Erro ao recarregar dados da página inicial", e);
        }
    }
    
    /**
     * Verifica se há notícias recentes disponíveis
     * Referência: Sistema de Temas Claros e Escuros - project_rules.md
     * Exibição de notícias deve adaptar-se ao tema ativo do usuário
     */
    public boolean hasNoticiasRecentes() {
        return noticiasRecentes != null && !noticiasRecentes.isEmpty();
    }
    
    /**
     * Verifica se há categorias ativas disponíveis
     * Referência: Sistema de Temas Claros e Escuros - project_rules.md
     * Cores das categorias devem manter contraste adequado em ambos os temas
     */
    public boolean hasCategoriasAtivas() {
        return categoriasAtivas != null && !categoriasAtivas.isEmpty();
    }
    
    /**
     * Obtém uma estatística específica
     */
    public Long getEstatistica(String chave) {
        return estatisticas != null ? estatisticas.getOrDefault(chave, 0L) : 0L;
    }
    
    /**
     * Formata números para exibição (ex: 1.234)
     */
    public String formatarNumero(Long numero) {
        if (numero == null) return "0";
        return String.format("%,d", numero);
    }
    
    /**
     * Obtém uma prévia do conteúdo da notícia
     */
    public String getPreviewConteudo(NoticiaDTO noticia, int limite) {
        if (noticia == null || noticia.getConteudo() == null) {
            return "";
        }
        
        String conteudo = noticia.getConteudo();
        if (conteudo.length() <= limite) {
            return conteudo;
        }
        
        return conteudo.substring(0, limite) + "...";
    }
    
    /**
     * Calcula o tempo decorrido desde a publicação
     */
    public String getTempoPublicacao(NoticiaDTO noticia) {
        if (noticia == null || noticia.getDataPublicacao() == null) {
            return "";
        }
        
        // Implementação simplificada - pode ser melhorada
        long diff = System.currentTimeMillis() - noticia.getDataPublicacao().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        long dias = diff / (24 * 60 * 60 * 1000);
        
        if (dias == 0) {
            return "Hoje";
        } else if (dias == 1) {
            return "Ontem";
        } else if (dias < 7) {
            return dias + " dias atrás";
        } else if (dias < 30) {
            long semanas = dias / 7;
            return semanas + (semanas == 1 ? " semana atrás" : " semanas atrás");
        } else {
            long meses = dias / 30;
            return meses + (meses == 1 ? " mês atrás" : " meses atrás");
        }
    }
    
    // Getters e Setters
    public List<NoticiaDTO> getNoticiasRecentes() {
        return noticiasRecentes;
    }
    
    public void setNoticiasRecentes(List<NoticiaDTO> noticiasRecentes) {
        this.noticiasRecentes = noticiasRecentes;
    }
    
    public List<NoticiaDTO> getNoticiasMaisComentadas() {
        return noticiasMaisComentadas;
    }
    
    public void setNoticiasMaisComentadas(List<NoticiaDTO> noticiasMaisComentadas) {
        this.noticiasMaisComentadas = noticiasMaisComentadas;
    }
    
    public List<CategoriaDTO> getCategoriasAtivas() {
        return categoriasAtivas;
    }
    
    public void setCategoriasAtivas(List<CategoriaDTO> categoriasAtivas) {
        this.categoriasAtivas = categoriasAtivas;
    }
    
    public Map<String, Long> getEstatisticas() {
        return estatisticas;
    }
    
    public void setEstatisticas(Map<String, Long> estatisticas) {
        this.estatisticas = estatisticas;
    }
    
    public String getEmailNewsletter() {
        return emailNewsletter;
    }
    
    public void setEmailNewsletter(String emailNewsletter) {
        this.emailNewsletter = emailNewsletter;
    }
    
    public boolean isInscricaoSucesso() {
        return inscricaoSucesso;
    }
    
    public void setInscricaoSucesso(boolean inscricaoSucesso) {
        this.inscricaoSucesso = inscricaoSucesso;
    }
    
    public String getMensagemInscricao() {
        return mensagemInscricao;
    }
    
    public void setMensagemInscricao(String mensagemInscricao) {
        this.mensagemInscricao = mensagemInscricao;
    }
}