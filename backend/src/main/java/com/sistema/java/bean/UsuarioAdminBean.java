package com.sistema.java.bean;

import com.sistema.java.model.dto.UsuarioDTO;
import com.sistema.java.service.UsuarioService;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ViewScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.primefaces.model.LazyDataModel;
import org.primefaces.model.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Managed Bean para administração de usuários.
 * Responsável por gerenciar CRUD e operações administrativas de usuários.
 */
@Named("usuarioAdminBean")
@ViewScoped
public class UsuarioAdminBean implements Serializable {
    
    private static final Logger logger = LoggerFactory.getLogger(UsuarioAdminBean.class);
    
    @Inject
    private UsuarioService usuarioService;
    
    // Propriedades para listagem
    private LazyDataModel<UsuarioDTO> usuariosLazy;
    private List<UsuarioDTO> usuariosSelecionados;
    
    // Propriedades para filtros
    private String filtroNome;
    private String filtroEmail;
    private Boolean filtroAtivo;
    private Date dataInicio;
    private Date dataFim;
    private String termoPesquisa;
    
    // Propriedades para CRUD
    private UsuarioDTO usuarioEdicao;
    private UsuarioDTO usuarioVisualizacao;
    private boolean modoEdicao;
    private String senhaConfirmacao;
    private String novaSenha;
    
    // Estatísticas
    private long totalUsuarios;
    private long usuariosAtivos;
    private long usuariosInativos;
    private long usuariosCadastradosHoje;
    
    @PostConstruct
    public void init() {
        try {
            inicializarDados();
            configurarLazyModel();
            carregarEstatisticas();
        } catch (Exception e) {
            logger.error("Erro ao inicializar UsuarioAdminBean", e);
        }
    }
    
    /**
     * Inicializa os dados necessários
     */
    private void inicializarDados() {
        this.usuariosSelecionados = new ArrayList<>();
        inicializarUsuarioEdicao();
    }
    
    /**
     * Configura o modelo lazy para paginação
     */
    private void configurarLazyModel() {
        this.usuariosLazy = new LazyDataModel<UsuarioDTO>() {
            @Override
            public int count(Map<String, Object> filterBy) {
                try {
                    return (int) usuarioService.contarComFiltros(
                        filtroNome, filtroEmail, filtroAtivo, dataInicio, dataFim, termoPesquisa
                    );
                } catch (Exception e) {
                    logger.error("Erro ao contar usuários", e);
                    return 0;
                }
            }
            
            @Override
            public List<UsuarioDTO> load(int first, int pageSize, Map<String, Object> sortBy, Map<String, Object> filterBy) {
                try {
                    int pagina = first / pageSize;
                    String ordenacao = determinarOrdenacao(sortBy);
                    
                    return usuarioService.buscarComFiltros(
                        filtroNome, filtroEmail, filtroAtivo, dataInicio, dataFim, termoPesquisa,
                        ordenacao, pagina, pageSize
                    );
                } catch (Exception e) {
                    logger.error("Erro ao carregar usuários", e);
                    return new ArrayList<>();
                }
            }
        };
        
        this.usuariosLazy.setRowCount(20);
    }
    
    /**
     * Determina a ordenação baseada nos parâmetros
     */
    private String determinarOrdenacao(Map<String, Object> sortBy) {
        if (sortBy != null && !sortBy.isEmpty()) {
            String campo = sortBy.keySet().iterator().next();
            SortOrder ordem = (SortOrder) sortBy.get(campo);
            return campo + (ordem == SortOrder.DESCENDING ? " DESC" : " ASC");
        }
        return "dataCriacao DESC";
    }
    
    /**
     * Carrega as estatísticas dos usuários
     */
    private void carregarEstatisticas() {
        try {
            this.totalUsuarios = usuarioService.contar();
            this.usuariosAtivos = usuarioService.contarAtivos();
            this.usuariosInativos = usuarioService.contarInativos();
            this.usuariosCadastradosHoje = usuarioService.contarCadastradosHoje();
            
            logger.debug("Estatísticas carregadas - Total: {}, Ativos: {}, Inativos: {}, Hoje: {}", 
                        totalUsuarios, usuariosAtivos, usuariosInativos, usuariosCadastradosHoje);
        } catch (Exception e) {
            logger.error("Erro ao carregar estatísticas", e);
        }
    }
    
