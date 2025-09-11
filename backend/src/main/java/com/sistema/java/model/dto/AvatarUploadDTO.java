package com.sistema.java.model.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * DTO para upload e processamento de avatar
 * Referência: Regras de Avatar - project_rules.md
 * Referência: Regras de Validação de Perfil - project_rules.md
 */
public class AvatarUploadDTO {

    @NotNull(message = "ID do usuário é obrigatório")
    private Long usuarioId;

    // Coordenadas para crop personalizado (opcional)
    @Min(value = 0, message = "Coordenada X deve ser maior ou igual a 0")
    private Integer x;

    @Min(value = 0, message = "Coordenada Y deve ser maior ou igual a 0")
    private Integer y;

    @Min(value = 1, message = "Largura deve ser maior que 0")
    private Integer largura;

    @Min(value = 1, message = "Altura deve ser maior que 0")
    private Integer altura;

    // Indica se deve aplicar crop centralizado automático
    private boolean cropCentralizado = true;

    // Qualidade da imagem (1-100)
    @Min(value = 1, message = "Qualidade deve ser entre 1 e 100")
    private Integer qualidade = 85;

    // Formato de saída preferido
    private String formatoSaida = "jpg";

    // Metadados adicionais
    private String nomeOriginal;
    private Long tamanhoOriginal;
    private String tipoMime;

    public AvatarUploadDTO() {
    }

    public AvatarUploadDTO(Long usuarioId) {
        this.usuarioId = usuarioId;
    }

    public AvatarUploadDTO(Long usuarioId, Integer x, Integer y, Integer largura, Integer altura) {
        this.usuarioId = usuarioId;
        this.x = x;
        this.y = y;
        this.largura = largura;
        this.altura = altura;
        this.cropCentralizado = false;
    }

    /**
     * Verifica se tem informações de crop personalizado
     */
    public boolean temCropInfo() {
        return !cropCentralizado && x != null && y != null && largura != null && altura != null;
    }

    /**
     * Valida se as coordenadas de crop são válidas
     */
    public boolean cropValido() {
        if (cropCentralizado) {
            return true;
        }
        
        return x != null && y != null && largura != null && altura != null &&
               x >= 0 && y >= 0 && largura > 0 && altura > 0;
    }

    /**
     * Cria DTO para crop centralizado
     */
    public static AvatarUploadDTO cropCentralizado(Long usuarioId) {
        AvatarUploadDTO dto = new AvatarUploadDTO(usuarioId);
        dto.setCropCentralizado(true);
        return dto;
    }

    /**
     * Cria DTO para crop personalizado
     */
    public static AvatarUploadDTO cropPersonalizado(Long usuarioId, int x, int y, int largura, int altura) {
        return new AvatarUploadDTO(usuarioId, x, y, largura, altura);
    }

    // Getters e Setters

    public Long getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(Long usuarioId) {
        this.usuarioId = usuarioId;
    }

    public Integer getX() {
        return x;
    }

    public void setX(Integer x) {
        this.x = x;
    }

    public Integer getY() {
        return y;
    }

    public void setY(Integer y) {
        this.y = y;
    }

    public Integer getLargura() {
        return largura;
    }

    public void setLargura(Integer largura) {
        this.largura = largura;
    }

    public Integer getAltura() {
        return altura;
    }

    public void setAltura(Integer altura) {
        this.altura = altura;
    }

    public boolean isCropCentralizado() {
        return cropCentralizado;
    }

    public void setCropCentralizado(boolean cropCentralizado) {
        this.cropCentralizado = cropCentralizado;
    }

    public Integer getQualidade() {
        return qualidade;
    }

    public void setQualidade(Integer qualidade) {
        this.qualidade = qualidade;
    }

    public String getFormatoSaida() {
        return formatoSaida;
    }

    public void setFormatoSaida(String formatoSaida) {
        this.formatoSaida = formatoSaida;
    }

    public String getNomeOriginal() {
        return nomeOriginal;
    }

    public void setNomeOriginal(String nomeOriginal) {
        this.nomeOriginal = nomeOriginal;
    }

    public Long getTamanhoOriginal() {
        return tamanhoOriginal;
    }

    public void setTamanhoOriginal(Long tamanhoOriginal) {
        this.tamanhoOriginal = tamanhoOriginal;
    }

    public String getTipoMime() {
        return tipoMime;
    }

    public void setTipoMime(String tipoMime) {
        this.tipoMime = tipoMime;
    }

    @Override
    public String toString() {
        return "AvatarUploadDTO{" +
                "usuarioId=" + usuarioId +
                ", x=" + x +
                ", y=" + y +
                ", largura=" + largura +
                ", altura=" + altura +
                ", cropCentralizado=" + cropCentralizado +
                ", qualidade=" + qualidade +
                ", formatoSaida='" + formatoSaida + '\'' +
                ", nomeOriginal='" + nomeOriginal + '\'' +
                ", tamanhoOriginal=" + tamanhoOriginal +
                ", tipoMime='" + tipoMime + '\'' +
                '}';
    }
}