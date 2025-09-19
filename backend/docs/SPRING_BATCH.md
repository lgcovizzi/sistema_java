# Spring Batch - Documentação Técnica

## 📋 Visão Geral

O sistema implementa Spring Batch 5.x para processamento em lote robusto e escalável, gerenciando três tipos principais de operações:

1. **Envio de Emails em Lote**
2. **Redimensionamento de Imagens**
3. **Processamento de Arquivos**

## 🏗️ Arquitetura

### Componentes Principais

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Controllers   │    │    Services     │    │   Batch Jobs    │
│                 │    │                 │    │                 │
│ BatchJobCtrl    │───▶│ BatchJobService │───▶│ EmailJobConfig  │
│ EmailQueueCtrl  │    │ EmailService    │    │ ImageJobConfig  │
│ ImageQueueCtrl  │    │ ImageService    │    │ FileJobConfig   │
│ FileQueueCtrl   │    │ FileService     │    │                 │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
         ▼                       ▼                       ▼
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Repositories  │    │    Entities     │    │  Spring Batch   │
│                 │    │                 │    │   Metadata      │
│ EmailQueueRepo  │    │ EmailQueue      │    │                 │
│ ImageQueueRepo  │    │ ImageResizeQueue│    │ BATCH_JOB_*     │
│ FileQueueRepo   │    │ FileProcessQueue│    │ BATCH_STEP_*    │
│ BatchJobRepo    │    │ BatchJob        │    │ BATCH_EXECUTION │
└─────────────────┘    └─────────────────┘    └─────────────────┘
```

## 📊 Entidades de Fila

### 1. EmailQueue

```java
@Entity
@Table(name = "email_queue")
public class EmailQueue {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String toEmail;
    
    @Column(nullable = false)
    private String subject;
    
    @Column(columnDefinition = "TEXT")
    private String body;
    
    @Enumerated(EnumType.STRING)
    private Priority priority = Priority.MEDIUM;
    
    @Enumerated(EnumType.STRING)
    private ProcessingStatus status = ProcessingStatus.PENDING;
    
    private Integer attempts = 0;
    private Integer maxAttempts = 3;
    
    // Timestamps e outros campos...
}
```

**Enums:**
- `Priority`: LOW, MEDIUM, HIGH, URGENT
- `ProcessingStatus`: PENDING, PROCESSING, COMPLETED, FAILED

### 2. ImageResizeQueue

```java
@Entity
@Table(name = "image_resize_queue")
public class ImageResizeQueue {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String originalPath;
    
    private String targetPath;
    private Integer targetWidth;
    private Integer targetHeight;
    
    @Enumerated(EnumType.STRING)
    private ResizeMode resizeMode = ResizeMode.SCALE_TO_FIT;
    
    private String format = "JPEG";
    private Float quality = 85.0f;
    
    // Campos de processamento...
}
```

**Enums:**
- `ResizeMode`: SCALE_TO_FIT, CROP, STRETCH, SCALE_TO_FILL
- `ProcessingStatus`: PENDING, PROCESSING, COMPLETED, FAILED
- `Priority`: LOW, MEDIUM, HIGH, URGENT

### 3. FileProcessingQueue

```java
@Entity
@Table(name = "file_processing_queue")
public class FileProcessingQueue {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String sourcePath;
    
    private String targetPath;
    
    @Enumerated(EnumType.STRING)
    private OperationType operationType;
    
    private String filePattern;
    private Boolean includeSubdirectories = false;
    private Boolean deleteSourceAfterProcessing = false;
    
    // Campos de resultado...
}
```

**Enums:**
- `OperationType`: LIST_FILES, ANALYZE_FILES, COMPRESS_FILES, FIND_DUPLICATES
- `ProcessingStatus`: PENDING, PROCESSING, COMPLETED, FAILED
- `Priority`: LOW, MEDIUM, HIGH, URGENT

## 🎯 Configurações de Jobs

### 1. EmailJobConfig

```java
@Configuration
@EnableBatchProcessing
public class EmailJobConfig {
    
