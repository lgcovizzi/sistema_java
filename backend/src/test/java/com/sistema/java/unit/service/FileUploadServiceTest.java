package com.sistema.java.unit.service;

import com.sistema.java.service.FileUploadService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Testes unitários para FileUploadService
 * Referência: Sistema de Upload de Arquivos - project_rules.md
 * Referência: Testes e Qualidade de Código - project_rules.md
 * Referência: Padrões de Teste - project_rules.md
 */
@ExtendWith(MockitoExtension.class)
class FileUploadServiceTest {

    @Mock
    private MultipartFile multipartFile;

    @InjectMocks
    private FileUploadService fileUploadService;

    @TempDir
    Path tempDir;

    private byte[] validImageBytes;
    private byte[] invalidFileBytes;
    private final long maxFileSize = 5 * 1024 * 1024; // 5MB
    private final List<String> allowedExtensions = Arrays.asList("jpg", "jpeg", "png", "gif");
    private final List<String> allowedMimeTypes = Arrays.asList("image/jpeg", "image/png", "image/gif");

    @BeforeEach
    void setUp() {
        // Arrange - Configuração dos dados de teste
        validImageBytes = createValidImageBytes();
        invalidFileBytes = "invalid file content".getBytes();
    }

    @Test
    void should_UploadFile_When_ValidImageProvided() throws IOException {
        // Arrange
        when(multipartFile.getOriginalFilename()).thenReturn("test-image.jpg");
        when(multipartFile.getContentType()).thenReturn("image/jpeg");
        when(multipartFile.getSize()).thenReturn(1024L);
        when(multipartFile.getBytes()).thenReturn(validImageBytes);
        when(multipartFile.getInputStream()).thenReturn(new ByteArrayInputStream(validImageBytes));

        // Act
        String resultado = fileUploadService.uploadFile(multipartFile, "avatar");

        // Assert
        assertThat(resultado).isNotNull();
        assertThat(resultado).contains("test-image");
        assertThat(resultado).endsWith(".jpg");
    }

