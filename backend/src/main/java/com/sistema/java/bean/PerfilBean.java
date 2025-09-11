package com.sistema.java.bean;

import com.sistema.java.model.Usuario;
import com.sistema.java.service.AuthService;
import com.sistema.java.service.FileUploadService;
import com.sistema.java.service.UsuarioService;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.file.UploadedFile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.Period;

@Component
@Scope("view")
public class PerfilBean implements Serializable {
    
    @Autowired
    private AuthService authService;
    
    @Autowired
    private UsuarioService usuarioService;
    
    @Autowired
    private FileUploadService fileUploadService;
    
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    
    // Dados do usuário
    private Usuario usuario;
    private Usuario usuarioOriginal;
    
    // Campos para alteração de senha
    private String senhaAtual;
    private String novaSenha;
    private String confirmaNovaSenha;
    
    // Estado da interface
    private boolean editandoPerfil = false;
    private boolean alterandoSenha = false;
    private boolean avatarCarregado = false;
    
    @PostConstruct
    public void init() {
        carregarDadosUsuario();
    }
    
    /**
     * Carrega dados do usuário logado
     */
    public void carregarDadosUsuario() {
        Usuario usuarioLogado = authService.getUsuarioLogado();
        if (usuarioLogado != null) {
            // Recarregar dados atualizados do banco
            this.usuario = usuarioService.findById(usuarioLogado.getId());
            this.usuarioOriginal = cloneUsuario(this.usuario);
        } else {
            addErrorMessage("Usuário não encontrado. Faça login novamente.");
        }
    }
    
    /**
     * Inicia edição do perfil
     */
    public void iniciarEdicao() {
        editandoPerfil = true;
        usuarioOriginal = cloneUsuario(usuario);
    }
    
    /**
     * Cancela edição do perfil
     */
    public void cancelarEdicao() {
        editandoPerfil = false;
        usuario = cloneUsuario(usuarioOriginal);
        limparCamposSenha();
    }
    
    /**
     * Salva alterações do perfil
     */
    public void salvarPerfil() {
        try {
            if (!validarDadosPerfil()) {
                return;
            }
            
            // Atualizar dados
            Usuario usuarioAtualizado = usuarioService.update(usuario);
            
            if (usuarioAtualizado != null) {
                this.usuario = usuarioAtualizado;
                this.usuarioOriginal = cloneUsuario(usuarioAtualizado);
                editandoPerfil = false;
                
                addInfoMessage("Perfil atualizado com sucesso!");
            } else {
                addErrorMessage("Erro ao atualizar perfil.");
            }
            
        } catch (Exception e) {
            addErrorMessage("Erro ao salvar perfil: " + e.getMessage());
        }
    }
    
    /**
     * Inicia alteração de senha
     */
    public void iniciarAlteracaoSenha() {
        alterandoSenha = true;
        limparCamposSenha();
    }
    
    /**
     * Cancela alteração de senha
     */
    public void cancelarAlteracaoSenha() {
        alterandoSenha = false;
        limparCamposSenha();
    }
    
    /**
     * Altera senha do usuário
     */
    public void alterarSenha() {
        try {
            if (!validarAlteracaoSenha()) {
                return;
            }
            
            // Verificar senha atual
            if (!passwordEncoder.matches(senhaAtual, usuario.getSenha())) {
                addErrorMessage("Senha atual incorreta.");
                return;
            }
            
            // Atualizar senha
            usuario.setSenha(passwordEncoder.encode(novaSenha));
            Usuario usuarioAtualizado = usuarioService.update(usuario);
            
            if (usuarioAtualizado != null) {
                this.usuario = usuarioAtualizado;
                alterandoSenha = false;
                limparCamposSenha();
                
                addInfoMessage("Senha alterada com sucesso!");
            } else {
                addErrorMessage("Erro ao alterar senha.");
            }
            
        } catch (Exception e) {
            addErrorMessage("Erro ao alterar senha: " + e.getMessage());
        }
    }
    
    /**
     * Processa upload de avatar
     */
    public void handleFileUpload(FileUploadEvent event) {
        try {
            UploadedFile file = event.getFile();
            
            if (file != null && file.getContent().length > 0) {
                // Remover avatar anterior se existir
                if (usuario.getAvatar() != null && !usuario.getAvatar().trim().isEmpty()) {
                    fileUploadService.removeFile(usuario.getAvatar());
                }
                
                // Fazer upload do novo avatar
                String avatarPath = fileUploadService.uploadAvatar(
                    convertToMultipartFile(file), usuario.getId()
                );
                
                // Atualizar usuário
                usuario.setAvatar(avatarPath);
                Usuario usuarioAtualizado = usuarioService.update(usuario);
                
                if (usuarioAtualizado != null) {
                    this.usuario = usuarioAtualizado;
                    avatarCarregado = true;
                    
                    addInfoMessage("Avatar atualizado com sucesso!");
                } else {
                    addErrorMessage("Erro ao salvar avatar.");
                }
            }
            
        } catch (Exception e) {
            addErrorMessage("Erro ao fazer upload do avatar: " + e.getMessage());
        }
    }
    
