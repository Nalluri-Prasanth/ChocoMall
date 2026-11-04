package com.chocomall.core.storage;

import com.chocomall.core.config.StorageProperties;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Local-disk storage for development, served back under {@code /files/**}. */
@Slf4j
@Service
@ConditionalOnProperty(name = "chocomall.storage.type", havingValue = "local", matchIfMissing = true)
public class LocalFileStorageService implements FileStorageService {

    private final Path root;
    private final String baseUrl;

    public LocalFileStorageService(StorageProperties props) {
        this.root = Path.of(props.localDir()).toAbsolutePath().normalize();
        this.baseUrl = props.publicBaseUrl();
    }

    @Override
    public String store(MultipartFile file) {
        String name = UUID.randomUUID() + FileStorageService.validatedExtension(file);
        try (InputStream in = file.getInputStream()) {
            Files.createDirectories(root);
            Path target = root.resolve(name).normalize();
            if (!target.startsWith(root)) {
                throw new IllegalStateException("Invalid target path");
            }
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return baseUrl + "/files/" + name;
    }

    /** Exposes the upload directory. Public files are images only, so serving them directly is safe. */
    @Configuration
    @ConditionalOnProperty(name = "chocomall.storage.type", havingValue = "local", matchIfMissing = true)
    static class ServeFiles implements WebMvcConfigurer {

        private final String location;

        ServeFiles(StorageProperties props) {
            this.location = Path.of(props.localDir()).toAbsolutePath().normalize().toUri().toString();
        }

        @Override
        public void addResourceHandlers(ResourceHandlerRegistry registry) {
            registry.addResourceHandler("/files/**").addResourceLocations(location);
        }
    }
}