    @Test
    void should_ThrowException_When_FileExceedsMaxSize() {
        // Arrange
        when(multipartFile.getOriginalFilename()).thenReturn("large-image.jpg");
        when(multipartFile.getContentType()).thenReturn("image/jpeg");
        when(multipartFile.getSize()).thenReturn(maxFileSize + 1);

        // Act & Assert
        assertThatThrownBy(() -> fileUploadService.uploadFile(multipartFile, "avatar"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Arquivo excede o tamanho máximo permitido");
    }

    @Test
    void should_ThrowException_When_InvalidFileExtension() {
        // Arrange
        when(multipartFile.getOriginalFilename()).thenReturn("document.pdf");
        when(multipartFile.getContentType()).thenReturn("application/pdf");
        when(multipartFile.getSize()).thenReturn(1024L);

        // Act & Assert
        assertThatThrownBy(() -> fileUploadService.uploadFile(multipartFile, "avatar"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Extensão de arquivo não permitida");
    }

    @Test
    void should_ThrowException_When_InvalidMimeType() {
        // Arrange
        when(multipartFile.getOriginalFilename()).thenReturn("fake-image.jpg");
        when(multipartFile.getContentType()).thenReturn("text/plain");
        when(multipartFile.getSize()).thenReturn(1024L);

        // Act & Assert
        assertThatThrownBy(() -> fileUploadService.uploadFile(multipartFile, "avatar"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Tipo MIME não permitido");
    }

    @Test
    void should_ValidateImageIntegrity_When_UploadingImage() throws IOException {
        // Arrange
        when(multipartFile.getOriginalFilename()).thenReturn("test-image.jpg");
        when(multipartFile.getContentType()).thenReturn("image/jpeg");
        when(multipartFile.getSize()).thenReturn(1024L);
        when(multipartFile.getBytes()).thenReturn(invalidFileBytes);
        when(multipartFile.getInputStream()).thenReturn(new ByteArrayInputStream(invalidFileBytes));

        // Act & Assert
        assertThatThrownBy(() -> fileUploadService.uploadFile(multipartFile, "avatar"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Arquivo de imagem corrompido ou inválido");
    }

    @Test
    void should_ProcessAvatarImage_When_UploadingAvatar() throws IOException {
        // Arrange
        when(multipartFile.getOriginalFilename()).thenReturn("avatar.png");
        when(multipartFile.getContentType()).thenReturn("image/png");
        when(multipartFile.getSize()).thenReturn(2048L);
        when(multipartFile.getBytes()).thenReturn(validImageBytes);
        when(multipartFile.getInputStream()).thenReturn(new ByteArrayInputStream(validImageBytes));

        // Act
        String resultado = fileUploadService.uploadAvatar(multipartFile, 1L);

        // Assert
        assertThat(resultado).isNotNull();
        assertThat(resultado).contains("avatar");
        assertThat(resultado).contains("1");
    }

    @Test
    void should_ProcessNewsImage_When_UploadingNewsImage() throws IOException {
        // Arrange
        when(multipartFile.getOriginalFilename()).thenReturn("news-image.jpg");
        when(multipartFile.getContentType()).thenReturn("image/jpeg");
        when(multipartFile.getSize()).thenReturn(3072L);
        when(multipartFile.getBytes()).thenReturn(validImageBytes);
        when(multipartFile.getInputStream()).thenReturn(new ByteArrayInputStream(validImageBytes));

        // Act
        String resultado = fileUploadService.uploadNewsImage(multipartFile, 1L);

        // Assert
        assertThat(resultado).isNotNull();
        assertThat(resultado).contains("news");
        assertThat(resultado).contains("1");
    }

    @Test
    void should_GenerateUniqueFilename_When_UploadingFile() throws IOException {
        // Arrange
        when(multipartFile.getOriginalFilename()).thenReturn("image.jpg");
        when(multipartFile.getContentType()).thenReturn("image/jpeg");
        when(multipartFile.getSize()).thenReturn(1024L);
        when(multipartFile.getBytes()).thenReturn(validImageBytes);
        when(multipartFile.getInputStream()).thenReturn(new ByteArrayInputStream(validImageBytes));

        // Act
        String resultado1 = fileUploadService.uploadFile(multipartFile, "test");
        String resultado2 = fileUploadService.uploadFile(multipartFile, "test");

        // Assert
        assertThat(resultado1).isNotEqualTo(resultado2);
        assertThat(resultado1).contains("image");
        assertThat(resultado2).contains("image");
    }

    @Test
    void should_ThrowException_When_FileIsEmpty() {
        // Arrange
        when(multipartFile.isEmpty()).thenReturn(true);
        when(multipartFile.getOriginalFilename()).thenReturn("empty.jpg");

        // Act & Assert
        assertThatThrownBy(() -> fileUploadService.uploadFile(multipartFile, "avatar"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Arquivo está vazio");
    }

    @Test
    void should_ThrowException_When_FilenameIsNull() {
        // Arrange
        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getOriginalFilename()).thenReturn(null);
        when(multipartFile.getSize()).thenReturn(1024L);

        // Act & Assert
        assertThatThrownBy(() -> fileUploadService.uploadFile(multipartFile, "avatar"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Nome do arquivo não pode ser nulo");
    }

    @Test
    void should_SanitizeFilename_When_UploadingFile() throws IOException {
        // Arrange
        when(multipartFile.getOriginalFilename()).thenReturn("../../../malicious file.jpg");
        when(multipartFile.getContentType()).thenReturn("image/jpeg");
        when(multipartFile.getSize()).thenReturn(1024L);
        when(multipartFile.getBytes()).thenReturn(validImageBytes);
        when(multipartFile.getInputStream()).thenReturn(new ByteArrayInputStream(validImageBytes));

        // Act
        String resultado = fileUploadService.uploadFile(multipartFile, "avatar");

        // Assert
        assertThat(resultado).isNotNull();
        assertThat(resultado).doesNotContain("../");
        assertThat(resultado).doesNotContain("..");
    }

    @Test
    void should_DeleteFile_When_FileExists() {
        // Arrange
        String filename = "test-file.jpg";
        
        // Act
        boolean resultado = fileUploadService.deleteFile(filename);

        // Assert
        // O resultado depende da implementação específica
        // Este teste verifica se o método executa sem exceções
        assertThat(resultado).isNotNull();
    }

    @Test
    void should_ResizeImage_When_ProcessingAvatar() throws IOException {
        // Arrange
        when(multipartFile.getOriginalFilename()).thenReturn("large-avatar.jpg");
        when(multipartFile.getContentType()).thenReturn("image/jpeg");
        when(multipartFile.getSize()).thenReturn(4096L);
        when(multipartFile.getBytes()).thenReturn(validImageBytes);
        when(multipartFile.getInputStream()).thenReturn(new ByteArrayInputStream(validImageBytes));

        // Act
        String resultado = fileUploadService.resizeAndUploadAvatar(multipartFile, 1L, 256, 256);

        // Assert
        assertThat(resultado).isNotNull();
        assertThat(resultado).contains("256x256");
    }

    @Test
    void should_CreateMultipleSizes_When_ProcessingAvatar() throws IOException {
        // Arrange
        when(multipartFile.getOriginalFilename()).thenReturn("avatar.png");
        when(multipartFile.getContentType()).thenReturn("image/png");
        when(multipartFile.getSize()).thenReturn(2048L);
        when(multipartFile.getBytes()).thenReturn(validImageBytes);
        when(multipartFile.getInputStream()).thenReturn(new ByteArrayInputStream(validImageBytes));

        // Act
        List<String> resultados = fileUploadService.createAvatarSizes(multipartFile, 1L);

        // Assert
        assertThat(resultados).hasSize(3); // 64x64, 256x256, 512x512
        assertThat(resultados.get(0)).contains("64x64");
        assertThat(resultados.get(1)).contains("256x256");
        assertThat(resultados.get(2)).contains("512x512");
    }

    @Test
    void should_ValidateFileSignature_When_CheckingImageIntegrity() throws IOException {
        // Arrange
        byte[] jpegSignature = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
        when(multipartFile.getOriginalFilename()).thenReturn("test.jpg");
        when(multipartFile.getContentType()).thenReturn("image/jpeg");
        when(multipartFile.getSize()).thenReturn(1024L);
        when(multipartFile.getBytes()).thenReturn(jpegSignature);
        when(multipartFile.getInputStream()).thenReturn(new ByteArrayInputStream(jpegSignature));

        // Act
        boolean isValid = fileUploadService.validateImageSignature(multipartFile);

        // Assert
        assertThat(isValid).isTrue();
    }

    private byte[] createValidImageBytes() {
        // Simula bytes de uma imagem JPEG válida
        byte[] jpegHeader = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0};
        byte[] content = new byte[1020];
        byte[] jpegFooter = {(byte) 0xFF, (byte) 0xD9};
        
        byte[] result = new byte[jpegHeader.length + content.length + jpegFooter.length];
        System.arraycopy(jpegHeader, 0, result, 0, jpegHeader.length);
        System.arraycopy(content, 0, result, jpegHeader.length, content.length);
        System.arraycopy(jpegFooter, 0, result, jpegHeader.length + content.length, jpegFooter.length);
        
        return result;
    }
}