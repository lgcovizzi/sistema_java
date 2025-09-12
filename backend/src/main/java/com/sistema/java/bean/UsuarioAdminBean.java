package com.sistema.java.bean;

import com.sistema.java.model.entity.Usuario;
import com.sistema.java.model.enums.PapelUsuario;
import com.sistema.java.service.AuthService;
import com.sistema.java.service.UsuarioService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Managed Bean para administração de usuários
 * Referência: Controle de Acesso - project_rules.md
 * Referência: Padrões de Desenvolvimento - project_rules.md
 */
@Component("usuarioAdminBean")
@Scope("view")
public class UsuarioAdminBean implements Serializable {
    
    private static final Logger logger = LoggerFactory.getLogger(UsuarioAdminBean.class);
    
    @Autowired
    private UsuarioService usuarioService;
    
    @Autowired
    private AuthService authService;
    
    // Estado do componente
    private List<Usuario> usuarios;
    private List<Usuario> usuariosFiltrados;
    private Usuario usuarioSelecionado;
    private Usuario novoUsuario;
    private boolean dialogoEdicaoAberto = false;
    private boolean dialogoNovoUsuarioAberto = false;
    
    // Filtros
    private String filtroNome;
    private String filtroEmail;
    private PapelUsuario filtroPapel;
    private Boolean filtroAtivo;
    
    @PostConstruct
    public void init() {
        try {
            logger.info("Inicializando UsuarioAdminBean");
            verificarPermissoes();
            carregarUsuarios();
            inicializarNovoUsuario();
        } catch (Exception e) {
            logger.error("Erro ao inicializar UsuarioAdminBean", e);
            adicionarMensagemErro("Erro ao carregar dados de usuários");
        }
    }
    
    /**
     * Verifica se o usuário atual tem permissões para administrar usuários
     * Referência: Controle de Acesso - project_rules.md
     */
    private void verificarPermissoes() {
        Usuario usuarioAtual = authService.getUsuarioLogado();
        if (usuarioAtual == null || 
            (!usuarioAtual.getPapel().equals(PapelUsuario.ADMINISTRADOR) && 
             !usuarioAtual.getPapel().equals(PapelUsuario.FUNDADOR))) {
            
            logger.warn("Acesso negado para administração de usuários. Usuário: {}", 
                       usuarioAtual != null ? usuarioAtual.getEmail() : "não logado");
            
            adicionarMensagemErro("Acesso negado. Você não tem permissão para administrar usuários.");
            throw new SecurityException("Acesso negado para administração de usuários");
        }
    }
    
    /**
     * Carrega todos os usuários do sistema
     */
    public void carregarUsuarios() {
        try {
            logger.debug("Carregando lista de usuários");
            usuarios = usuarioService.listarTodosUsuarios();
            usuariosFiltrados = new ArrayList<>(usuarios);
            logger.info("Carregados {} usuários", usuarios.size());
        } catch (Exception e) {
            logger.error("Erro ao carregar usuários", e);
            adicionarMensagemErro("Erro ao carregar lista de usuários");
            usuarios = new ArrayList<>();
            usuariosFiltrados = new ArrayList<>();
        }
    }
    
    /**
     * Aplica filtros na lista de usuários
     */
    public void aplicarFiltros() {
        try {
            logger.debug("Aplicando filtros: nome={}, email={}, papel={}, ativo={}", 
                        filtroNome, filtroEmail, filtroPapel, filtroAtivo);
            
            usuariosFiltrados = usuarios.stream()
                .filter(u -> filtroNome == null || filtroNome.isEmpty() || 
                           u.getNome().toLowerCase().contains(filtroNome.toLowerCase()) ||
                           u.getSobrenome().toLowerCase().contains(filtroNome.toLowerCase()))
                .filter(u -> filtroEmail == null || filtroEmail.isEmpty() || 
                           u.getEmail().toLowerCase().contains(filtroEmail.toLowerCase()))
                .filter(u -> filtroPapel == null || u.getPapel().equals(filtroPapel))
                .filter(u -> filtroAtivo == null || u.getAtivo().equals(filtroAtivo))
                .toList();
                
            logger.debug("Filtros aplicados. {} usuários encontrados", usuariosFiltrados.size());
        } catch (Exception e) {
            logger.error("Erro ao aplicar filtros", e);
            adicionarMensagemErro("Erro ao filtrar usuários");
        }
    }
    
