package com.sistema.batch;

import com.sistema.entity.FileProcessingQueue;
import com.sistema.entity.FileProcessingQueue.OperationType;
import com.sistema.entity.FileProcessingQueue.ProcessingStatus;
import com.sistema.repository.FileProcessingQueueRepository;
import org.apache.commons.io.FileUtils;
import org.apache.tika.Tika;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.data.RepositoryItemReader;
import org.springframework.batch.item.data.builder.RepositoryItemReaderBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.PlatformTransactionManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Configuração do job de processamento de arquivos.
 * Processa operações de arquivo da fila usando Spring Batch.
 */
@Configuration
public class FileProcessingJobConfig {

    private static final Logger logger = LoggerFactory.getLogger(FileProcessingJobConfig.class);

    @Autowired
    private FileProcessingQueueRepository fileProcessingQueueRepository;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private final Tika tika = new Tika();

    /**
     * Reader para buscar arquivos prontos para processamento.
     */
    @Bean
    public ItemReader<FileProcessingQueue> fileProcessingReader() {
        return new RepositoryItemReaderBuilder<FileProcessingQueue>()
                .name("fileProcessingReader")
                .repository(fileProcessingQueueRepository)
                .methodName("findByStatus")
                .arguments(Collections.singletonList(ProcessingStatus.PENDING))
                .sorts(Collections.singletonMap("priority", Sort.Direction.DESC))
                .pageSize(10)
                .build();
    }