    /**
     * Aplica os filtros de pesquisa
     */
    public void aplicarFiltros() {
        try {
            configurarLazyModel();
            logger.debug("Filtros aplicados - Nome: {}, Email: {}, Ativo: {}", 
                        filtroNome, filtroEmail, filtroAtivo);
        } catch (Exception e) {
            logger.error("Erro ao aplicar filtros", e);
            adicionarMensagem(FacesMessage.SEVERITY_ERROR, "Erro ao aplicar filtros");
        }
    }
    
    /**
     * Limpa todos os filtros
     */
    public void limparFiltros() {
        this.filtroNome = null;
        this.filtroEmail = null;
        this.filtroAtivo = null;
        this.dataInicio = null;
        this.dataFim = null;
        this.termoPesquisa = null;
        aplicarFiltros();
    }
    
    /**
     * Pesquisa usuários por termo
     */
    public void pesquisar() {
        aplicarFiltros();
    }
    
    /**
     * Inicializa um novo usuário para edição
     */
    private void inicializarUsuarioEdicao() {
        this.usuarioEdicao = new UsuarioDTO();
        this.usuarioEdicao.setAtivo(true);
        this.modoEdicao = false;
        this.senhaConfirmacao = null;
        this.novaSenha = null;
    }
    
    /**
     * Prepara para criar um novo usuário
     */
    public void novoUsuario() {
        inicializarUsuarioEdicao();
        this.modoEdicao = false;
    }
    
    /**
     * Prepara para editar um usuário existente
     */
    public void editarUsuario(UsuarioDTO usuario) {
        try {
            this.usuarioEdicao = usuarioService.buscarPorId(usuario.getId());
            this.modoEdicao = true;
            this.senhaConfirmacao = null;
            this.novaSenha = null;
        } catch (Exception e) {
            logger.error("Erro ao carregar usuário para edição: " + usuario.getId(), e);
            adicionarMensagem(FacesMessage.SEVERITY_ERROR, "Erro ao carregar usuário");
        }
    }
    
    /**
     * Visualiza detalhes de um usuário
     */
    public void visualizarUsuario(UsuarioDTO usuario) {
        try {
            this.usuarioVisualizacao = usuarioService.buscarPorId(usuario.getId());
        } catch (Exception e) {
            logger.error("Erro ao carregar usuário para visualização: " + usuario.getId(), e);
            adicionarMensagem(FacesMessage.SEVERITY_ERROR, "Erro ao carregar usuário");
        }
    }
    
    /**
     * Salva o usuário (criar ou atualizar)
     */
    public void salvarUsuario() {
        try {
            // Validações
            if (!validarUsuario()) {
                return;
            }
            
            if (modoEdicao) {
                // Atualizar usuário existente
                if (novaSenha != null && !novaSenha.trim().isEmpty()) {
                    usuarioEdicao.setSenha(novaSenha);
                }
                
                usuarioService.atualizar(usuarioEdicao.getId(), usuarioEdicao);
                adicionarMensagem(FacesMessage.SEVERITY_INFO, "Usuário atualizado com sucesso!");
                
                logger.info("Usuário atualizado: {}", usuarioEdicao.getEmail());
            } else {
                // Criar novo usuário
                usuarioEdicao.setSenha(novaSenha);
                usuarioEdicao.setDataCriacao(new Date());
                
                usuarioService.criar(usuarioEdicao);
                adicionarMensagem(FacesMessage.SEVERITY_INFO, "Usuário criado com sucesso!");
                
                logger.info("Novo usuário criado: {}", usuarioEdicao.getEmail());
            }
            
            // Atualiza a listagem e estatísticas
            configurarLazyModel();
            carregarEstatisticas();
            
        } catch (Exception e) {
            logger.error("Erro ao salvar usuário", e);
            adicionarMensagem(FacesMessage.SEVERITY_ERROR, "Erro ao salvar usuário: " + e.getMessage());
        }
    }
    