    /**
     * Limpa todos os filtros
     */
    public void limparFiltros() {
        logger.debug("Limpando filtros");
        filtroNome = null;
        filtroEmail = null;
        filtroPapel = null;
        filtroAtivo = null;
        usuariosFiltrados = new ArrayList<>(usuarios);
    }
    
    /**
     * Prepara para editar um usuário
     */
    public void editarUsuario(Usuario usuario) {
        try {
            logger.info("Preparando edição do usuário: {}", usuario.getEmail());
            
            // Verificar se pode editar este usuário
            if (!podeEditarUsuario(usuario)) {
                adicionarMensagemErro("Você não tem permissão para editar este usuário");
                return;
            }
            
            usuarioSelecionado = new Usuario();
            // Copiar dados (evitar referência direta)
            usuarioSelecionado.setId(usuario.getId());
            usuarioSelecionado.setNome(usuario.getNome());
            usuarioSelecionado.setSobrenome(usuario.getSobrenome());
            usuarioSelecionado.setEmail(usuario.getEmail());
            usuarioSelecionado.setCpf(usuario.getCpf());
            usuarioSelecionado.setTelefone(usuario.getTelefone());
            usuarioSelecionado.setDataNascimento(usuario.getDataNascimento());
            usuarioSelecionado.setPapel(usuario.getPapel());
            usuarioSelecionado.setAtivo(usuario.getAtivo());
            
            dialogoEdicaoAberto = true;
            
        } catch (Exception e) {
            logger.error("Erro ao preparar edição do usuário", e);
            adicionarMensagemErro("Erro ao preparar edição do usuário");
        }
    }
    
