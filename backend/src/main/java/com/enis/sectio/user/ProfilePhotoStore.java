package com.enis.sectio.user;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;

@Service
public class ProfilePhotoStore {
    private final Path root;
    public ProfilePhotoStore(@Value("${PROFILE_PHOTO_DIR:./profile-photos}") String root) { this.root = Path.of(root).toAbsolutePath().normalize(); }
    public String save(MultipartFile file) {
        if (file.isEmpty() || file.getSize() > 2_000_000) throw error("Fotoğraf en fazla 2 MB olmalı.");
        try {
            byte[] data = file.getBytes();
            try (ImageInputStream stream = ImageIO.createImageInputStream(new ByteArrayInputStream(data))) {
                var readers = ImageIO.getImageReaders(stream);
                if (!readers.hasNext()) throw error("PNG veya JPEG fotoğraf seçin.");
                ImageReader reader = readers.next();
                try {
                    reader.setInput(stream, true, true);
                    String format = reader.getFormatName();
                    if (!(format.equalsIgnoreCase("png") || format.equalsIgnoreCase("jpeg"))) throw error("PNG veya JPEG fotoğraf seçin.");
                    int width = reader.getWidth(0), height = reader.getHeight(0);
                    if (width < 1 || height < 1 || width > 4096 || height > 4096 || (long) width * height > 12_000_000) throw error("Fotoğraf boyutları çok büyük.");
                    var image = reader.read(0);
                    if (image == null) throw error("Fotoğraf okunamadı.");
                    Files.createDirectories(root);
                    String key = UUID.randomUUID() + ".png";
                    Path output = resolve(key);
                    try {
                        if (!ImageIO.write(image, "png", output.toFile())) throw new IOException("PNG writer unavailable");
                    } catch (IOException | RuntimeException failure) {
                        try { Files.deleteIfExists(output); } catch (IOException cleanup) { failure.addSuppressed(cleanup); }
                        throw failure;
                    }
                    afterCompletion(() -> delete(key), false);
                    return key;
                } finally { reader.dispose(); }
            }
        } catch (IOException e) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Fotoğraf kaydedilemedi.", e); }
    }
    public byte[] read(String key) {
        try { return Files.readAllBytes(resolve(key)); }
        catch (IOException e) { throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Fotoğraf bulunamadı."); }
    }
    public void deleteAfterCommit(String key) { if (key != null) afterCompletion(() -> delete(key), true); }
    private void delete(String key) { try { Files.deleteIfExists(resolve(key)); } catch (IOException e) { throw new IllegalStateException("Profil fotoğrafı silinemedi", e); } }
    private Path resolve(String key) {
        if (key == null || !key.matches("[0-9a-f-]{36}\\.png")) throw new IllegalArgumentException("Invalid photo key");
        return root.resolve(key);
    }
    private static void afterCompletion(Runnable action, boolean commit) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) { if (commit) action.run(); return; }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCompletion(int status) { if ((status == STATUS_COMMITTED) == commit) action.run(); }
        });
    }
    private static ResponseStatusException error(String message) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, message); }
}
