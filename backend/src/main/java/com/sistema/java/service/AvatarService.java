package com.sistema.java.service;

import com.sistema.java.model.entity.Usuario;
import com.sistema.java.model.dto.AvatarUploadDTO;
import com.sistema.java.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Serviço para processamento de avatars de usuários
 * Referência: Regras de Avatar - project_rules.md
 * Referência: Regras de Validação de Perfil - project_rules.md
 */
@Service
public class AvatarService {

    private static final Logger logger = LoggerFactory.getLogger(AvatarService.class);
    
    private final UsuarioRepository usuarioRepository;
    
    @Value("${aplicacao.avatar.diretorio:${java.io.tmpdir}/avatars}")
    private String diretorioAvatars;
    
    @Value("${aplicacao.avatar.tamanhos.pequeno:64}")
    private int tamanhoP;
    
    @Value("${aplicacao.avatar.tamanhos.medio:256}")
    private int tamanhoM;
    
    @Value("${aplicacao.avatar.tamanhos.grande:512}")
    private int tamanhoG;
    
    @Value("${aplicacao.avatar.tamanho-maximo:5242880}")
    private long tamanhoMaximo; // 5MB
    
    private static final List<String> FORMATOS_ACEITOS = Arrays.asList(
        "image/jpeg", "image/jpg", "image/png"
    );
    
    private static final List<String> EXTENSOES_ACEITAS = Arrays.asList(
        "jpg", "jpeg", "png"
    );