    /**
     * Valida os dados do usuário
     */
    private boolean validarUsuario() {
        if (usuarioEdicao.getNome() == null || usuarioEdicao.getNome().trim().isEmpty()) {
            adicionarMensagem(FacesMessage.SEVERITY_WARN, "Nome é obrigatório");
            return false;
        }
        
        if (usuarioEdicao.getEmail() == null || usuarioEdicao.getEmail().trim().isEmpty()) {
            adicionarMensagem(FacesMessage.SEVERITY_WARN, "E-mail é obrigatório");
            return false;
        }
        
        if (!modoEdicao || (novaSenha != null && !novaSenha.trim().isEmpty())) {
            if (novaSenha == null || novaSenha.trim().isEmpty()) {
                adicionarMensagem(FacesMessage.SEVERITY_WARN, "Senha é obrigatória");
                return false;
            }
            
            if (novaSenha.length() < 6) {
                adicionarMensagem(FacesMessage.SEVERITY_WARN, "Senha deve ter pelo menos 6 caracteres");
                return false;
            }
            
            if (!novaSenha.equals(senhaConfirmacao)) {
                adicionarMensagem(FacesMessage.SEVERITY_WARN, "Senhas não conferem");
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Exclui um usuário
     */
    public void excluirUsuario(UsuarioDTO usuario) {
        try {
            usuarioService.excluir(usuario.getId());
            adicionarMensagem(FacesMessage.SEVERITY_INFO, "Usuário excluído com sucesso!");
            
            configurarLazyModel();
            carregarEstatisticas();
            
            logger.info("Usuário excluído: {}", usuario.getEmail());
            
        } catch (Exception e) {
            logger.error("Erro ao excluir usuário: " + usuario.getId(), e);
            adicionarMensagem(FacesMessage.SEVERITY_ERROR, "Erro ao excluir usuário: " + e.getMessage());
        }
    }
    
    /**
     * Ativa/desativa um usuário
     */
    public void alterarStatusUsuario(UsuarioDTO usuario) {
        try {
            usuario.setAtivo(!usuario.getAtivo());
            usuarioService.atualizarStatus(usuario.getId(), usuario.getAtivo());
            
            String status = usuario.getAtivo() ? "ativado" : "desativado";
            adicionarMensagem(FacesMessage.SEVERITY_INFO, "Usuário " + status + " com sucesso!");
            
            carregarEstatisticas();
            
            logger.info("Status do usuário alterado: {} - {}", usuario.getEmail(), status);
            
        } catch (Exception e) {
            logger.error("Erro ao alterar status do usuário: " + usuario.getId(), e);
            adicionarMensagem(FacesMessage.SEVERITY_ERROR, "Erro ao alterar status do usuário");
            
            // Reverte a alteração
            usuario.setAtivo(!usuario.getAtivo());
        }
    }
    
    /**
     * Ativa usuários selecionados
     */
    public void ativarSelecionados() {
        try {
            if (usuariosSelecionados.isEmpty()) {
                adicionarMensagem(FacesMessage.SEVERITY_WARN, "Selecione pelo menos um usuário");
                return;
            }
            
            List<Long> ids = usuariosSelecionados.stream()
                .map(UsuarioDTO::getId)
                .toList();
            
            usuarioService.atualizarStatusEmLote(ids, true);
            
            adicionarMensagem(FacesMessage.SEVERITY_INFO, 
                usuariosSelecionados.size() + " usuário(s) ativado(s) com sucesso!");
            
            configurarLazyModel();
            carregarEstatisticas();
            usuariosSelecionados.clear();
            
            logger.info("Usuários ativados em lote: {}", ids.size());
            
        } catch (Exception e) {
            logger.error("Erro ao ativar usuários selecionados", e);
            adicionarMensagem(FacesMessage.SEVERITY_ERROR, "Erro ao ativar usuários selecionados");
        }
    }
    
    /**
     * Desativa usuários selecionados
     */
    public void desativarSelecionados() {
        try {
            if (usuariosSelecionados.isEmpty()) {
                adicionarMensagem(FacesMessage.SEVERITY_WARN, "Selecione pelo menos um usuário");
                return;
            }
            
            List<Long> ids = usuariosSelecionados.stream()
                .map(UsuarioDTO::getId)
                .toList();
            
            usuarioService.atualizarStatusEmLote(ids, false);
            
            adicionarMensagem(FacesMessage.SEVERITY_INFO, 
                usuariosSelecionados.size() + " usuário(s) desativado(s) com sucesso!");
            
            configurarLazyModel();
            carregarEstatisticas();
            usuariosSelecionados.clear();
            
            logger.info("Usuários desativados em lote: {}", ids.size());
            
        } catch (Exception e) {
            logger.error("Erro ao desativar usuários selecionados", e);
            adicionarMensagem(FacesMessage.SEVERITY_ERROR, "Erro ao desativar usuários selecionados");
        }
    }
    
    /**
     * Exclui usuários selecionados
     */
    public void excluirSelecionados() {
        try {
            if (usuariosSelecionados.isEmpty()) {
                adicionarMensagem(FacesMessage.SEVERITY_WARN, "Selecione pelo menos um usuário");
                return;
            }
            
            List<Long> ids = usuariosSelecionados.stream()
                .map(UsuarioDTO::getId)
                .toList();
            
            usuarioService.excluirEmLote(ids);
            
            adicionarMensagem(FacesMessage.SEVERITY_INFO, 
                usuariosSelecionados.size() + " usuário(s) excluído(s) com sucesso!");
            
            configurarLazyModel();
            carregarEstatisticas();
            usuariosSelecionados.clear();
            
            logger.info("Usuários excluídos em lote: {}", ids.size());
            
        } catch (Exception e) {
            logger.error("Erro ao excluir usuários selecionados", e);
            adicionarMensagem(FacesMessage.SEVERITY_ERROR, "Erro ao excluir usuários selecionados");
        }
    }
    
    /**
     * Adiciona mensagem ao contexto JSF
     */
    private void adicionarMensagem(FacesMessage.Severity severity, String mensagem) {
        FacesContext.getCurrentInstance().addMessage(null, 
            new FacesMessage(severity, mensagem, null));
    }
    
    // Getters e Setters
    public LazyDataModel<UsuarioDTO> getUsuariosLazy() {
        return usuariosLazy;
    }
    
    public void setUsuariosLazy(LazyDataModel<UsuarioDTO> usuariosLazy) {
        this.usuariosLazy = usuariosLazy;
    }
    
    public List<UsuarioDTO> getUsuariosSelecionados() {
        return usuariosSelecionados;
    }
    
    public void setUsuariosSelecionados(List<UsuarioDTO> usuariosSelecionados) {
        this.usuariosSelecionados = usuariosSelecionados;
    }
    
    public String getFiltroNome() {
        return filtroNome;
    }
    
    public void setFiltroNome(String filtroNome) {
        this.filtroNome = filtroNome;
    }
    
    public String getFiltroEmail() {
        return filtroEmail;
    }
    
    public void setFiltroEmail(String filtroEmail) {
        this.filtroEmail = filtroEmail;
    }
    
    public Boolean getFiltroAtivo() {
        return filtroAtivo;
    }
    
    public void setFiltroAtivo(Boolean filtroAtivo) {
        this.filtroAtivo = filtroAtivo;
    }
    
    public Date getDataInicio() {
        return dataInicio;
    }
    
    public void setDataInicio(Date dataInicio) {
        this.dataInicio = dataInicio;
    }
    
    public Date getDataFim() {
        return dataFim;
    }
    
    public void setDataFim(Date dataFim) {
        this.dataFim = dataFim;
    }
    
    public String getTermoPesquisa() {
        return termoPesquisa;
    }
    
    public void setTermoPesquisa(String termoPesquisa) {
        this.termoPesquisa = termoPesquisa;
    }
    
    public UsuarioDTO getUsuarioEdicao() {
        return usuarioEdicao;
    }
    
    public void setUsuarioEdicao(UsuarioDTO usuarioEdicao) {
        this.usuarioEdicao = usuarioEdicao;
    }
    
    public UsuarioDTO getUsuarioVisualizacao() {
        return usuarioVisualizacao;
    }
    
    public void setUsuarioVisualizacao(UsuarioDTO usuarioVisualizacao) {
        this.usuarioVisualizacao = usuarioVisualizacao;
    }
    
    public boolean isModoEdicao() {
        return modoEdicao;
    }
    
    public void setModoEdicao(boolean modoEdicao) {
        this.modoEdicao = modoEdicao;
    }
    
    public String getSenhaConfirmacao() {
        return senhaConfirmacao;
    }
    
    public void setSenhaConfirmacao(String senhaConfirmacao) {
        this.senhaConfirmacao = senhaConfirmacao;
    }
    
    public String getNovaSenha() {
        return novaSenha;
    }
    
    public void setNovaSenha(String novaSenha) {
        this.novaSenha = novaSenha;
    }
    
    public long getTotalUsuarios() {
        return totalUsuarios;
    }
    
    public void setTotalUsuarios(long totalUsuarios) {
        this.totalUsuarios = totalUsuarios;
    }
    
    public long getUsuariosAtivos() {
        return usuariosAtivos;
    }
    
    public void setUsuariosAtivos(long usuariosAtivos) {
        this.usuariosAtivos = usuariosAtivos;
    }
    
    public long getUsuariosInativos() {
        return usuariosInativos;
    }
    
    public void setUsuariosInativos(long usuariosInativos) {
        this.usuariosInativos = usuariosInativos;
    }
    
    public long getUsuariosCadastradosHoje() {
        return usuariosCadastradosHoje;
    }
    
    public void setUsuariosCadastradosHoje(long usuariosCadastradosHoje) {
        this.usuariosCadastradosHoje = usuariosCadastradosHoje;
    }
}