    /**
     * Processor para processar operações de arquivo.
     */
    @Bean
    public ItemProcessor<FileProcessingQueue, FileProcessingQueue> fileProcessingProcessor() {
        return new ItemProcessor<FileProcessingQueue, FileProcessingQueue>() {
            @Override
            public FileProcessingQueue process(FileProcessingQueue item) throws Exception {
                logger.info("Processando operação de arquivo: {} - {}", 
                           item.getOperationType(), item.getSourcePath());
                
                try {
                    // Marcar como em processamento
                    item.setStatus(ProcessingStatus.PROCESSING);
                    item.setStartedAt(LocalDateTime.now());
                    item.setAttempts(item.getAttempts() + 1);
                    fileProcessingQueueRepository.save(item);

                    long startTime = System.currentTimeMillis();

                    // Processar baseado no tipo de operação
                    switch (item.getOperationType()) {
                        case LIST_FILES:
                            processListFiles(item);
                            break;
                        case ANALYZE_DIRECTORY:
                            processAnalyzeDirectory(item);
                            break;
                        case COMPRESS_FILES:
                            processCompressFiles(item);
                            break;
                        case EXTRACT_FILES:
                            processExtractFiles(item);
                            break;
                        case ORGANIZE_FILES:
                            processOrganizeFiles(item);
                            break;
                        case DUPLICATE_DETECTION:
                            processDuplicateDetection(item);
                            break;
                        case FILE_VALIDATION:
                            processFileValidation(item);
                            break;
                        case METADATA_EXTRACTION:
                            processMetadataExtraction(item);
                            break;
                        case BATCH_RENAME:
                            processBatchRename(item);
                            break;
                        case SYNC_DIRECTORIES:
                            processSyncDirectories(item);
                            break;
                        default:
                            throw new UnsupportedOperationException("Operação não suportada: " + item.getOperationType());
                    }

                    long processingTime = System.currentTimeMillis() - startTime;
                    item.setProcessingTimeMs(processingTime);

                    // Marcar como concluído
                    item.setStatus(ProcessingStatus.COMPLETED);
                    item.setCompletedAt(LocalDateTime.now());
                    item.setErrorMessage(null);

                    logger.info("Operação de arquivo processada com sucesso: {} ({}ms)", 
                               item.getOperationType(), processingTime);

                    return item;

                } catch (Exception e) {
                    logger.error("Erro ao processar operação de arquivo {}: {}", 
                                item.getOperationType(), e.getMessage(), e);
                    
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
    public ItemWriter<FileProcessingQueue> fileProcessingWriter() {
        return new ItemWriter<FileProcessingQueue>() {
            @Override
            public void write(Chunk<? extends FileProcessingQueue> chunk) throws Exception {
                List<? extends FileProcessingQueue> items = chunk.getItems();
                for (FileProcessingQueue item : items) {
                    fileProcessingQueueRepository.save(item);
                    
                    if (item.getStatus() == ProcessingStatus.COMPLETED) {
                        logger.info("Operação de arquivo processada com sucesso: {}", item.getOperationType());
                        
                        // Deletar origem se especificado
                        if (Boolean.TRUE.equals(item.getDeleteSourceAfterProcessing()) && 
                            item.getSourcePath() != null) {
                            try {
                                Path sourcePath = Paths.get(item.getSourcePath());
                                if (Files.exists(sourcePath)) {
                                    if (Files.isDirectory(sourcePath)) {
                                        FileUtils.deleteDirectory(sourcePath.toFile());
                                    } else {
                                        Files.delete(sourcePath);
                                    }
                                    logger.info("Arquivo/diretório de origem deletado: {}", item.getSourcePath());
                                }
                            } catch (Exception e) {
                                logger.warn("Erro ao deletar origem {}: {}", item.getSourcePath(), e.getMessage());
                            }
                        }
                        
                    } else if (item.getStatus() == ProcessingStatus.FAILED) {
                        logger.warn("Falha no processamento da operação {}: {}", 
                                   item.getOperationType(), item.getErrorMessage());
                        
                        // Se ainda há tentativas disponíveis, marcar para retry
                        if (item.getAttempts() < item.getMaxAttempts()) {
                            item.setStatus(ProcessingStatus.PENDING);
                            item.setStartedAt(null);
                            fileProcessingQueueRepository.save(item);
                            logger.info("Operação marcada para retry: {} (tentativa {}/{})", 
                                       item.getOperationType(), item.getAttempts(), item.getMaxAttempts());
                        }
                    }
                }
            }
        };
    }

    /**
     * Step de processamento de arquivos.
     */
    @Bean
    public Step fileProcessingStep() {
        return new StepBuilder("fileProcessingStep", jobRepository)
                .<FileProcessingQueue, FileProcessingQueue>chunk(5, transactionManager)
                .reader(fileProcessingReader())
                .processor(fileProcessingProcessor())
                .writer(fileProcessingWriter())
                .faultTolerant()
                .skipLimit(10)
                .skip(Exception.class)
                .build();
    }

    /**
     * Job de processamento de arquivos.
     */
    @Bean
    public Job fileProcessingJob() {
        return new JobBuilder("fileProcessingJob", jobRepository)
                .start(fileProcessingStep())
                .build();
    }

    // Métodos de processamento específicos

    private void processListFiles(FileProcessingQueue item) throws IOException {
        Path sourcePath = Paths.get(item.getSourcePath());
        if (!Files.exists(sourcePath)) {
            throw new IOException("Caminho não encontrado: " + item.getSourcePath());
        }

        List<String> fileList = new ArrayList<>();
        long totalSize = 0;
        long fileCount = 0;

        if (Files.isDirectory(sourcePath)) {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(sourcePath, item.getFilePattern())) {
                for (Path file : stream) {
                    if (Files.isRegularFile(file)) {
                        fileList.add(file.toString());
                        totalSize += Files.size(file);
                        fileCount++;
                    } else if (Files.isDirectory(file) && Boolean.TRUE.equals(item.getIncludeSubdirectories())) {
                        Files.walk(file)
                             .filter(Files::isRegularFile)
                             .forEach(f -> {
                                 try {
                                     fileList.add(f.toString());
                                 } catch (Exception e) {
                                     logger.warn("Erro ao processar arquivo {}: {}", f, e.getMessage());
                                 }
                             });
                    }
                }
            }
        } else {
            fileList.add(sourcePath.toString());
            totalSize = Files.size(sourcePath);
            fileCount = 1;
        }

        item.setFileCount(fileCount);
        item.setTotalSizeBytes(totalSize);
        item.setResult("Listados " + fileCount + " arquivos, total: " + FileUtils.byteCountToDisplaySize(totalSize));
        
        // Salvar lista em arquivo se especificado
        if (item.getTargetPath() != null) {
            Path targetPath = Paths.get(item.getTargetPath());
            Files.createDirectories(targetPath.getParent());
            Files.write(targetPath, fileList);
        }
    }

    private void processAnalyzeDirectory(FileProcessingQueue item) throws IOException {
        Path sourcePath = Paths.get(item.getSourcePath());
        if (!Files.exists(sourcePath) || !Files.isDirectory(sourcePath)) {
            throw new IOException("Diretório não encontrado: " + item.getSourcePath());
        }

        Map<String, Long> extensionCount = new HashMap<>();
        Map<String, Long> extensionSize = new HashMap<>();
        long totalFiles = 0;
        long totalSize = 0;

        Files.walk(sourcePath)
             .filter(Files::isRegularFile)
             .forEach(file -> {
                 try {
                     String extension = getFileExtension(file.getFileName().toString()).toLowerCase();
                     long size = Files.size(file);
                     
                     extensionCount.merge(extension, 1L, Long::sum);
                     extensionSize.merge(extension, size, Long::sum);
                 } catch (IOException e) {
                     logger.warn("Erro ao analisar arquivo {}: {}", file, e.getMessage());
                 }
             });

        totalFiles = extensionCount.values().stream().mapToLong(Long::longValue).sum();
        totalSize = extensionSize.values().stream().mapToLong(Long::longValue).sum();

        item.setFileCount(totalFiles);
        item.setTotalSizeBytes(totalSize);
        
        StringBuilder analysis = new StringBuilder();
        analysis.append("Análise do diretório:\n");
        analysis.append("Total de arquivos: ").append(totalFiles).append("\n");
        analysis.append("Tamanho total: ").append(FileUtils.byteCountToDisplaySize(totalSize)).append("\n");
        analysis.append("Extensões encontradas:\n");
        
        extensionCount.entrySet().stream()
                     .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                     .forEach(entry -> {
                         String ext = entry.getKey().isEmpty() ? "sem extensão" : entry.getKey();
                         long count = entry.getValue();
                         long size = extensionSize.get(entry.getKey());
                         analysis.append("  ").append(ext).append(": ")
                                .append(count).append(" arquivos, ")
                                .append(FileUtils.byteCountToDisplaySize(size)).append("\n");
                     });

        item.setResult(analysis.toString());
        item.setMetadata(String.format("extensions:%d,files:%d,size:%d", 
                                      extensionCount.size(), totalFiles, totalSize));
    }

    private void processCompressFiles(FileProcessingQueue item) throws IOException {
        Path sourcePath = Paths.get(item.getSourcePath());
        Path targetPath = Paths.get(item.getTargetPath());
        
        if (!Files.exists(sourcePath)) {
            throw new IOException("Caminho de origem não encontrado: " + item.getSourcePath());
        }

        Files.createDirectories(targetPath.getParent());

        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(targetPath.toFile()))) {
            if (Files.isDirectory(sourcePath)) {
                compressDirectory(sourcePath, sourcePath, zos, item.getIncludeSubdirectories());
            } else {
                compressFile(sourcePath, sourcePath.getParent(), zos);
            }
        }

        long originalSize = calculateDirectorySize(sourcePath);
        long compressedSize = Files.size(targetPath);
        
        item.setTotalSizeBytes(originalSize);
        item.setFileCount(countFiles(sourcePath, item.getIncludeSubdirectories()));
        item.setResult(String.format("Compressão concluída: %s -> %s (%.1f%% de redução)",
                                   FileUtils.byteCountToDisplaySize(originalSize),
                                   FileUtils.byteCountToDisplaySize(compressedSize),
                                   (1.0 - (double)compressedSize / originalSize) * 100));
    }

    private void processDuplicateDetection(FileProcessingQueue item) throws Exception {
        Path sourcePath = Paths.get(item.getSourcePath());
        if (!Files.exists(sourcePath) || !Files.isDirectory(sourcePath)) {
            throw new IOException("Diretório não encontrado: " + item.getSourcePath());
        }

        Map<String, List<Path>> hashGroups = new HashMap<>();
        long totalFiles = 0;

        Files.walk(sourcePath)
             .filter(Files::isRegularFile)
             .forEach(file -> {
                 try {
                     String hash = calculateFileHash(file);
                     hashGroups.computeIfAbsent(hash, k -> new ArrayList<>()).add(file);
                 } catch (Exception e) {
                     logger.warn("Erro ao calcular hash do arquivo {}: {}", file, e.getMessage());
                 }
             });

        totalFiles = hashGroups.values().stream().mapToLong(List::size).sum();
        long duplicateGroups = hashGroups.values().stream().filter(list -> list.size() > 1).count();
        long duplicateFiles = hashGroups.values().stream()
                                       .filter(list -> list.size() > 1)
                                       .mapToLong(list -> list.size() - 1)
                                       .sum();

        item.setFileCount(totalFiles);
        
        StringBuilder result = new StringBuilder();
        result.append("Detecção de duplicatas:\n");
        result.append("Total de arquivos: ").append(totalFiles).append("\n");
        result.append("Grupos de duplicatas: ").append(duplicateGroups).append("\n");
        result.append("Arquivos duplicados: ").append(duplicateFiles).append("\n");

        if (item.getTargetPath() != null) {
            List<String> duplicateReport = new ArrayList<>();
            hashGroups.values().stream()
                     .filter(list -> list.size() > 1)
                     .forEach(list -> {
                         duplicateReport.add("Grupo de duplicatas:");
                         list.forEach(path -> duplicateReport.add("  " + path.toString()));
                         duplicateReport.add("");
                     });
            
            Path targetPath = Paths.get(item.getTargetPath());
            Files.createDirectories(targetPath.getParent());
            Files.write(targetPath, duplicateReport);
        }

        item.setResult(result.toString());
    }

    // Métodos auxiliares

    private String getFileExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        return lastDot > 0 ? fileName.substring(lastDot + 1) : "";
    }