    public AvatarService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    /**
     * Processa upload de avatar de forma assíncrona
     * Referência: Regras de Avatar - project_rules.md
     */
    @Async
    public CompletableFuture<String> processarUploadAvatar(Long usuarioId, MultipartFile arquivo, 
                                                           AvatarUploadDTO cropInfo) {
        try {
            logger.info("Iniciando processamento de avatar para usuário: {}", usuarioId);
            
            // Validar arquivo
            validarArquivo(arquivo);
            
            // Criar diretório se não existir
            criarDiretorioSeNecessario();
            
            // Gerar nome único para o arquivo
            String nomeArquivo = gerarNomeArquivo(usuarioId);
            
            // Ler imagem original
            BufferedImage imagemOriginal = ImageIO.read(arquivo.getInputStream());
            if (imagemOriginal == null) {
                throw new IllegalArgumentException("Arquivo não é uma imagem válida");
            }
            
            // Aplicar crop se especificado
            BufferedImage imagemCropada = aplicarCrop(imagemOriginal, cropInfo);
            
            // Gerar diferentes tamanhos
            gerarTamanhos(imagemCropada, nomeArquivo);
            
            // Atualizar usuário com novo avatar
            String caminhoAvatar = "/avatars/" + nomeArquivo;
            atualizarAvatarUsuario(usuarioId, caminhoAvatar);
            
            logger.info("Avatar processado com sucesso para usuário: {}", usuarioId);
            return CompletableFuture.completedFuture(caminhoAvatar);
            
        } catch (Exception e) {
            logger.error("Erro ao processar avatar para usuário {}: {}", usuarioId, e.getMessage(), e);
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * Valida arquivo de upload
     * Referência: Regras de Avatar - project_rules.md
     */
    private void validarArquivo(MultipartFile arquivo) {
        if (arquivo == null || arquivo.isEmpty()) {
            throw new IllegalArgumentException("Arquivo não pode estar vazio");
        }
        
        if (arquivo.getSize() > tamanhoMaximo) {
            throw new IllegalArgumentException(
                String.format("Arquivo muito grande. Tamanho máximo: %d bytes", tamanhoMaximo)
            );
        }
        
        String contentType = arquivo.getContentType();
        if (contentType == null || !FORMATOS_ACEITOS.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException(
                "Formato não aceito. Formatos válidos: " + String.join(", ", FORMATOS_ACEITOS)
            );
        }
        
        String nomeOriginal = arquivo.getOriginalFilename();
        if (nomeOriginal != null) {
            String extensao = obterExtensao(nomeOriginal).toLowerCase();
            if (!EXTENSOES_ACEITAS.contains(extensao)) {
                throw new IllegalArgumentException(
                    "Extensão não aceita. Extensões válidas: " + String.join(", ", EXTENSOES_ACEITAS)
                );
            }
        }
    }

    /**
     * Aplica crop na imagem conforme especificado
     * Referência: Regras de Avatar - project_rules.md
     */
    private BufferedImage aplicarCrop(BufferedImage imagemOriginal, AvatarUploadDTO cropInfo) {
        if (cropInfo == null || !cropInfo.temCropInfo()) {
            // Crop centralizado automático para quadrado
            return cropCentralizado(imagemOriginal);
        }
        
        // Crop personalizado
        int x = Math.max(0, cropInfo.getX());
        int y = Math.max(0, cropInfo.getY());
        int largura = Math.min(cropInfo.getLargura(), imagemOriginal.getWidth() - x);
        int altura = Math.min(cropInfo.getAltura(), imagemOriginal.getHeight() - y);
        
        if (largura <= 0 || altura <= 0) {
            logger.warn("Coordenadas de crop inválidas, aplicando crop centralizado");
            return cropCentralizado(imagemOriginal);
        }
        
        return imagemOriginal.getSubimage(x, y, largura, altura);
    }

    /**
     * Aplica crop centralizado para criar imagem quadrada
     */
    private BufferedImage cropCentralizado(BufferedImage imagem) {
        int largura = imagem.getWidth();
        int altura = imagem.getHeight();
        int tamanho = Math.min(largura, altura);
        
        int x = (largura - tamanho) / 2;
        int y = (altura - tamanho) / 2;
        
        return imagem.getSubimage(x, y, tamanho, tamanho);
    }

    /**
     * Gera diferentes tamanhos do avatar
     * Referência: Regras de Avatar - project_rules.md
     */
    private void gerarTamanhos(BufferedImage imagemCropada, String nomeArquivo) throws IOException {
        // Tamanho pequeno (64x64)
        BufferedImage pequeno = redimensionar(imagemCropada, tamanhoP, tamanhoP);
        salvarImagem(pequeno, nomeArquivo + "_64.jpg");
        
        // Tamanho médio (256x256)
        BufferedImage medio = redimensionar(imagemCropada, tamanhoM, tamanhoM);
        salvarImagem(medio, nomeArquivo + "_256.jpg");
        
        // Tamanho grande (512x512)
        BufferedImage grande = redimensionar(imagemCropada, tamanhoG, tamanhoG);
        salvarImagem(grande, nomeArquivo + "_512.jpg");
        
        // Imagem original processada
        salvarImagem(imagemCropada, nomeArquivo + "_original.jpg");
    }

    /**
     * Redimensiona imagem mantendo qualidade
     */
    private BufferedImage redimensionar(BufferedImage imagemOriginal, int largura, int altura) {
        BufferedImage imagemRedimensionada = new BufferedImage(largura, altura, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = imagemRedimensionada.createGraphics();
        
        // Configurações para melhor qualidade
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        g2d.drawImage(imagemOriginal, 0, 0, largura, altura, null);
        g2d.dispose();
        
        return imagemRedimensionada;
    }

    /**
     * Salva imagem no sistema de arquivos
     */
    private void salvarImagem(BufferedImage imagem, String nomeArquivo) throws IOException {
        Path caminhoCompleto = Paths.get(diretorioAvatars, nomeArquivo);
        File arquivo = caminhoCompleto.toFile();
        ImageIO.write(imagem, "jpg", arquivo);
        logger.debug("Imagem salva: {}", caminhoCompleto);
    }

    /**
     * Atualiza o campo avatar do usuário
     */
    private void atualizarAvatarUsuario(Long usuarioId, String caminhoAvatar) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
            .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado: " + usuarioId));
        
        // Remover avatar anterior se existir
        if (usuario.getAvatar() != null && !usuario.getAvatar().isEmpty()) {
            removerAvatarAnterior(usuario.getAvatar());
        }
        
        usuario.setAvatar(caminhoAvatar);
        usuario.setDataAtualizacao(LocalDateTime.now());
        usuarioRepository.save(usuario);
        
        logger.info("Avatar atualizado para usuário: {}", usuarioId);
    }

    /**
     * Remove arquivos do avatar anterior
     */
    private void removerAvatarAnterior(String caminhoAvatar) {
        try {
            String nomeBase = obterNomeBase(caminhoAvatar);
            
            // Remover todos os tamanhos
            String[] sufixos = {"_64.jpg", "_256.jpg", "_512.jpg", "_original.jpg"};
            for (String sufixo : sufixos) {
                Path arquivo = Paths.get(diretorioAvatars, nomeBase + sufixo);
                Files.deleteIfExists(arquivo);
            }
            
            logger.debug("Avatar anterior removido: {}", nomeBase);
        } catch (Exception e) {
            logger.warn("Erro ao remover avatar anterior: {}", e.getMessage());
        }
    }

    /**
     * Obtém avatar do usuário em tamanho específico
     */
    public String obterAvatarUsuario(Long usuarioId, String tamanho) {
        Usuario usuario = usuarioRepository.findById(usuarioId).orElse(null);
        if (usuario == null || usuario.getAvatar() == null) {
            return obterAvatarPadrao(tamanho);
        }
        
        String nomeBase = obterNomeBase(usuario.getAvatar());
        String sufixo = obterSufixoPorTamanho(tamanho);
        
        Path caminhoAvatar = Paths.get(diretorioAvatars, nomeBase + sufixo);
        if (Files.exists(caminhoAvatar)) {
            return "/avatars/" + nomeBase + sufixo;
        }
        
        return obterAvatarPadrao(tamanho);
    }

    /**
     * Retorna avatar padrão para tamanho específico
     */
    private String obterAvatarPadrao(String tamanho) {
        return "/images/avatar-default-" + tamanho + ".svg";
    }

    /**
     * Obtém sufixo do arquivo baseado no tamanho
     */
    private String obterSufixoPorTamanho(String tamanho) {
        return switch (tamanho.toLowerCase()) {
            case "pequeno", "small", "64" -> "_64.jpg";
            case "medio", "medium", "256" -> "_256.jpg";
            case "grande", "large", "512" -> "_512.jpg";
            case "original" -> "_original.jpg";
            default -> "_256.jpg"; // Padrão médio
        };
    }

    /**
     * Gera nome único para arquivo de avatar
     */
    private String gerarNomeArquivo(Long usuarioId) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        return String.format("avatar_%d_%s_%s", usuarioId, timestamp, uuid);
    }

    /**
     * Obtém nome base do arquivo (sem sufixo de tamanho)
     */
    private String obterNomeBase(String caminhoAvatar) {
        String nomeArquivo = Paths.get(caminhoAvatar).getFileName().toString();
        // Remove sufixos como _64.jpg, _256.jpg, etc.
        return nomeArquivo.replaceAll("_(64|256|512|original)\\.(jpg|jpeg|png)$", "");
    }

    /**
     * Obtém extensão do arquivo
     */
    private String obterExtensao(String nomeArquivo) {
        int ultimoPonto = nomeArquivo.lastIndexOf('.');
        return ultimoPonto > 0 ? nomeArquivo.substring(ultimoPonto + 1) : "";
    }

    /**
     * Cria diretório de avatars se não existir
     */
    private void criarDiretorioSeNecessario() throws IOException {
        Path diretorio = Paths.get(diretorioAvatars);
        if (!Files.exists(diretorio)) {
            Files.createDirectories(diretorio);
            logger.info("Diretório de avatars criado: {}", diretorio);
        }
    }

    /**
     * Verifica se usuário pode editar avatar
     * Referência: Regras de Edição de Perfil - project_rules.md
     */
    public boolean podeEditarAvatar(Long usuarioId, Long usuarioLogadoId) {
        // Usuário só pode editar seu próprio avatar
        return usuarioId.equals(usuarioLogadoId);
    }

    /**
     * Obtém informações de uso de espaço
     */
    public long calcularEspacoUsado() {
        try {
            Path diretorio = Paths.get(diretorioAvatars);
            if (!Files.exists(diretorio)) {
                return 0;
            }
            
            return Files.walk(diretorio)
                .filter(Files::isRegularFile)
                .mapToLong(path -> {
                    try {
                        return Files.size(path);
                    } catch (IOException e) {
                        return 0;
                    }
                })
                .sum();
        } catch (IOException e) {
            logger.error("Erro ao calcular espaço usado: {}", e.getMessage());
            return 0;
        }
    }

    /**
     * Limpa avatars órfãos (sem usuário associado)
     */
    @Async
    public CompletableFuture<Integer> limparAvatarsOrfaos() {
        try {
            Path diretorio = Paths.get(diretorioAvatars);
            if (!Files.exists(diretorio)) {
                return CompletableFuture.completedFuture(0);
            }
            
            List<String> avatarsEmUso = usuarioRepository.findAllAvatarsEmUso();
            int removidos = 0;
            
            Files.walk(diretorio)
                .filter(Files::isRegularFile)
                .forEach(path -> {
                    String nomeArquivo = path.getFileName().toString();
                    String nomeBase = obterNomeBase("/" + nomeArquivo);
                    
                    boolean emUso = avatarsEmUso.stream()
                        .anyMatch(avatar -> obterNomeBase(avatar).equals(nomeBase));
                    
                    if (!emUso) {
                        try {
                            Files.delete(path);
                            logger.debug("Avatar órfão removido: {}", nomeArquivo);
                        } catch (IOException e) {
                            logger.warn("Erro ao remover avatar órfão {}: {}", nomeArquivo, e.getMessage());
                        }
                    }
                });
            
            logger.info("Limpeza de avatars órfãos concluída. Removidos: {}", removidos);
            return CompletableFuture.completedFuture(removidos);
            
        } catch (Exception e) {
            logger.error("Erro na limpeza de avatars órfãos: {}", e.getMessage(), e);
            return CompletableFuture.failedFuture(e);
        }
    }
}