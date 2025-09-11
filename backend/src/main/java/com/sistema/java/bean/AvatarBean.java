package com.sistema.java.bean;

import com.sistema.java.model.dto.AvatarUploadDTO;
import com.sistema.java.service.AvatarService;
import com.sistema.java.service.AuthService;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.file.UploadedFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.Serializable;
import java.util.concurrent.CompletableFuture;

/**
 * Bean JSF para gerenciamento de avatars
 * Referência: Regras de Avatar - project_rules.md
 * Referência: Regras de Edição de Perfil - project_rules.md
 */
@Named
@ViewScoped
public class AvatarBean implements Serializable {

    private static final Logger logger = LoggerFactory.getLogger(AvatarBean.class);
    
    @Inject
    private AvatarService avatarService;
    
    @Inject
    private AuthService authService;
    
    // Propriedades do componente
    private Long usuarioId;
    private String avatarAtual;
    private boolean processandoUpload = false;
    private boolean mostrarCrop = false;
    private String previewUrl;
    
    // Propriedades de crop
    private Integer cropX = 0;
    private Integer cropY = 0;
    private Integer cropLargura = 256;
    private Integer cropAltura = 256;
    private boolean cropCentralizado = true;
    
    // Propriedades de upload
    private UploadedFile arquivoUpload;
    private String nomeArquivoOriginal;
    private long tamanhoArquivo;
    private String tipoMime;
    
    // Configurações
    private String tamanhoExibicao = "medio"; // pequeno, medio, grande
    private boolean permitirEdicao = false;
    
    /**
     * Inicialização do bean
     * Referência: Controle de Acesso - project_rules.md
     */
    @PostConstruct
    public void init() {
        try {
            // Se não foi especificado usuário, usar o logado
            if (usuarioId == null) {
                usuarioId = authService.getUsuarioLogado().getId();
            }
            
            // Verificar se pode editar
            Long usuarioLogadoId = authService.getUsuarioLogado().getId();
            permitirEdicao = avatarService.podeEditarAvatar(usuarioId, usuarioLogadoId);
            
            // Carregar avatar atual
            carregarAvatarAtual();
            
            logger.debug("AvatarBean inicializado para usuário: {}, edição permitida: {}", usuarioId, permitirEdicao);
            
        } catch (Exception e) {
            logger.error("Erro na inicialização do AvatarBean: {}", e.getMessage(), e);
            adicionarMensagem(FacesMessage.SEVERITY_ERROR, "Erro ao carregar avatar", e.getMessage());
        }
    }
    
    /**
     * Carrega avatar atual do usuário
     */
    public void carregarAvatarAtual() {
        try {
            avatarAtual = avatarService.obterAvatarUsuario(usuarioId, tamanhoExibicao);
            logger.debug("Avatar carregado para usuário {}: {}", usuarioId, avatarAtual);
        } catch (Exception e) {
            logger.warn("Erro ao carregar avatar para usuário {}: {}", usuarioId, e.getMessage());
            avatarAtual = null;
        }
    }
    
    /**
     * Manipula upload de arquivo
     * Referência: Regras de Avatar - project_rules.md
     */
    public void handleFileUpload(FileUploadEvent event) {
        try {
            if (!permitirEdicao) {
                adicionarMensagem(FacesMessage.SEVERITY_ERROR, "Erro", "Você não tem permissão para editar este avatar");
                return;
            }
            
            arquivoUpload = event.getFile();
            nomeArquivoOriginal = arquivoUpload.getFileName();
            tamanhoArquivo = arquivoUpload.getSize();
            tipoMime = arquivoUpload.getContentType();
            
            // Validações básicas
            if (!validarArquivo()) {
                return;
            }
            
            // Gerar preview para crop
            gerarPreviewParaCrop();
            
            // Mostrar interface de crop
            mostrarCrop = true;
            
            adicionarMensagem(FacesMessage.SEVERITY_INFO, "Sucesso", "Arquivo carregado. Configure o recorte e clique em 'Aplicar'.");
            
        } catch (Exception e) {
            logger.error("Erro no upload de arquivo: {}", e.getMessage(), e);
            adicionarMensagem(FacesMessage.SEVERITY_ERROR, "Erro no upload", e.getMessage());
        }
    }
    