    /**
     * Verifica se o usuário atual pode editar o usuário especificado
     * Referência: Controle de Acesso - project_rules.md
     */
    private boolean podeEditarUsuario(Usuario usuario) {
        Usuario usuarioAtual = authService.getUsuarioLogado();
        
        // ADMINISTRADOR pode editar todos exceto FUNDADOR
        if (usuarioAtual.getPapel().equals(PapelUsuario.ADMINISTRADOR)) {
            return !usuario.getPapel().equals(PapelUsuario.FUNDADOR);
        }
        
        // FUNDADOR pode editar todos
        if (usuarioAtual.getPapel().equals(PapelUsuario.FUNDADOR)) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Salva as alterações do usuário
     */
    public void salvarUsuario() {
        try {
            if (usuarioSelecionado == null) {
                adicionarMensagemErro("Nenhum usuário selecionado");
                return;
            }
            
            logger.info("Salvando alterações do usuário: {}", usuarioSelecionado.getEmail());
            
            // Validar dados
            if (!validarUsuario(usuarioSelecionado)) {
                return;
            }
            
            // Atualizar usuário
            usuarioService.atualizarUsuario(usuarioSelecionado);
            
            // Recarregar lista
            carregarUsuarios();
            aplicarFiltros();
            
            dialogoEdicaoAberto = false;
            usuarioSelecionado = null;
            
            adicionarMensagemSucesso("Usuário atualizado com sucesso");
            
        } catch (Exception e) {
            logger.error("Erro ao salvar usuário", e);
            adicionarMensagemErro("Erro ao salvar usuário: " + e.getMessage());
        }
    }
    
    /**
     * Prepara para criar um novo usuário
     */
    public void novoUsuario() {
        logger.info("Preparando criação de novo usuário");
        inicializarNovoUsuario();
        dialogoNovoUsuarioAberto = true;
    }
    
    /**
     * Inicializa objeto para novo usuário
     */
    private void inicializarNovoUsuario() {
        novoUsuario = new Usuario();
        novoUsuario.setPapel(PapelUsuario.USUARIO); // Papel padrão
        novoUsuario.setAtivo(true); // Ativo por padrão
    }
    
    /**
     * Cria um novo usuário
     */
    public void criarUsuario() {
        try {
            if (novoUsuario == null) {
                adicionarMensagemErro("Dados do usuário inválidos");
                return;
            }
            
            logger.info("Criando novo usuário: {}", novoUsuario.getEmail());
            
            // Validar dados
            if (!validarUsuario(novoUsuario)) {
                return;
            }
            
            // Verificar se pode criar usuário com este papel
            if (!podeAtribuirPapel(novoUsuario.getPapel())) {
                adicionarMensagemErro("Você não tem permissão para atribuir este papel");
                return;
            }
            
            // Criar usuário
            usuarioService.criarUsuario(novoUsuario);
            
            // Recarregar lista
            carregarUsuarios();
            aplicarFiltros();
            
            dialogoNovoUsuarioAberto = false;
            inicializarNovoUsuario();
            
            adicionarMensagemSucesso("Usuário criado com sucesso");
            
        } catch (Exception e) {
            logger.error("Erro ao criar usuário", e);
            adicionarMensagemErro("Erro ao criar usuário: " + e.getMessage());
        }
    }
    
    /**
     * Verifica se o usuário atual pode atribuir o papel especificado
     * Referência: Controle de Acesso - project_rules.md
     */
    private boolean podeAtribuirPapel(PapelUsuario papel) {
        Usuario usuarioAtual = authService.getUsuarioLogado();
        
        // FUNDADOR pode atribuir qualquer papel
        if (usuarioAtual.getPapel().equals(PapelUsuario.FUNDADOR)) {
            return true;
        }
        
        // ADMINISTRADOR pode atribuir todos exceto FUNDADOR
        if (usuarioAtual.getPapel().equals(PapelUsuario.ADMINISTRADOR)) {
            return !papel.equals(PapelUsuario.FUNDADOR);
        }
        
        return false;
    }
    
    /**
     * Valida os dados do usuário
     */
    private boolean validarUsuario(Usuario usuario) {
        if (usuario.getNome() == null || usuario.getNome().trim().isEmpty()) {
            adicionarMensagemErro("Nome é obrigatório");
            return false;
        }
        
        if (usuario.getSobrenome() == null || usuario.getSobrenome().trim().isEmpty()) {
            adicionarMensagemErro("Sobrenome é obrigatório");
            return false;
        }
        
        if (usuario.getEmail() == null || usuario.getEmail().trim().isEmpty()) {
            adicionarMensagemErro("Email é obrigatório");
            return false;
        }
        
        if (usuario.getCpf() == null || usuario.getCpf().trim().isEmpty()) {
            adicionarMensagemErro("CPF é obrigatório");
            return false;
        }
        
        if (usuario.getPapel() == null) {
            adicionarMensagemErro("Papel é obrigatório");
            return false;
        }
        
        return true;
    }
    
    /**
     * Alterna o status ativo/inativo do usuário
     */
    public void alternarStatusUsuario(Usuario usuario) {
        try {
            if (!podeEditarUsuario(usuario)) {
                adicionarMensagemErro("Você não tem permissão para alterar este usuário");
                return;
            }
            
            boolean novoStatus = !usuario.getAtivo();
            logger.info("Alterando status do usuário {} para: {}", 
                       usuario.getEmail(), novoStatus ? "ativo" : "inativo");
            
            usuario.setAtivo(novoStatus);
            usuarioService.atualizarUsuario(usuario);
            
            String mensagem = novoStatus ? "Usuário ativado" : "Usuário desativado";
            adicionarMensagemSucesso(mensagem + " com sucesso");
            
        } catch (Exception e) {
            logger.error("Erro ao alterar status do usuário", e);
            adicionarMensagemErro("Erro ao alterar status do usuário");
        }
    }
    
    /**
     * Cancela a edição do usuário
     */
    public void cancelarEdicao() {
        logger.debug("Cancelando edição de usuário");
        dialogoEdicaoAberto = false;
        usuarioSelecionado = null;
    }
    
    /**
     * Cancela a criação de novo usuário
     */
    public void cancelarNovoUsuario() {
        logger.debug("Cancelando criação de novo usuário");
        dialogoNovoUsuarioAberto = false;
        inicializarNovoUsuario();
    }
    
    /**
     * Retorna lista de papéis disponíveis para atribuição
     */
    public List<PapelUsuario> getPapeisDisponiveis() {
        Usuario usuarioAtual = authService.getUsuarioLogado();
        
        if (usuarioAtual.getPapel().equals(PapelUsuario.FUNDADOR)) {
            return Arrays.asList(PapelUsuario.values());
        }
        
        if (usuarioAtual.getPapel().equals(PapelUsuario.ADMINISTRADOR)) {
            return Arrays.asList(
                PapelUsuario.ADMINISTRADOR,
                PapelUsuario.COLABORADOR,
                PapelUsuario.ASSOCIADO,
                PapelUsuario.USUARIO,
                PapelUsuario.CONVIDADO
            );
        }
        
        return Arrays.asList(PapelUsuario.USUARIO, PapelUsuario.CONVIDADO);
    }
    
    /**
     * Retorna o total de usuários
     */
    public int getTotalUsuarios() {
        return usuarios != null ? usuarios.size() : 0;
    }
    
    /**
     * Retorna o total de usuários ativos
     */
    public int getTotalUsuariosAtivos() {
        if (usuarios == null) return 0;
        return (int) usuarios.stream().filter(Usuario::getAtivo).count();
    }
    
    /**
     * Retorna o total de usuários inativos
     */
    public int getTotalUsuariosInativos() {
        return getTotalUsuarios() - getTotalUsuariosAtivos();
    }
    
    // Métodos utilitários para mensagens
    private void adicionarMensagemSucesso(String mensagem) {
        FacesContext.getCurrentInstance().addMessage(null, 
            new FacesMessage(FacesMessage.SEVERITY_INFO, "Sucesso", mensagem));
    }
    
    private void adicionarMensagemErro(String mensagem) {
        FacesContext.getCurrentInstance().addMessage(null, 
            new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro", mensagem));
    }
    
    // Getters e Setters
    public List<Usuario> getUsuarios() {
        return usuarios;
    }
    
    public void setUsuarios(List<Usuario> usuarios) {
        this.usuarios = usuarios;
    }
    
    public List<Usuario> getUsuariosFiltrados() {
        return usuariosFiltrados;
    }
    
    public void setUsuariosFiltrados(List<Usuario> usuariosFiltrados) {
        this.usuariosFiltrados = usuariosFiltrados;
    }
    
    public Usuario getUsuarioSelecionado() {
        return usuarioSelecionado;
    }
    
    public void setUsuarioSelecionado(Usuario usuarioSelecionado) {
        this.usuarioSelecionado = usuarioSelecionado;
    }
    
    public Usuario getNovoUsuario() {
        return novoUsuario;
    }
    
    public void setNovoUsuario(Usuario novoUsuario) {
        this.novoUsuario = novoUsuario;
    }
    
    public boolean isDialogoEdicaoAberto() {
        return dialogoEdicaoAberto;
    }
    
    public void setDialogoEdicaoAberto(boolean dialogoEdicaoAberto) {
        this.dialogoEdicaoAberto = dialogoEdicaoAberto;
    }
    
    public boolean isDialogoNovoUsuarioAberto() {
        return dialogoNovoUsuarioAberto;
    }
    
    public void setDialogoNovoUsuarioAberto(boolean dialogoNovoUsuarioAberto) {
        this.dialogoNovoUsuarioAberto = dialogoNovoUsuarioAberto;
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
    
    public PapelUsuario getFiltroPapel() {
        return filtroPapel;
    }
    
    public void setFiltroPapel(PapelUsuario filtroPapel) {
        this.filtroPapel = filtroPapel;
    }
    
    public Boolean getFiltroAtivo() {
        return filtroAtivo;
    }
    
    public void setFiltroAtivo(Boolean filtroAtivo) {
        this.filtroAtivo = filtroAtivo;
    }
}