    @Bean
    @StepScope
    public JpaPagingItemReader<EmailQueue> emailReader() {
        return new JpaPagingItemReaderBuilder<EmailQueue>()
                .name("emailReader")
                .entityManagerFactory(entityManagerFactory)
                .queryString("SELECT e FROM EmailQueue e WHERE e.status = 'PENDING' ORDER BY e.priority DESC, e.createdAt ASC")
                .pageSize(10)
                .build();
    }
    
    @Bean
    public ItemProcessor<EmailQueue, EmailQueue> emailProcessor() {
        return email -> {
            // Lógica de processamento do email
            email.setStatus(ProcessingStatus.PROCESSING);
            email.setStartedAt(LocalDateTime.now());
            return email;
        };
    }
    
    @Bean
    public ItemWriter<EmailQueue> emailWriter() {
        return emails -> {
            for (EmailQueue email : emails) {
                try {
                    emailService.sendEmail(email);
                    email.setStatus(ProcessingStatus.COMPLETED);
                    email.setCompletedAt(LocalDateTime.now());
                } catch (Exception e) {
                    email.setStatus(ProcessingStatus.FAILED);
                    email.setErrorMessage(e.getMessage());
                    email.setAttempts(email.getAttempts() + 1);
                }
                emailQueueRepository.save(email);
            }
        };
    }
    
    @Bean
    public Job emailJob() {
        return jobBuilderFactory.get("emailJob")
                .start(emailStep())
                .build();
    }
    
    @Bean
    public Step emailStep() {
        return stepBuilderFactory.get("emailStep")
                .<EmailQueue, EmailQueue>chunk(10)
                .reader(emailReader())
                .processor(emailProcessor())
                .writer(emailWriter())
                .faultTolerant()
                .retryLimit(3)
                .retry(Exception.class)
                .build();
    }
}
```

### 2. ImageResizeJobConfig

```java
@Configuration
public class ImageResizeJobConfig {
    
    @Bean
    @StepScope
    public JpaPagingItemReader<ImageResizeQueue> imageReader() {
        return new JpaPagingItemReaderBuilder<ImageResizeQueue>()
                .name("imageReader")
                .entityManagerFactory(entityManagerFactory)
                .queryString("SELECT i FROM ImageResizeQueue i WHERE i.status = 'PENDING' ORDER BY i.priority DESC, i.createdAt ASC")
                .pageSize(5) // Menor chunk para imagens
                .build();
    }
    
    @Bean
    public ItemProcessor<ImageResizeQueue, ImageResizeQueue> imageProcessor() {
        return image -> {
            try {
                // Lógica de redimensionamento
                BufferedImage originalImage = ImageIO.read(new File(image.getOriginalPath()));
                BufferedImage resizedImage = resizeImage(originalImage, image);
                
                String targetPath = generateTargetPath(image);
                ImageIO.write(resizedImage, image.getFormat(), new File(targetPath));
                
                image.setTargetPath(targetPath);
                image.setStatus(ProcessingStatus.COMPLETED);
                image.setCompletedAt(LocalDateTime.now());
                
                return image;
            } catch (Exception e) {
                image.setStatus(ProcessingStatus.FAILED);
                image.setErrorMessage(e.getMessage());
                image.setAttempts(image.getAttempts() + 1);
                return image;
            }
        };
    }
    
    // Métodos auxiliares para redimensionamento...
}
```

### 3. FileProcessingJobConfig

```java
@Configuration
public class FileProcessingJobConfig {
    
    @Bean
    public ItemProcessor<FileProcessingQueue, FileProcessingQueue> fileProcessor() {
        return file -> {
            try {
                switch (file.getOperationType()) {
                    case LIST_FILES:
                        return processListFiles(file);
                    case ANALYZE_FILES:
                        return processAnalyzeFiles(file);
                    case COMPRESS_FILES:
                        return processCompressFiles(file);
                    case FIND_DUPLICATES:
                        return processFindDuplicates(file);
                    default:
                        throw new IllegalArgumentException("Tipo de operação não suportado");
                }
            } catch (Exception e) {
                file.setStatus(ProcessingStatus.FAILED);
                file.setErrorMessage(e.getMessage());
                file.setAttempts(file.getAttempts() + 1);
                return file;
            }
        };
    }
    
