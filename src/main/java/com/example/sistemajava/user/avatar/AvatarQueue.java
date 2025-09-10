package com.example.sistemajava.user.avatar;

import net.coobird.thumbnailator.Thumbnails;
import net.coobird.thumbnailator.geometry.Positions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Service
@EnableAsync
public class AvatarQueue {
    private static final Logger log = LoggerFactory.getLogger(AvatarQueue.class);

    private final String uploadsDir;
    private final String uploadsTmpDir;

    public AvatarQueue(@Value("${app.uploadsDir:uploads}") String uploadsDir,
                       @Value("${app.uploadsTmpDir:tmp-uploads}") String uploadsTmpDir) throws IOException {
        this.uploadsDir = uploadsDir;
        this.uploadsTmpDir = uploadsTmpDir;
        Files.createDirectories(Path.of(uploadsDir));
        Files.createDirectories(Path.of(uploadsTmpDir));
    }

    public record Task(String userId, String tempFilename, Integer x, Integer y, Integer w, Integer h) {}

    @Async
    public void process(Task task) {
        Path tmpPath = Path.of(uploadsTmpDir, task.tempFilename);
        try {
            BufferedImage src = ImageIO.read(tmpPath.toFile());
            if (src == null) throw new IOException("Arquivo de imagem inválido");

            BufferedImage cropped = src;
            if (task.x != null && task.y != null && task.w != null && task.h != null && task.w > 0 && task.h > 0) {
                int cx = Math.max(0, task.x);
                int cy = Math.max(0, task.y);
                int cw = Math.min(task.w, src.getWidth() - cx);
                int ch = Math.min(task.h, src.getHeight() - cy);
                cropped = src.getSubimage(cx, cy, cw, ch);
            }

            BufferedImage out = Thumbnails.of(cropped)
                    .size(300, 300)
                    .crop(Positions.CENTER)
                    .asBufferedImage();

            String filename = "avatar-" + task.userId + "-" + UUID.randomUUID() + ".jpg";
            Path outPath = Path.of(uploadsDir, filename);
            ImageIO.write(out, "jpg", outPath.toFile());
        } catch (Exception e) {
            log.error("Falha ao processar avatar {}: {}", task.tempFilename, e.getMessage());
        } finally {
            try { Files.deleteIfExists(tmpPath); } catch (IOException ignore) {}
        }
    }
}


