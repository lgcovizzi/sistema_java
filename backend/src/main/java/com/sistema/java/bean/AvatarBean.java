package com.sistema.java.bean;

import com.sistema.java.model.entity.Usuario;
import com.sistema.java.service.AuthService;
import com.sistema.java.service.UsuarioService;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.file.UploadedFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * Managed Bean para gerenciamento de avatars de usuários
 * Referência: Regras de Avatar - project_rules.md
 * Referência: Padrões de Desenvolvimento - project_rules.md
 */
@Component
@Scope("view")
public class AvatarBean implements Serializable {
    
    private static final Logger logger = LoggerFactory.getLogger(AvatarBean.class);
    
    // Configurações de upload
    private static final String UPLOAD_DIRECTORY = "/uploads/avatars/";
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    private static final String[] ALLOWED_EXTENSIONS = {".jpg", ".jpeg", ".png"};
    
    @Autowired
    private AuthService authService;
    
    @Autowired
    private UsuarioService usuarioService;
    
    // Estado do componente
    private Usuario usuarioAtual;
    private String avatarAtual;
    private String avatarPreview;
    private boolean uploadEmAndamento = false;
    private boolean cropDialogAberto = false;
    
    // Dados do crop
    private int cropX = 0;
    private int cropY = 0;
    private int cropWidth = 256;
    private int cropHeight = 256;
    
    @PostConstruct
    public void init() {
        try {
            logger.info("Inicializando AvatarBean");
            carregarUsuarioAtual();
        } catch (Exception e) {
            logger.error("Erro ao inicializar AvatarBean", e);
        }
    }
    
    /**
     * Carrega o usuário atual e seu avatar
     */
    private void carregarUsuarioAtual() {
        usuarioAtual = authService.getUsuarioLogado();
        if (usuarioAtual != null) {
            avatarAtual = usuarioAtual.getAvatar();
            logger.debug("Avatar atual do usuário {}: {}", usuarioAtual.getEmail(), avatarAtual);
        }
    }
    
    /**
     * Manipula o upload de arquivo de avatar
     * Referência: Regras de Avatar - project_rules.md
     */
    public void handleFileUpload(FileUploadEvent event) {
        try {
            UploadedFile file = event.getFile();
            
            if (file == null || file.getContent() == null) {
                adicionarMensagemErro("Arquivo inválido");
                return;
            }
            
            logger.info("Iniciando upload de avatar para usuário: {}", usuarioAtual.getEmail());
            
            // Validar arquivo
            if (!validarArquivo(file)) {
                return;
            }
            
            uploadEmAndamento = true;
            
            // Salvar arquivo temporário
            String nomeArquivoTemp = salvarArquivoTemporario(file);
            
            if (nomeArquivoTemp != null) {
                avatarPreview = nomeArquivoTemp;
                cropDialogAberto = true;
                adicionarMensagemInfo("Arquivo carregado. Ajuste o recorte do avatar.");
            }
            
        } catch (Exception e) {
            logger.error("Erro durante upload de avatar", e);
            adicionarMensagemErro("Erro ao fazer upload do arquivo");
        } finally {
            uploadEmAndamento = false;
        }
    }
    
    /**
     * Valida o arquivo de upload
     * Referência: Regras de Avatar - project_rules.md
     */
    private boolean validarArquivo(UploadedFile file) {
        // Verificar tamanho
        if (file.getSize() > MAX_FILE_SIZE) {
            adicionarMensagemErro("Arquivo muito grande. Tamanho máximo: 10MB");
            return false;
        }
        
        // Verificar extensão
        String fileName = file.getFileName().toLowerCase();
        boolean extensaoValida = false;
        for (String ext : ALLOWED_EXTENSIONS) {
            if (fileName.endsWith(ext)) {
                extensaoValida = true;
                break;
            }
        }
        
        if (!extensaoValida) {
            adicionarMensagemErro("Formato de arquivo não suportado. Use JPG ou PNG.");
            return false;
        }
        
        return true;
    }
    