    private FileProcessingQueue processListFiles(FileProcessingQueue file) {
        Path sourcePath = Paths.get(file.getSourcePath());
        List<String> fileList = new ArrayList<>();
        
        try (Stream<Path> paths = Files.walk(sourcePath)) {
            fileList = paths
                .filter(Files::isRegularFile)
                .map(Path::toString)
                .collect(Collectors.toList());
        }
        
        file.setFileCount((long) fileList.size());
        file.setResultData(String.join("\n", fileList));
        file.setStatus(ProcessingStatus.COMPLETED);
        
        return file;
    }
    
    // Outros métodos de processamento...
}
```

## 🎮 Controladores REST

### BatchJobController

```java
@RestController
@RequestMapping("/api/batch")
@PreAuthorize("hasRole('USER')")
public class BatchJobController {
    
    @PostMapping("/execute/{jobType}")
    public ResponseEntity<Map<String, Object>> executeJob(
            @PathVariable String jobType,
            @RequestParam(defaultValue = "false") boolean async) {
        
        JobExecution execution = batchJobService.executeJob(jobType, async);
        
        Map<String, Object> response = new HashMap<>();
        response.put("jobId", execution.getJobId());
        response.put("status", execution.getStatus().toString());
        response.put("startTime", execution.getStartTime());
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<BatchJob> getJob(@PathVariable Long id) {
        return batchJobService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping
    public ResponseEntity<Page<BatchJob>> getAllJobs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String jobType) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<BatchJob> jobs = batchJobService.findAll(pageable, status, jobType);
        
        return ResponseEntity.ok(jobs);
    }
    
    @GetMapping("/statistics")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        return ResponseEntity.ok(batchJobService.getJobStatistics());
    }
}
```

## 📊 Monitoramento e Métricas

### Estatísticas Disponíveis

```java
public Map<String, Object> getJobStatistics() {
    Map<String, Object> stats = new HashMap<>();
    
    // Estatísticas gerais
    stats.put("totalJobs", batchJobRepository.count());
    stats.put("successfulJobs", batchJobRepository.countByStatus(JobStatus.COMPLETED));
    stats.put("failedJobs", batchJobRepository.countByStatus(JobStatus.FAILED));
    stats.put("runningJobs", batchJobRepository.countByStatus(JobStatus.RUNNING));
    
    // Estatísticas por tipo
    Map<String, Long> jobsByType = new HashMap<>();
    for (JobType type : JobType.values()) {
        jobsByType.put(type.name(), batchJobRepository.countByJobType(type));
    }
    stats.put("jobsByType", jobsByType);
    
    // Estatísticas das filas
    Map<String, Long> queueSizes = new HashMap<>();
    queueSizes.put("emailQueue", emailQueueRepository.countByStatus(ProcessingStatus.PENDING));
    queueSizes.put("imageQueue", imageResizeQueueRepository.countByStatus(ProcessingStatus.PENDING));
    queueSizes.put("fileQueue", fileProcessingQueueRepository.countByStatus(ProcessingStatus.PENDING));
    stats.put("queueSizes", queueSizes);
    
    return stats;
}
```

### Logs e Auditoria

```java
@Component
public class BatchJobListener implements JobExecutionListener {
    
    private static final Logger logger = LoggerFactory.getLogger(BatchJobListener.class);
    
    @Override
    public void beforeJob(JobExecution jobExecution) {
        logger.info("Iniciando job: {} com parâmetros: {}", 
                   jobExecution.getJobInstance().getJobName(),
                   jobExecution.getJobParameters());
    }
    
    @Override
    public void afterJob(JobExecution jobExecution) {
        logger.info("Job finalizado: {} com status: {} em {}ms",
                   jobExecution.getJobInstance().getJobName(),
                   jobExecution.getStatus(),
                   jobExecution.getEndTime().getTime() - jobExecution.getStartTime().getTime());
    }
}
```

## 🔧 Configuração

### application.yml

```yaml
spring:
  batch:
    job:
      enabled: false  # Não executar jobs automaticamente na inicialização
    jdbc:
      initialize-schema: always
      table-prefix: BATCH_
    
  datasource:
    url: jdbc:h2:mem:batchdb
    driver-class-name: org.h2.Driver
    username: sa
    password: 
    
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: false
    properties:
      hibernate:
        format_sql: true

