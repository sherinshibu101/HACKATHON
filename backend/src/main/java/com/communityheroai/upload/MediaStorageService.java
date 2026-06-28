package com.communityheroai.upload;

import com.communityheroai.exception.ResourceNotFoundException;
import com.communityheroai.issue.entity.IssueMediaType;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class MediaStorageService {
    private static final long MAX_IMAGE_SIZE = 5L * 1024 * 1024;
    private static final long MAX_VIDEO_SIZE = 50L * 1024 * 1024;
    private static final Map<String, MediaFormat> ALLOWED_TYPES = Map.of(
            "image/jpeg", new MediaFormat(IssueMediaType.IMAGE, ".jpg"),
            "image/png", new MediaFormat(IssueMediaType.IMAGE, ".png"),
            "image/webp", new MediaFormat(IssueMediaType.IMAGE, ".webp"),
            "video/mp4", new MediaFormat(IssueMediaType.VIDEO, ".mp4"),
            "video/webm", new MediaFormat(IssueMediaType.VIDEO, ".webm")
    );

    private final String bucketName;
    private final String objectPrefix;
    private final Storage cloudStorage;

    @Autowired
    public MediaStorageService(
            @Value("${app.media.gcs-bucket}") String bucketName,
            @Value("${app.media.gcs-prefix:evidence}") String objectPrefix) {
        this(bucketName, objectPrefix, StorageOptions.getDefaultInstance().getService());
    }

    MediaStorageService(String bucketName, String objectPrefix, Storage cloudStorage) {
        this.bucketName = bucketName == null ? "" : bucketName.trim();
        this.objectPrefix = normalizePrefix(objectPrefix);
        this.cloudStorage = cloudStorage;
        if (this.bucketName.isBlank()) {
            throw new IllegalStateException("MEDIA_GCS_BUCKET is required for persistent media storage.");
        }
    }

    public StoredMedia store(MultipartFile file) {
        MediaFormat format = validate(file);
        String storageKey = UUID.randomUUID() + format.extension;
        try {
            BlobInfo info = BlobInfo.newBuilder(BlobId.of(bucketName, objectName(storageKey)))
                    .setContentType(file.getContentType())
                    .setCacheControl("private, max-age=3600")
                    .build();
            cloudStorage.create(info, file.getBytes(), Storage.BlobTargetOption.doesNotExist());
            return new StoredMedia(storageKey, format.type);
        } catch (IOException | RuntimeException ex) {
            throw new MediaStorageException("The media file could not be stored in Cloud Storage.", ex);
        }
    }

    public StoredObject read(String storageKey) {
        validateStorageKey(storageKey);
        Blob blob = cloudStorage.get(BlobId.of(bucketName, objectName(storageKey)));
        if (blob == null || !blob.exists()) throw new ResourceNotFoundException("Media file not found.");
        return new StoredObject(blob.getContent(), contentType(blob.getContentType(), storageKey));
    }

    public void delete(String storageKey) {
        if (storageKey == null || storageKey.isBlank()) return;
        validateStorageKey(storageKey);
        try {
            cloudStorage.delete(BlobId.of(bucketName, objectName(storageKey)));
        } catch (RuntimeException ex) {
            throw new MediaStorageException("The media file could not be deleted from Cloud Storage.", ex);
        }
    }

    private MediaFormat validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Please select a media file to upload.");
        }
        MediaFormat format = ALLOWED_TYPES.get(file.getContentType());
        if (format == null) {
            throw new IllegalArgumentException("Only JPG, PNG, WebP, MP4, and WebM files are supported.");
        }
        long maximumSize = format.type == IssueMediaType.IMAGE ? MAX_IMAGE_SIZE : MAX_VIDEO_SIZE;
        if (file.getSize() > maximumSize) {
            throw new IllegalArgumentException(format.type == IssueMediaType.IMAGE
                    ? "Each image must not exceed 5 MB."
                    : "Video must not exceed 50 MB.");
        }
        return format;
    }

    private String objectName(String storageKey) {
        return objectPrefix.isBlank() ? storageKey : objectPrefix + "/" + storageKey;
    }

    private void validateStorageKey(String storageKey) {
        if (storageKey == null || storageKey.isBlank() || storageKey.contains("/") || storageKey.contains("\\")) {
            throw new IllegalArgumentException("Invalid media storage key.");
        }
    }

    private String contentType(String storedType, String storageKey) {
        if (storedType != null && !storedType.isBlank()) return storedType;
        String lower = storageKey.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".webp")) return "image/webp";
        if (lower.endsWith(".mp4")) return "video/mp4";
        if (lower.endsWith(".webm")) return "video/webm";
        return "image/jpeg";
    }

    private String normalizePrefix(String prefix) {
        if (prefix == null) return "";
        return prefix.trim().replaceAll("^/+|/+$", "");
    }

    private record MediaFormat(IssueMediaType type, String extension) { }
    public record StoredMedia(String storageKey, IssueMediaType type) { }
    public record StoredObject(byte[] bytes, String contentType) { }

    public static class MediaStorageException extends RuntimeException {
        public MediaStorageException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