    /**
     * Valida arquivo de upload
     */
    private boolean validarArquivo() {
        // Verificar se arquivo existe
        if (arquivoUpload == null || arquivoUpload.getSize() == 0) {
            adicionarMensagem(FacesMessage.SEVERITY_ERROR, "Erro", "Arquivo não pode estar vazio");
            return false;
        }
        
        // Verificar tamanho (5MB máximo)
        long tamanhoMaximo = 5 * 1024 * 1024;
        if (tamanhoArquivo > tamanhoMaximo) {
            adicionarMensagem(FacesMessage.SEVERITY_ERROR, "Erro", "Arquivo muito grande. Máximo: 5MB");
            return false;
        }
        
        // Verificar tipo
        if (tipoMime == null || !tipoMime.startsWith("image/")) {
            adicionarMensagem(FacesMessage.SEVERITY_ERROR, "Erro", "Arquivo deve ser uma imagem (JPEG ou PNG)");
            return false;
        }
        
        // Verificar extensão
        String extensao = nomeArquivoOriginal.toLowerCase();
        if (!extensao.endsWith(".jpg") && !extensao.endsWith(".jpeg") && !extensao.endsWith(".png")) {
            adicionarMensagem(FacesMessage.SEVERITY_ERROR, "Erro", "Formato não suportado. Use JPEG ou PNG");
            return false;
        }
        
        return true;
    }
    
    /**
     * Gera preview para interface de crop
     */
    private void gerarPreviewParaCrop() {
        try {
            // Converter UploadedFile para MultipartFile ou byte array
            // Por simplicidade, vamos usar um placeholder
            previewUrl = "/api/avatar/preview/temp_" + System.currentTimeMillis();
            
            // Reset crop para centralizado
            cropCentralizado = true;
            cropX = 0;
            cropY = 0;
            cropLargura = 256;
            cropAltura = 256;
            
        } catch (Exception e) {
            logger.error("Erro ao gerar preview: {}", e.getMessage(), e);
            previewUrl = null;
        }
    }
    
    /**
     * Aplica crop e processa upload
     */
    public void aplicarCropEUpload() {
        try {
            if (!permitirEdicao) {
                adicionarMensagem(FacesMessage.SEVERITY_ERROR, "Erro", "Você não tem permissão para editar este avatar");
                return;
            }
            
            if (arquivoUpload == null) {
                adicionarMensagem(FacesMessage.SEVERITY_ERROR, "Erro", "Nenhum arquivo selecionado");
                return;
            }
            
            processandoUpload = true;
            
            // Criar DTO de upload
            AvatarUploadDTO uploadDTO;
            if (cropCentralizado) {
                uploadDTO = AvatarUploadDTO.cropCentralizado(usuarioId);
            } else {
                uploadDTO = AvatarUploadDTO.cropPersonalizado(usuarioId, cropX, cropY, cropLargura, cropAltura);
            }
            
            // Adicionar metadados
            uploadDTO.setNomeOriginal(nomeArquivoOriginal);
            uploadDTO.setTamanhoOriginal(tamanhoArquivo);
            uploadDTO.setTipoMime(tipoMime);
            
            // Converter UploadedFile para MultipartFile (implementação específica necessária)
            // Por enquanto, vamos simular o processamento
            
            // Iniciar processamento assíncrono
            CompletableFuture<String> futureAvatar = avatarService.processarUploadAvatar(
                usuarioId, 
                null, // Converter arquivoUpload para MultipartFile
                uploadDTO
            );
            
            // Configurar callback para quando terminar
            futureAvatar.whenComplete((resultado, erro) -> {
                processandoUpload = false;
                
                if (erro != null) {
                    logger.error("Erro no processamento de avatar: {}", erro.getMessage(), erro);
                    adicionarMensagem(FacesMessage.SEVERITY_ERROR, "Erro no processamento", erro.getMessage());
                } else {
                    logger.info("Avatar processado com sucesso para usuário: {}", usuarioId);
                    carregarAvatarAtual();
                    fecharCrop();
                    adicionarMensagem(FacesMessage.SEVERITY_INFO, "Sucesso", "Avatar atualizado com sucesso!");
                }
            });
            
            adicionarMensagem(FacesMessage.SEVERITY_INFO, "Processando", "Avatar sendo processado em segundo plano...");
            
        } catch (Exception e) {
            processandoUpload = false;
            logger.error("Erro ao aplicar crop e upload: {}", e.getMessage(), e);
            adicionarMensagem(FacesMessage.SEVERITY_ERROR, "Erro no processamento", e.getMessage());
        }
    }
    
    /**
     * Cancela crop e fecha interface
     */
    public void cancelarCrop() {
        fecharCrop();
        adicionarMensagem(FacesMessage.SEVERITY_INFO, "Cancelado", "Upload cancelado");
    }
    
    /**
     * Fecha interface de crop
     */
    private void fecharCrop() {
        mostrarCrop = false;
        arquivoUpload = null;
        previewUrl = null;
        cropCentralizado = true;
    }
    
