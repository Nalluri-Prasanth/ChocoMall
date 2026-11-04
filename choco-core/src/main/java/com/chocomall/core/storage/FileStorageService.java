package com.chocomall.core.storage;

import com.chocomall.common.exception.Asserts;
import java.util.Map;
import org.springframework.web.multipart.MultipartFile;

/** Stores uploaded images and returns the URL clients should use. */
public interface FileStorageService {

    long MAX_BYTES = 5L * 1024 * 1024;

    Map<String, String> ALLOWED_TYPES = Map.of(
            "image/png", ".png", "image/jpeg", ".jpg", "image/webp", ".webp", "image/gif", ".gif");

    String store(MultipartFile file);

    /** Validates size and type; the extension is derived from the content type, never from the client file name. */
    static String validatedExtension(MultipartFile file) {
        Asserts.isTrue(file != null && !file.isEmpty(), "No file was uploaded");
        Asserts.isTrue(file.getSize() <= MAX_BYTES, "File is larger than 5 MB");
        String extension = ALLOWED_TYPES.get(file.getContentType());
        Asserts.isTrue(extension != null, "Only PNG, JPEG, WebP and GIF images are accepted");
        return extension;
    }
}
