package com.librarymanagement.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
public class FileStorageService {
    private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);
    private static final List<String> ALLOWED_CONTENT_TYPES = Arrays.asList(
            "image/jpeg", "image/jpg", "image/png", "image/webp", "image/gif"
    );
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB

    private final AppProperties properties;

    public FileStorageService(AppProperties properties) {
        this.properties = properties;
        init();
    }

    private void init() {
        try {
            Path uploadPath = Paths.get(properties.getUploadDir(), properties.getCoverImageDir());
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
                log.info("Created upload directory: {}", uploadPath.toAbsolutePath());
            }
        } catch (IOException e) {
            log.error("Could not create upload directory", e);
        }
    }

    public String storeCoverImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }

        // Validate file size
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("Image file too large. Maximum 5MB allowed.");
        }

        // Validate content type
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException("Invalid image type. Allowed: JPEG, PNG, WebP, GIF");
        }

        // Generate unique filename
        String originalFilename = file.getOriginalFilename();
        String extension = getExtension(originalFilename);
        if (extension.isEmpty()) {
            extension = getExtensionFromContentType(contentType);
        }
        String filename = UUID.randomUUID().toString() + extension;

        try {
            Path uploadPath = Paths.get(properties.getUploadDir(), properties.getCoverImageDir());
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            Path filePath = uploadPath.resolve(filename);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            log.info("Stored cover image: {} ({} bytes)", filename, file.getSize());
            return filename;
        } catch (IOException e) {
            log.error("Failed to store cover image", e);
            throw new RuntimeException("Failed to store cover image: " + e.getMessage(), e);
        }
    }

    public void deleteCoverImage(String filename) {
        if (filename == null || filename.isBlank()) {
            return;
        }
        try {
            Path filePath = Paths.get(properties.getUploadDir(), properties.getCoverImageDir(), filename);
            if (Files.exists(filePath)) {
                Files.delete(filePath);
                log.info("Deleted cover image: {}", filename);
            }
        } catch (IOException e) {
            log.warn("Could not delete cover image: {}", filename, e);
        }
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".")).toLowerCase();
    }

    private String getExtensionFromContentType(String contentType) {
        return switch (contentType.toLowerCase()) {
            case "image/jpeg", "image/jpg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            case "image/gif" -> ".gif";
            default -> ".jpg";
        };
    }
}