    /**
     * Remove avatar do usuário
     */
    public void removerAvatar() {
        try {
            if (usuario.getAvatar() != null && !usuario.getAvatar().trim().isEmpty()) {
                // Remover arquivo
                fileUploadService.removeFile(usuario.getAvatar());
                
                // Atualizar usuário
                usuario.setAvatar(null);
                Usuario usuarioAtualizado = usuarioService.update(usuario);
                
                if (usuarioAtualizado != null) {
                    this.usuario = usuarioAtualizado;
                    avatarCarregado = false;
                    
                    addInfoMessage("Avatar removido com sucesso!");
                } else {
                    addErrorMessage("Erro ao remover avatar.");
                }
            }
            
        } catch (Exception e) {
            addErrorMessage("Erro ao remover avatar: " + e.getMessage());
        }
    }
    
    /**
     * Valida idade mínima (18 anos)
     */
    public void validarIdadeMinima() {
        if (usuario.getDataNascimento() != null) {
            LocalDate hoje = LocalDate.now();
            LocalDate dataNascimento = usuario.getDataNascimento();
            
            if (dataNascimento.plusYears(18).isAfter(hoje)) {
                addErrorMessage("Você deve ter pelo menos 18 anos.");
                usuario.setDataNascimento(usuarioOriginal.getDataNascimento());
            }
        }
    }
    
    /**
     * Formata CPF durante digitação
     */
    public void formatarCpf() {
        if (usuario.getCpf() != null) {
            String cpf = usuario.getCpf().replaceAll("\\D", "");
            if (cpf.length() <= 11) {
                usuario.setCpf(cpf);
            } else {
                usuario.setCpf(cpf.substring(0, 11));
            }
        }
    }
    
    /**
     * Formata telefone durante digitação
     */
    public void formatarTelefone() {
        if (usuario.getTelefone() != null) {
            String telefone = usuario.getTelefone().replaceAll("\\D", "");
            if (telefone.length() <= 11) {
                usuario.setTelefone(telefone);
            } else {
                usuario.setTelefone(telefone.substring(0, 11));
            }
        }
    }
    
    /**
     * Obtém idade do usuário
     */
    public Integer getIdade() {
        if (usuario.getDataNascimento() != null) {
            return Period.between(usuario.getDataNascimento(), LocalDate.now()).getYears();
        }
        return null;
    }
    
    /**
     * Obtém URL do avatar
     */
    public String getAvatarUrl() {
        if (usuario.getAvatar() != null && !usuario.getAvatar().trim().isEmpty()) {
            return fileUploadService.getFileUrl(usuario.getAvatar());
        }
        return "/resources/images/default-avatar.png";
    }
    
    /**
     * Verifica se usuário tem avatar
     */
    public boolean hasAvatar() {
        return usuario.getAvatar() != null && !usuario.getAvatar().trim().isEmpty();
    }
    
    // Métodos privados
    
    private boolean validarDadosPerfil() {
        if (usuario.getNome() == null || usuario.getNome().trim().length() < 2) {
            addErrorMessage("Nome deve ter pelo menos 2 caracteres.");
            return false;
        }
        
        if (usuario.getEmail() == null || !usuario.getEmail().matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")) {
            addErrorMessage("Email deve ter formato válido.");
            return false;
        }
        
        if (usuario.getCpf() != null && !usuario.getCpf().trim().isEmpty()) {
            if (!usuario.getCpf().matches("^\\d{11}$")) {
                addErrorMessage("CPF deve ter exatamente 11 dígitos.");
                return false;
            }
            
            // Verificar se CPF já existe para outro usuário
            if (!usuario.getCpf().equals(usuarioOriginal.getCpf())) {
                if (usuarioService.existsByCpfAndIdNot(usuario.getCpf(), usuario.getId())) {
                    addErrorMessage("CPF já cadastrado para outro usuário.");
                    return false;
                }
            }
        }
        
        if (usuario.getTelefone() != null && !usuario.getTelefone().trim().isEmpty()) {
            if (!usuario.getTelefone().matches("^\\d{10,11}$")) {
                addErrorMessage("Telefone deve ter 10 ou 11 dígitos.");
                return false;
            }
        }
        
        // Verificar se email já existe para outro usuário
        if (!usuario.getEmail().equals(usuarioOriginal.getEmail())) {
            if (usuarioService.existsByEmailAndIdNot(usuario.getEmail(), usuario.getId())) {
                addErrorMessage("Email já cadastrado para outro usuário.");
                return false;
            }
        }
        
        return true;
    }
    
