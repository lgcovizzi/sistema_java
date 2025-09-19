package com.sistema.batch;

import com.sistema.entity.ImageResizeQueue;
import com.sistema.entity.ImageResizeQueue.ProcessingStatus;
import com.sistema.entity.ImageResizeQueue.ResizeMode;
import com.sistema.repository.ImageResizeQueueRepository;
import org.imgscalr.Scalr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.data.RepositoryItemReader;
import org.springframework.batch.item.data.builder.RepositoryItemReaderBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.PlatformTransactionManager;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/**
 * Configuração do job de redimensionamento de imagens.
 * Processa imagens da fila de redimensionamento usando Spring Batch.
 */
@Configuration
public class ImageResizeJobConfig {

    private static final Logger logger = LoggerFactory.getLogger(ImageResizeJobConfig.class);

    @Autowired
    private ImageResizeQueueRepository imageResizeQueueRepository;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    /**
     * Reader para buscar imagens prontas para processamento.
     */
    @Bean
    public ItemReader<ImageResizeQueue> imageResizeReader() {
        return new RepositoryItemReaderBuilder<ImageResizeQueue>()
                .name("imageResizeReader")
                .repository(imageResizeQueueRepository)
                .methodName("findByStatus")
                .arguments(Collections.singletonList(ProcessingStatus.PENDING))
                .sorts(Collections.singletonMap("priority", Sort.Direction.DESC))
                .pageSize(10)
                .build();
    }

    /**
     * Processor para redimensionar imagens.
     */
    @Bean
    public ItemProcessor<ImageResizeQueue, ImageResizeQueue> imageResizeProcessor() {
        return new ItemProcessor<ImageResizeQueue, ImageResizeQueue>() {
            @Override
            public ImageResizeQueue process(ImageResizeQueue item) throws Exception {
                logger.info("Processando redimensionamento de imagem: {}", item.getOriginalPath());
                
                try {
                    // Marcar como em processamento
                    item.setStatus(ProcessingStatus.PROCESSING);
                    item.setStartedAt(LocalDateTime.now());
                    item.setAttempts(item.getAttempts() + 1);
                    imageResizeQueueRepository.save(item);

                    long startTime = System.currentTimeMillis();

                    // Verificar se arquivo original existe
                    Path originalPath = Paths.get(item.getOriginalPath());
                    if (!Files.exists(originalPath)) {
                        throw new IOException("Arquivo original não encontrado: " + item.getOriginalPath());
                    }

                    // Ler imagem original
                    BufferedImage originalImage = ImageIO.read(originalPath.toFile());
                    if (originalImage == null) {
                        throw new IOException("Não foi possível ler a imagem: " + item.getOriginalPath());
                    }

                    // Obter tamanho original
                    long originalSize = Files.size(originalPath);
                    item.setOriginalSizeBytes(originalSize);

                    // Redimensionar imagem
                    BufferedImage resizedImage = resizeImage(originalImage, item);

                    // Aplicar watermark se especificado
                    if (item.getWatermarkText() != null && !item.getWatermarkText().trim().isEmpty()) {
                        resizedImage = applyWatermark(resizedImage, item.getWatermarkText());
                    }

                    // Criar diretório de destino se não existir
                    Path targetPath = Paths.get(item.getTargetPath());
                    Files.createDirectories(targetPath.getParent());

                    // Salvar imagem redimensionada
                    String format = item.getFormat() != null ? item.getFormat() : "jpg";
                    File outputFile = targetPath.toFile();
                    
                    if (item.getQuality() != null && item.getQuality() > 0 && item.getQuality() <= 1.0f) {
                        // Salvar com qualidade específica (para JPEG)
                        saveImageWithQuality(resizedImage, outputFile, format, item.getQuality());
                    } else {
                        // Salvar com qualidade padrão
                        ImageIO.write(resizedImage, format, outputFile);
                    }

                    // Calcular estatísticas
                    long targetSize = Files.size(targetPath);
                    item.setTargetSizeBytes(targetSize);
                    
                    if (originalSize > 0) {
                        item.setCompressionRatio((float) targetSize / originalSize);
                    }

                    long processingTime = System.currentTimeMillis() - startTime;
                    item.setProcessingTimeMs(processingTime);

                    // Marcar como concluído
                    item.setStatus(ProcessingStatus.COMPLETED);
                    item.setCompletedAt(LocalDateTime.now());
                    item.setErrorMessage(null);

                    logger.info("Imagem redimensionada com sucesso: {} -> {} ({}ms)", 
                               item.getOriginalPath(), item.getTargetPath(), processingTime);

                    return item;

                } catch (Exception e) {
                    logger.error("Erro ao processar imagem {}: {}", item.getOriginalPath(), e.getMessage(), e);
                    
                    // Marcar como falha
                    item.setStatus(ProcessingStatus.FAILED);
                    item.setCompletedAt(LocalDateTime.now());
                    item.setErrorMessage(e.getMessage());
                    
                    return item;
                }
            }
        };
    }

    /**
     * Writer para salvar resultados do processamento.
     */
    @Bean
    public ItemWriter<ImageResizeQueue> imageResizeWriter() {
        return new ItemWriter<ImageResizeQueue>() {
            @Override
            public void write(Chunk<? extends ImageResizeQueue> chunk) throws Exception {
                List<? extends ImageResizeQueue> items = chunk.getItems();
                for (ImageResizeQueue item : items) {
                    imageResizeQueueRepository.save(item);
                    
                    if (item.getStatus() == ProcessingStatus.COMPLETED) {
                        logger.info("Imagem processada com sucesso: {}", item.getTargetPath());
                    } else if (item.getStatus() == ProcessingStatus.FAILED) {
                        logger.warn("Falha no processamento da imagem {}: {}", 
                                   item.getOriginalPath(), item.getErrorMessage());
                        
                        // Se ainda há tentativas disponíveis, marcar para retry
                        if (item.getAttempts() < item.getMaxAttempts()) {
                            item.setStatus(ProcessingStatus.PENDING);
                            item.setStartedAt(null);
                            imageResizeQueueRepository.save(item);
                            logger.info("Imagem marcada para retry: {} (tentativa {}/{})", 
                                       item.getOriginalPath(), item.getAttempts(), item.getMaxAttempts());
                        }
                    }
                }
            }
        };
    }