    private void compressDirectory(Path sourceDir, Path baseDir, ZipOutputStream zos, Boolean includeSubdirs) throws IOException {
        Files.walk(sourceDir, includeSubdirs ? Integer.MAX_VALUE : 1)
             .filter(Files::isRegularFile)
             .forEach(file -> {
                 try {
                     compressFile(file, baseDir, zos);
                 } catch (IOException e) {
                     logger.warn("Erro ao comprimir arquivo {}: {}", file, e.getMessage());
                 }
             });
    }

    private void compressFile(Path file, Path baseDir, ZipOutputStream zos) throws IOException {
        String entryName = baseDir.relativize(file).toString().replace('\\', '/');
        ZipEntry entry = new ZipEntry(entryName);
        zos.putNextEntry(entry);
        
        try (FileInputStream fis = new FileInputStream(file.toFile())) {
            byte[] buffer = new byte[8192];
            int length;
            while ((length = fis.read(buffer)) > 0) {
                zos.write(buffer, 0, length);
            }
        }
        
        zos.closeEntry();
    }

    private long calculateDirectorySize(Path directory) throws IOException {
        if (Files.isRegularFile(directory)) {
            return Files.size(directory);
        }
        
        return Files.walk(directory)
                    .filter(Files::isRegularFile)
                    .mapToLong(file -> {
                        try {
                            return Files.size(file);
                        } catch (IOException e) {
                            return 0L;
                        }
                    })
                    .sum();
    }