app:
  batch:
    chunk-size: 10
    thread-pool-size: 5
    retry-limit: 3
    cleanup-days: 30
    max-concurrent-jobs: 3
    
logging:
  level:
    org.springframework.batch: INFO
    com.sistema.batch: DEBUG
```

### Configuração de Thread Pool

```java
@Configuration
public class BatchConfig {
    
    @Bean
    public TaskExecutor batchTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(25);
        executor.setThreadNamePrefix("batch-");
        executor.initialize();
        return executor;
    }
    
    @Bean
    public JobRepository jobRepository() throws Exception {
        JobRepositoryFactoryBean factory = new JobRepositoryFactoryBean();
        factory.setDataSource(dataSource);
        factory.setTransactionManager(transactionManager);
        factory.setIsolationLevelForCreate("ISOLATION_SERIALIZABLE");
        factory.setTablePrefix("BATCH_");
        factory.setMaxVarCharLength(1000);
        return factory.getObject();
    }
}
```

## 🚀 Exemplos de Uso

### 1. Adicionar Email à Fila

```bash
curl -X POST http://localhost:8080/api/email-queue/add \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${TOKEN}" \
  -d '{
    "toEmail": "usuario@example.com",
    "subject": "Bem-vindo ao Sistema",
    "body": "Obrigado por se cadastrar!",
    "priority": "HIGH",
    "template": "welcome"
  }'
```

### 2. Executar Job de Emails

```bash
curl -X POST http://localhost:8080/api/batch/execute/EMAIL \
  -H "Authorization: Bearer ${TOKEN}"
```

### 3. Monitorar Job

```bash
curl -X GET http://localhost:8080/api/batch/12345 \
  -H "Authorization: Bearer ${TOKEN}"
```

### 4. Obter Estatísticas

```bash
curl -X GET http://localhost:8080/api/batch/statistics \
  -H "Authorization: Bearer ${TOKEN}"
```

## 🛡️ Segurança

### Autenticação e Autorização

- Todos os endpoints requerem autenticação JWT
- Operações administrativas requerem role ADMIN
- Usuários só podem ver seus próprios jobs
- Logs de auditoria para todas as operações

### Validação de Entrada

```java
@PostMapping("/add")
public ResponseEntity<EmailQueue> addEmail(@Valid @RequestBody AddEmailRequest request) {
    // Validação automática via Bean Validation
    EmailQueue email = emailQueueService.addToQueue(request);
    return ResponseEntity.ok(email);
}
```

## 🔍 Troubleshooting

### Problemas Comuns

1. **Job não inicia**: Verificar se há jobs em execução (limite de concorrência)
2. **Falhas de processamento**: Verificar logs e configuração de retry
3. **Performance lenta**: Ajustar chunk-size e thread-pool-size
4. **Memória insuficiente**: Reduzir chunk-size para jobs de imagem

### Comandos de Diagnóstico

```bash
# Verificar jobs em execução
curl -X GET http://localhost:8080/api/batch/running

# Verificar estatísticas
curl -X GET http://localhost:8080/api/batch/statistics

# Limpar jobs antigos
curl -X DELETE http://localhost:8080/api/batch/cleanup
```

## 📈 Performance

### Otimizações Implementadas

- **Chunk Processing**: Processamento em lotes para eficiência
- **Paginação**: Leitura paginada para grandes volumes
- **Thread Pool**: Processamento paralelo configurável
- **Retry Logic**: Tentativas automáticas em caso de falha
- **Cleanup Automático**: Remoção de dados antigos

### Métricas de Performance

- Tempo médio de processamento por item
- Taxa de throughput (itens/segundo)
- Taxa de sucesso/falha
- Utilização de memória e CPU
- Tamanho das filas em tempo real

---

**Documentação atualizada em:** Janeiro 2024  
**Versão do Spring Batch:** 5.x  
**Compatibilidade:** Spring Boot 3.x