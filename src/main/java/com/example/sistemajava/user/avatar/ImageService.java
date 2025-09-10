package com.example.sistemajava.user.avatar;

import net.coobird.thumbnailator.Thumbnails;
import net.coobird.thumbnailator.geometry.Region;
import net.coobird.thumbnailator.geometry.Positions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Service
public class ImageService {

    private static String staticUploadsDir;

    public ImageService(@Value("${app.uploadsDir:uploads}") String uploadsDir) throws IOException {
        staticUploadsDir = uploadsDir;
        Files.createDirectories(Path.of(staticUploadsDir));
    }

    public static String processAndStore(String userId, MultipartFile file, Integer x, Integer y, Integer w, Integer h) throws IOException {
        BufferedImage src = ImageIO.read(file.getInputStream());

        if (src == null) {
            throw new IOException("Imagem inválida");
        }

        BufferedImage cropped = src;
        if (x != null && y != null && w != null && h != null && w > 0 && h > 0) {
            int cx = Math.max(0, x);
            int cy = Math.max(0, y);
            int cw = Math.min(w, src.getWidth() - cx);
            int ch = Math.min(h, src.getHeight() - cy);
            cropped = src.getSubimage(cx, cy, cw, ch);
        }

        // Redimensiona e faz crop central para 300x300
        BufferedImage out = Thumbnails.of(cropped)
                .size(300, 300)
                .crop(Positions.CENTER)
                .asBufferedImage();

        String filename = "avatar-" + userId + "-" + UUID.randomUUID() + ".jpg";
        Path outPath = Path.of(staticUploadsDir, filename);
        ImageIO.write(out, "jpg", outPath.toFile());
        return "/uploads/" + filename;
    }
}