    /**
     * Step de redimensionamento de imagens.
     */
    @Bean
    public Step imageResizeStep() {
        return new StepBuilder("imageResizeStep", jobRepository)
                .<ImageResizeQueue, ImageResizeQueue>chunk(5, transactionManager)
                .reader(imageResizeReader())
                .processor(imageResizeProcessor())
                .writer(imageResizeWriter())
                .faultTolerant()
                .skipLimit(10)
                .skip(Exception.class)
                .build();
    }

    /**
     * Job de redimensionamento de imagens.
     */
    @Bean
    public Job imageResizeJob() {
        return new JobBuilder("imageResizeJob", jobRepository)
                .start(imageResizeStep())
                .build();
    }

    /**
     * Redimensiona uma imagem baseado no modo especificado.
     */
    private BufferedImage resizeImage(BufferedImage originalImage, ImageResizeQueue item) {
        int targetWidth = item.getTargetWidth() != null ? item.getTargetWidth() : originalImage.getWidth();
        int targetHeight = item.getTargetHeight() != null ? item.getTargetHeight() : originalImage.getHeight();
        
        ResizeMode mode = item.getResizeMode() != null ? item.getResizeMode() : ResizeMode.SCALE_TO_FIT;

        switch (mode) {
            case SCALE_TO_FIT:
                // Redimensiona mantendo proporção, cabendo dentro das dimensões especificadas
                return Scalr.resize(originalImage, Scalr.Method.QUALITY, Scalr.Mode.AUTOMATIC, 
                                   targetWidth, targetHeight);
                
            case SCALE_TO_FILL:
                // Redimensiona preenchendo as dimensões especificadas, pode cortar
                return Scalr.resize(originalImage, Scalr.Method.QUALITY, Scalr.Mode.FIT_TO_HEIGHT, 
                                   targetWidth, targetHeight);
                
            case STRETCH:
                // Redimensiona forçando as dimensões exatas, pode distorcer
                return Scalr.resize(originalImage, Scalr.Method.QUALITY, Scalr.Mode.FIT_EXACT, 
                                   targetWidth, targetHeight);
                
            case CROP:
                // Corta a imagem para as dimensões especificadas
                int x = Math.max(0, (originalImage.getWidth() - targetWidth) / 2);
                int y = Math.max(0, (originalImage.getHeight() - targetHeight) / 2);
                return Scalr.crop(originalImage, x, y, 
                                 Math.min(targetWidth, originalImage.getWidth()),
                                 Math.min(targetHeight, originalImage.getHeight()));
                
            case THUMBNAIL:
                // Cria miniatura com qualidade otimizada
                return Scalr.resize(originalImage, Scalr.Method.SPEED, Scalr.Mode.FIT_TO_WIDTH, 
                                   targetWidth, targetHeight);
                
            default:
                return Scalr.resize(originalImage, Scalr.Method.QUALITY, Scalr.Mode.AUTOMATIC, 
                                   targetWidth, targetHeight);
        }
    }

    /**
     * Aplica watermark de texto na imagem.
     */
    private BufferedImage applyWatermark(BufferedImage image, String watermarkText) {
        BufferedImage watermarkedImage = new BufferedImage(
            image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
        
        Graphics2D g2d = watermarkedImage.createGraphics();
        
        // Desenhar imagem original
        g2d.drawImage(image, 0, 0, null);
        
        // Configurar watermark
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setColor(new Color(255, 255, 255, 128)); // Branco semi-transparente
        
        // Calcular tamanho da fonte baseado no tamanho da imagem
        int fontSize = Math.max(12, image.getWidth() / 30);
        Font font = new Font("Arial", Font.BOLD, fontSize);
        g2d.setFont(font);
        
        // Calcular posição do watermark (canto inferior direito)
        FontMetrics fm = g2d.getFontMetrics();
        int textWidth = fm.stringWidth(watermarkText);
        int textHeight = fm.getHeight();
        
        int x = image.getWidth() - textWidth - 10;
        int y = image.getHeight() - 10;
        
        // Desenhar watermark
        g2d.drawString(watermarkText, x, y);
        g2d.dispose();
        
        return watermarkedImage;
    }

    /**
     * Salva imagem com qualidade específica (para JPEG).
     */
    private void saveImageWithQuality(BufferedImage image, File outputFile, String format, float quality) 
            throws IOException {
        if ("jpg".equalsIgnoreCase(format) || "jpeg".equalsIgnoreCase(format)) {
            // Para JPEG, usar ImageWriter com compressão
            javax.imageio.ImageWriter writer = ImageIO.getImageWritersByFormatName("jpg").next();
            javax.imageio.ImageWriteParam param = writer.getDefaultWriteParam();
            param.setCompressionMode(javax.imageio.ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(quality);
            
            try (javax.imageio.stream.ImageOutputStream ios = ImageIO.createImageOutputStream(outputFile)) {
                writer.setOutput(ios);
                writer.write(null, new javax.imageio.IIOImage(image, null, null), param);
            } finally {
                writer.dispose();
            }
        } else {
            // Para outros formatos, usar método padrão
            ImageIO.write(image, format, outputFile);
        }
    }
}