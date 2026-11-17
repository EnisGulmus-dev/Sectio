package com.enis.sectio.user;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Path;
import static org.assertj.core.api.Assertions.*;

class ProfilePhotoStoreTest {
    @TempDir Path root;

    @Test void validatesAndStoresAnImageAndRemovesItOnRequest() throws Exception {
        var store = new ProfilePhotoStore(root.toString());
        var bytes = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(16, 16, BufferedImage.TYPE_INT_RGB), "png", bytes);
        String key = store.save(new MockMultipartFile("file", "photo.png", "image/png", bytes.toByteArray()));
        assertThat(store.read(key)).isNotEmpty();
        store.deleteAfterCommit(key);
        assertThatThrownBy(() -> store.read(key)).isInstanceOf(ResponseStatusException.class);
    }

    @Test void rejectsFakeAndOversizedFiles() {
        var store = new ProfilePhotoStore(root.toString());
        assertThatThrownBy(() -> store.save(new MockMultipartFile("file", "fake.png", "image/png", new byte[]{1, 2, 3})))
                .isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> store.save(new MockMultipartFile("file", "big.png", "image/png", new byte[2_000_001])))
                .isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> store.read("../secret.png")).isInstanceOf(IllegalArgumentException.class);
    }
}
