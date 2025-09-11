package com.sistema.java.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

@Service
public class FileUploadService {
    
    private static final Logger logger = Logger.getLogger(FileUploadService.class.getName());
    
    @Value("${app.upload.path:uploads}")
    private String uploadPath;
    
    @Value("${app.upload.max-size:5242880}") // 5MB
    private long maxFileSize;
    
    @Value("${app.upload.avatar.width:200}")
    private int avatarWidth;
    
    @Value("${app.upload.avatar.height:200}")
    private int avatarHeight;
    
    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList(
        "jpg", "jpeg", "png", "gif", "webp"
    );
    
    private static final List<String> ALLOWED_MIME_TYPES = Arrays.asList(
        "image/jpeg", "image/png", "image/gif", "image/webp"
    );
    
    /**
     * Faz upload de avatar do usuário
     */
    public String uploadAvatar(MultipartFile file, Long usuarioId) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Arquivo não pode estar vazio");
        }
        
        // Validar arquivo
        validateFile(file);
        
        // Criar diretório se não existir
        String avatarDir = uploadPath + File.separator + "avatars";
        createDirectoryIfNotExists(avatarDir);
        
        // Gerar nome único para o arquivo
        String originalFilename = file.getOriginalFilename();
        String extension = getFileExtension(originalFilename);
        String filename = "avatar_" + usuarioId + "_" + UUID.randomUUID().toString() + "." + extension;
        
        // Caminho completo do arquivo
        Path filePath = Paths.get(avatarDir, filename);
        
        try {
            // Processar e redimensionar imagem
            BufferedImage processedImage = processImage(file);
            
            // Salvar imagem processada
            ImageIO.write(processedImage, extension, filePath.toFile());
            
            // Retornar caminho relativo
            String relativePath = "avatars/" + filename;
            
            logger.info("Avatar salvo com sucesso: " + relativePath);
            return relativePath;
            
        } catch (Exception e) {
            logger.severe("Erro ao processar avatar: " + e.getMessage());
            throw new IOException("Erro ao processar imagem: " + e.getMessage());
        }
    }
    
    /**
     * Faz upload de imagem para notícia
     */
    public String uploadImagemNoticia(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Arquivo não pode estar vazio");
        }
        
        // Validar arquivo
        validateFile(file);
        
        // Criar diretório se não existir
        String noticiasDir = uploadPath + File.separator + "noticias";
        createDirectoryIfNotExists(noticiasDir);
        
        // Gerar nome único para o arquivo
        String originalFilename = file.getOriginalFilename();
        String extension = getFileExtension(originalFilename);
        String filename = "noticia_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString() + "." + extension;
        
        // Caminho completo do arquivo
        Path filePath = Paths.get(noticiasDir, filename);
        
        try {
            // Processar imagem (redimensionar se necessário)
            BufferedImage processedImage = processImageNoticia(file);
            
            // Salvar imagem processada
            ImageIO.write(processedImage, extension, filePath.toFile());
            
            // Retornar caminho relativo
            String relativePath = "noticias/" + filename;
            
            logger.info("Imagem de notícia salva: " + relativePath);
            return relativePath;
            
        } catch (Exception e) {
            logger.severe("Erro ao processar imagem de notícia: " + e.getMessage());
            throw new IOException("Erro ao processar imagem: " + e.getMessage());
        }
    }
    
    /**
     * Remove arquivo do sistema
     */
    public boolean removeFile(String relativePath) {
        if (relativePath == null || relativePath.trim().isEmpty()) {
            return false;
        }
        
        try {
            Path filePath = Paths.get(uploadPath, relativePath);
            
            if (Files.exists(filePath)) {
                Files.delete(filePath);
                logger.info("Arquivo removido: " + relativePath);
                return true;
            }
            
        } catch (Exception e) {
            logger.severe("Erro ao remover arquivo: " + e.getMessage());
        }
        
        return false;
    }
    
    /**
     * Verifica se arquivo existe
     */
    public boolean fileExists(String relativePath) {
        if (relativePath == null || relativePath.trim().isEmpty()) {
            return false;
        }
        
        Path filePath = Paths.get(uploadPath, relativePath);
        return Files.exists(filePath);
    }
    
    /**
     * Obtém URL pública do arquivo
     */
    public String getFileUrl(String relativePath) {
        if (relativePath == null || relativePath.trim().isEmpty()) {
            return null;
        }
        
        return "/uploads/" + relativePath;
    }
    
    /**
     * Obtém tamanho do arquivo em bytes
     */
    public long getFileSize(String relativePath) {
        if (relativePath == null || relativePath.trim().isEmpty()) {
            return 0;
        }
        
        try {
            Path filePath = Paths.get(uploadPath, relativePath);
            if (Files.exists(filePath)) {
                return Files.size(filePath);
            }
        } catch (Exception e) {
            logger.severe("Erro ao obter tamanho do arquivo: " + e.getMessage());
        }
        
        return 0;
    }
    
    // Métodos privados
    
    private void validateFile(MultipartFile file) throws IOException {
        // Verificar tamanho
        if (file.getSize() > maxFileSize) {
            throw new IOException("Arquivo muito grande. Tamanho máximo: " + (maxFileSize / 1024 / 1024) + "MB");
        }
        
        // Verificar extensão
        String filename = file.getOriginalFilename();
        if (filename == null || filename.trim().isEmpty()) {
            throw new IOException("Nome do arquivo inválido");
        }
        
        String extension = getFileExtension(filename).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IOException("Tipo de arquivo não permitido. Permitidos: " + String.join(", ", ALLOWED_EXTENSIONS));
        }
        
        // Verificar MIME type
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType.toLowerCase())) {
            throw new IOException("Tipo de conteúdo não permitido");
        }
        
        // Verificar se é realmente uma imagem
        try {
            BufferedImage image = ImageIO.read(file.getInputStream());
            if (image == null) {
                throw new IOException("Arquivo não é uma imagem válida");
            }
        } catch (Exception e) {
            throw new IOException("Erro ao validar imagem: " + e.getMessage());
        }
    }
    
    private BufferedImage processImage(MultipartFile file) throws IOException {
        BufferedImage originalImage = ImageIO.read(file.getInputStream());
        
        // Redimensionar para avatar
        return resizeImage(originalImage, avatarWidth, avatarHeight);
    }
    
    private BufferedImage processImageNoticia(MultipartFile file) throws IOException {
        BufferedImage originalImage = ImageIO.read(file.getInputStream());
        
        // Para notícias, manter proporção e limitar largura máxima
        int maxWidth = 800;
        int maxHeight = 600;
        
        int originalWidth = originalImage.getWidth();
        int originalHeight = originalImage.getHeight();
        
        // Se a imagem já está dentro dos limites, retornar original
        if (originalWidth <= maxWidth && originalHeight <= maxHeight) {
            return originalImage;
        }
        
        // Calcular novas dimensões mantendo proporção
        double ratio = Math.min((double) maxWidth / originalWidth, (double) maxHeight / originalHeight);
        int newWidth = (int) (originalWidth * ratio);
        int newHeight = (int) (originalHeight * ratio);
        
        return resizeImage(originalImage, newWidth, newHeight);
    }
    
    private BufferedImage resizeImage(BufferedImage originalImage, int targetWidth, int targetHeight) {
        BufferedImage resizedImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = resizedImage.createGraphics();
        
        // Configurar qualidade de renderização
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Desenhar imagem redimensionada
        g2d.drawImage(originalImage, 0, 0, targetWidth, targetHeight, null);
        g2d.dispose();
        
        return resizedImage;
    }
    
    private String getFileExtension(String filename) {
        if (filename == null || filename.trim().isEmpty()) {
            return "";
        }
        
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == filename.length() - 1) {
            return "";
        }
        
        return filename.substring(lastDotIndex + 1).toLowerCase();
    }
    
    private void createDirectoryIfNotExists(String dirPath) throws IOException {
        Path path = Paths.get(dirPath);
        if (!Files.exists(path)) {
            Files.createDirectories(path);
            logger.info("Diretório criado: " + dirPath);
        }
    }
}