    private boolean validarAlteracaoSenha() {
        if (senhaAtual == null || senhaAtual.trim().isEmpty()) {
            addErrorMessage("Senha atual é obrigatória.");
            return false;
        }
        
        if (novaSenha == null || novaSenha.length() < 8) {
            addErrorMessage("Nova senha deve ter pelo menos 8 caracteres.");
            return false;
        }
        
        if (!novaSenha.equals(confirmaNovaSenha)) {
            addErrorMessage("Confirmação de senha não confere.");
            return false;
        }
        
        if (senhaAtual.equals(novaSenha)) {
            addErrorMessage("Nova senha deve ser diferente da atual.");
            return false;
        }
        
        return true;
    }
    
    private Usuario cloneUsuario(Usuario original) {
        if (original == null) return null;
        
        Usuario clone = new Usuario();
        clone.setId(original.getId());
        clone.setNome(original.getNome());
        clone.setSobrenome(original.getSobrenome());
        clone.setEmail(original.getEmail());
        clone.setSenha(original.getSenha());
        clone.setCpf(original.getCpf());
        clone.setTelefone(original.getTelefone());
        clone.setDataNascimento(original.getDataNascimento());
        clone.setAvatar(original.getAvatar());
        clone.setPapel(original.getPapel());
        clone.setAtivo(original.getAtivo());
        clone.setEmailVerificado(original.getEmailVerificado());
        clone.setDataCriacao(original.getDataCriacao());
        clone.setDataAtualizacao(original.getDataAtualizacao());
        clone.setUltimoAcesso(original.getUltimoAcesso());
        
        return clone;
    }
    
    private void limparCamposSenha() {
        senhaAtual = "";
        novaSenha = "";
        confirmaNovaSenha = "";
    }
    
    private org.springframework.web.multipart.MultipartFile convertToMultipartFile(UploadedFile file) {
        return new org.springframework.web.multipart.MultipartFile() {
            @Override
            public String getName() {
                return file.getFileName();
            }
            
            @Override
            public String getOriginalFilename() {
                return file.getFileName();
            }
            
            @Override
            public String getContentType() {
                return file.getContentType();
            }
            
            @Override
            public boolean isEmpty() {
                return file.getContent().length == 0;
            }
            
            @Override
            public long getSize() {
                return file.getContent().length;
            }
            
            @Override
            public byte[] getBytes() {
                return file.getContent();
            }
            
            @Override
            public java.io.InputStream getInputStream() {
                return new java.io.ByteArrayInputStream(file.getContent());
            }
            
            @Override
            public void transferTo(java.io.File dest) throws java.io.IOException {
                throw new UnsupportedOperationException();
            }
        };
    }
    
    // Getters e Setters
    
    public Usuario getUsuario() {
        return usuario;
    }
    
    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }
    
    public String getSenhaAtual() {
        return senhaAtual;
    }
    
    public void setSenhaAtual(String senhaAtual) {
        this.senhaAtual = senhaAtual;
    }
    
    public String getNovaSenha() {
        return novaSenha;
    }
    
    public void setNovaSenha(String novaSenha) {
        this.novaSenha = novaSenha;
    }
    
    public String getConfirmaNovaSenha() {
        return confirmaNovaSenha;
    }
    
    public void setConfirmaNovaSenha(String confirmaNovaSenha) {
        this.confirmaNovaSenha = confirmaNovaSenha;
    }
    
    public boolean isEditandoPerfil() {
        return editandoPerfil;
    }
    
    public void setEditandoPerfil(boolean editandoPerfil) {
        this.editandoPerfil = editandoPerfil;
    }
    
    public boolean isAlterandoSenha() {
        return alterandoSenha;
    }
    
    public void setAlterandoSenha(boolean alterandoSenha) {
        this.alterandoSenha = alterandoSenha;
    }
    
    public boolean isAvatarCarregado() {
        return avatarCarregado;
    }
    
    public void setAvatarCarregado(boolean avatarCarregado) {
        this.avatarCarregado = avatarCarregado;
    }
    
    // Métodos utilitários
    
    private void addErrorMessage(String message) {
        FacesContext.getCurrentInstance().addMessage(null, 
            new FacesMessage(FacesMessage.SEVERITY_ERROR, "Erro", message));
    }
    
    private void addInfoMessage(String message) {
        FacesContext.getCurrentInstance().addMessage(null, 
            new FacesMessage(FacesMessage.SEVERITY_INFO, "Sucesso", message));
    }
}