    private long countFiles(Path directory, Boolean includeSubdirs) throws IOException {
        if (Files.isRegularFile(directory)) {
            return 1L;
        }
        
        return Files.walk(directory, includeSubdirs ? Integer.MAX_VALUE : 1)
                    .filter(Files::isRegularFile)
                    .count();
    }

    private String calculateFileHash(Path file) throws Exception {
        MessageDigest md = MessageDigest.getInstance("MD5");
        try (FileInputStream fis = new FileInputStream(file.toFile())) {
            byte[] buffer = new byte[8192];
            int length;
            while ((length = fis.read(buffer)) > 0) {
                md.update(buffer, 0, length);
            }
        }
        
        StringBuilder sb = new StringBuilder();
        for (byte b : md.digest()) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    // Implementações simplificadas para outras operações
    private void processExtractFiles(FileProcessingQueue item) throws IOException {
        item.setResult("Extração de arquivos não implementada nesta versão");
    }

    private void processOrganizeFiles(FileProcessingQueue item) throws IOException {
        item.setResult("Organização de arquivos não implementada nesta versão");
    }

    private void processFileValidation(FileProcessingQueue item) throws IOException {
        item.setResult("Validação de arquivos não implementada nesta versão");
    }

    private void processMetadataExtraction(FileProcessingQueue item) throws IOException {
        item.setResult("Extração de metadados não implementada nesta versão");
    }

    private void processBatchRename(FileProcessingQueue item) throws IOException {
        item.setResult("Renomeação em lote não implementada nesta versão");
    }

    private void processSyncDirectories(FileProcessingQueue item) throws IOException {
        item.setResult("Sincronização de diretórios não implementada nesta versão");
    }
}