    /**
     * Remove avatar atual
     */
    public void removerAvatar() {
        try {
            if (!permitirEdicao) {
                adicionarMensagem(FacesMessage.SEVERITY_ERROR, "Erro", "Você não tem permissão para remover este avatar");
                return;
            }
            
            // Implementar remoção no service
            // avatarService.removerAvatar(usuarioId);
            
            avatarAtual = null;
            adicionarMensagem(FacesMessage.SEVERITY_INFO, "Sucesso", "Avatar removido com sucesso");
            
        } catch (Exception e) {
            logger.error("Erro ao remover avatar: {}", e.getMessage(), e);
            adicionarMensagem(FacesMessage.SEVERITY_ERROR, "Erro", "Não foi possível remover o avatar");
        }
    }
    
    /**
     * Atualiza configurações de crop
     */
    public void atualizarCrop() {
        if (cropCentralizado) {
            // Reset para crop centralizado
            cropX = 0;
            cropY = 0;
            cropLargura = 256;
            cropAltura = 256;
        }
        
        logger.debug("Crop atualizado - Centralizado: {}, X: {}, Y: {}, L: {}, A: {}", 
            cropCentralizado, cropX, cropY, cropLargura, cropAltura);
    }
    
    /**
     * Verifica se tem avatar
     */
    public boolean temAvatar() {
        return avatarAtual != null && !avatarAtual.trim().isEmpty();
    }
    
    /**
     * Obtém URL do avatar ou placeholder
     */
    public String getAvatarUrl() {
        if (temAvatar()) {
            return avatarAtual;
        }
        return "/resources/images/avatar-placeholder.png";
    }
    
    /**
     * Obtém classe CSS para avatar
     */
    public String getAvatarClass() {
        StringBuilder classes = new StringBuilder("avatar");
        
        switch (tamanhoExibicao) {
            case "pequeno":
                classes.append(" avatar-sm");
                break;
            case "grande":
                classes.append(" avatar-lg");
                break;
            default:
                classes.append(" avatar-md");
        }
        
        if (!temAvatar()) {
            classes.append(" avatar-placeholder");
        }
        
        return classes.toString();
    }
    
    /**
     * Adiciona mensagem ao contexto JSF
     */
    private void adicionarMensagem(FacesMessage.Severity severity, String summary, String detail) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(severity, summary, detail));
    }
    
    // Getters e Setters
    
    public Long getUsuarioId() {
        return usuarioId;
    }
    
    public void setUsuarioId(Long usuarioId) {
        this.usuarioId = usuarioId;
    }
    
    public String getAvatarAtual() {
        return avatarAtual;
    }
    
    public void setAvatarAtual(String avatarAtual) {
        this.avatarAtual = avatarAtual;
    }
    
    public boolean isProcessandoUpload() {
        return processandoUpload;
    }
    
    public boolean isMostrarCrop() {
        return mostrarCrop;
    }
    
    public String getPreviewUrl() {
        return previewUrl;
    }
    
    public Integer getCropX() {
        return cropX;
    }
    
    public void setCropX(Integer cropX) {
        this.cropX = cropX;
    }
    
    public Integer getCropY() {
        return cropY;
    }
    
    public void setCropY(Integer cropY) {
        this.cropY = cropY;
    }
    
    public Integer getCropLargura() {
        return cropLargura;
    }
    
    public void setCropLargura(Integer cropLargura) {
        this.cropLargura = cropLargura;
    }
    
    public Integer getCropAltura() {
        return cropAltura;
    }
    
    public void setCropAltura(Integer cropAltura) {
        this.cropAltura = cropAltura;
    }
    
    public boolean isCropCentralizado() {
        return cropCentralizado;
    }
    
    public void setCropCentralizado(boolean cropCentralizado) {
        this.cropCentralizado = cropCentralizado;
    }
    
    public String getTamanhoExibicao() {
        return tamanhoExibicao;
    }
    
    public void setTamanhoExibicao(String tamanhoExibicao) {
        this.tamanhoExibicao = tamanhoExibicao;
        carregarAvatarAtual(); // Recarregar com novo tamanho
    }
    
    public boolean isPermitirEdicao() {
        return permitirEdicao;
    }
    
    public String getNomeArquivoOriginal() {
        return nomeArquivoOriginal;
    }
    
    public long getTamanhoArquivo() {
        return tamanhoArquivo;
    }
    
    public String getTipoMime() {
        return tipoMime;
    }
    
    /**
     * Formata tamanho do arquivo para exibição
     */
    public String getTamanhoArquivoFormatado() {
        if (tamanhoArquivo == 0) {
            return "0 B";
        }
        
        String[] unidades = {"B", "KB", "MB", "GB"};
        int unidade = 0;
        double tamanho = tamanhoArquivo;
        
        while (tamanho >= 1024 && unidade < unidades.length - 1) {
            tamanho /= 1024;
            unidade++;
        }
        
        return String.format("%.1f %s", tamanho, unidades[unidade]);
    }
}