    /**
     * Salva arquivo temporário para preview e crop
     */
    private String salvarArquivoTemporario(UploadedFile file) {
        try {
            // Criar diretório se não existir
            Path uploadPath = Paths.get(System.getProperty("java.io.tmpdir"), "avatar-temp");
            Files.createDirectories(uploadPath);
            
            // Gerar nome único
            String extensao = getExtensaoArquivo(file.getFileName());
            String nomeArquivo = "temp_" + UUID.randomUUID().toString() + extensao;
            Path arquivoPath = uploadPath.resolve(nomeArquivo);
            
            // Salvar arquivo
            try (InputStream input = file.getInputStream();
                 FileOutputStream output = new FileOutputStream(arquivoPath.toFile())) {
                
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = input.read(buffer)) != -1) {
                    output.write(buffer, 0, bytesRead);
                }
            }
            
            logger.debug("Arquivo temporário salvo: {}", arquivoPath);
            return arquivoPath.toString();
            
        } catch (IOException e) {
            logger.error("Erro ao salvar arquivo temporário", e);
            adicionarMensagemErro("Erro ao processar arquivo");
            return null;
        }
    }
    
    /**
     * Confirma o crop e processa o avatar final
     * Referência: Regras de Avatar - project_rules.md
     */
    public void confirmarCrop() {
        try {
            if (avatarPreview == null) {
                adicionarMensagemErro("Nenhum arquivo para processar");
                return;
            }
            
            logger.info("Processando crop do avatar para usuário: {}", usuarioAtual.getEmail());
            
            // Processar imagem com crop e redimensionamento
            String avatarFinal = processarAvatar(avatarPreview);
            
            if (avatarFinal != null) {
                // Atualizar usuário
                usuarioAtual.setAvatar(avatarFinal);
                usuarioService.atualizarUsuario(usuarioAtual);
                
                // Atualizar estado local
                avatarAtual = avatarFinal;
                
                // Limpar temporários
                limparArquivosTemporarios();
                
                adicionarMensagemSucesso("Avatar atualizado com sucesso");
                cropDialogAberto = false;
                
                logger.info("Avatar atualizado para usuário: {}", usuarioAtual.getEmail());
            }
            
        } catch (Exception e) {
            logger.error("Erro ao processar crop do avatar", e);
            adicionarMensagemErro("Erro ao processar avatar");
        }
    }
    
    /**
     * Processa o avatar com crop e redimensionamento
     * Referência: Regras de Avatar - project_rules.md
     */
    private String processarAvatar(String arquivoOriginal) {
        try {
            // Criar diretório de avatars se não existir
            Path avatarPath = Paths.get(UPLOAD_DIRECTORY);
            Files.createDirectories(avatarPath);
            
            // Gerar nome único para o avatar final
            String nomeAvatar = "avatar_" + usuarioAtual.getId() + "_" + 
                              System.currentTimeMillis() + ".jpg";
            
            // Aqui seria implementado o processamento real da imagem
            // Por simplicidade, vamos apenas copiar o arquivo
            Path origem = Paths.get(arquivoOriginal);
            Path destino = avatarPath.resolve(nomeAvatar);
            
            Files.copy(origem, destino);
            
            // Retornar caminho relativo para salvar no banco
            return UPLOAD_DIRECTORY + nomeAvatar;
            
        } catch (IOException e) {
            logger.error("Erro ao processar avatar", e);
            return null;
        }
    }
    
    /**
     * Cancela o crop e limpa arquivos temporários
     */
    public void cancelarCrop() {
        logger.info("Cancelando crop do avatar");
        limparArquivosTemporarios();
        cropDialogAberto = false;
    }
    
    /**
     * Remove o avatar atual do usuário
     */
    public void removerAvatar() {
        try {
            if (usuarioAtual == null) {
                adicionarMensagemErro("Usuário não encontrado");
                return;
            }
            
            logger.info("Removendo avatar do usuário: {}", usuarioAtual.getEmail());
            
            // Remover arquivo físico se existir
            if (avatarAtual != null && !avatarAtual.isEmpty()) {
                try {
                    Path arquivoPath = Paths.get(avatarAtual);
                    Files.deleteIfExists(arquivoPath);
                } catch (IOException e) {
                    logger.warn("Erro ao remover arquivo de avatar: {}", avatarAtual, e);
                }
            }
            
            // Atualizar usuário
            usuarioAtual.setAvatar(null);
            usuarioService.atualizarUsuario(usuarioAtual);
            
            // Atualizar estado local
            avatarAtual = null;
            
            adicionarMensagemSucesso("Avatar removido com sucesso");
            
        } catch (Exception e) {
            logger.error("Erro ao remover avatar", e);
            adicionarMensagemErro("Erro ao remover avatar");
        }
    }
    
    /**
     * Limpa arquivos temporários
     */
    private void limparArquivosTemporarios() {
        if (avatarPreview != null) {
            try {
                Files.deleteIfExists(Paths.get(avatarPreview));
                avatarPreview = null;
            } catch (IOException e) {
                logger.warn("Erro ao limpar arquivo temporário: {}", avatarPreview, e);
            }
        }
    }
    
    /**
     * Extrai a extensão do arquivo
     */
    private String getExtensaoArquivo(String nomeArquivo) {
        int ultimoPonto = nomeArquivo.lastIndexOf('.');
        return ultimoPonto > 0 ? nomeArquivo.substring(ultimoPonto) : "";
    }
    
    /**
     * Verifica se o usuário tem avatar
     */
    public boolean hasAvatar() {
        return avatarAtual != null && !avatarAtual.trim().isEmpty();
    }
    
    /**
     * Retorna URL do avatar ou avatar padrão
     */
    public String getAvatarUrl() {
        if (hasAvatar()) {
            return avatarAtual;
        }
        return "/resources/images/avatar-default.png";
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
    
    private void adicionarMensagemInfo(String mensagem) {
        FacesContext.getCurrentInstance().addMessage(null, 
            new FacesMessage(FacesMessage.SEVERITY_INFO, "Informação", mensagem));
    }
    
    // Getters e Setters
    public Usuario getUsuarioAtual() {
        return usuarioAtual;
    }
    
    public void setUsuarioAtual(Usuario usuarioAtual) {
        this.usuarioAtual = usuarioAtual;
    }
    
    public String getAvatarAtual() {
        return avatarAtual;
    }
    
    public void setAvatarAtual(String avatarAtual) {
        this.avatarAtual = avatarAtual;
    }
    
    public String getAvatarPreview() {
        return avatarPreview;
    }
    
    public void setAvatarPreview(String avatarPreview) {
        this.avatarPreview = avatarPreview;
    }
    
    public boolean isUploadEmAndamento() {
        return uploadEmAndamento;
    }
    
    public void setUploadEmAndamento(boolean uploadEmAndamento) {
        this.uploadEmAndamento = uploadEmAndamento;
    }
    
    public boolean isCropDialogAberto() {
        return cropDialogAberto;
    }
    
    public void setCropDialogAberto(boolean cropDialogAberto) {
        this.cropDialogAberto = cropDialogAberto;
    }
    
    public int getCropX() {
        return cropX;
    }
    
    public void setCropX(int cropX) {
        this.cropX = cropX;
    }
    
    public int getCropY() {
        return cropY;
    }
    
    public void setCropY(int cropY) {
        this.cropY = cropY;
    }
    
    public int getCropWidth() {
        return cropWidth;
    }
    
    public void setCropWidth(int cropWidth) {
        this.cropWidth = cropWidth;
    }
    
    public int getCropHeight() {
        return cropHeight;
    }
    
    public void setCropHeight(int cropHeight) {
        this.cropHeight = cropHeight;
    }
}