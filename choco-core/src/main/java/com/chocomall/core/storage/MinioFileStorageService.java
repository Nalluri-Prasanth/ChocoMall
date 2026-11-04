package com.chocomall.core.storage;

import com.chocomall.core.config.StorageProperties;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import java.io.InputStream;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/** MinIO / S3-compatible storage for production ({@code chocomall.storage.type=minio}). */
@Service
@ConditionalOnProperty(name = "chocomall.storage.type", havingValue = "minio")
public class MinioFileStorageService implements FileStorageService {

    private final MinioClient client;
    private final String endpoint;
    private final String bucket;
    private volatile boolean bucketReady;

    public MinioFileStorageService(StorageProperties props) {
        StorageProperties.Minio m = props.minio();
        if (m == null || m.endpoint() == null || m.bucket() == null) {
            throw new IllegalStateException("chocomall.storage.minio.* must be configured when type=minio");
        }
        this.endpoint = m.endpoint().replaceAll("/+$", "");
        this.bucket = m.bucket();
        this.client = MinioClient.builder().endpoint(m.endpoint()).credentials(m.accessKey(), m.secretKey()).build();
    }

    @Override
    public String store(MultipartFile file) {
        String name = UUID.randomUUID() + FileStorageService.validatedExtension(file);
        try (InputStream in = file.getInputStream()) {
            ensureBucket();
            client.putObject(PutObjectArgs.builder().bucket(bucket).object(name)
                    .stream(in, file.getSize(), -1).contentType(file.getContentType()).build());
        } catch (Exception e) {
            throw new IllegalStateException("Could not upload file to object storage", e);
        }
        return endpoint + "/" + bucket + "/" + name;
    }

    private void ensureBucket() throws Exception {
        if (!bucketReady) {
            if (!client.bucketExists(BucketExistsArgs.builder().bucket(bucket).build())) {
                client.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
            }
            bucketReady = true;
        }
    }
}
