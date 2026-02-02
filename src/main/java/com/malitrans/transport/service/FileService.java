package com.malitrans.transport.service;

import com.malitrans.transport.config.FileUploadConfig;
import com.malitrans.transport.exception.FileUploadException;
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
public class FileService {

    private static final Logger logger = LoggerFactory.getLogger(FileService.class);
    
    // Maximum file size: 10MB
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB in bytes
    
    // Allowed MIME types
    private static final List<String> ALLOWED_MIME_TYPES = Arrays.asList(
        "image/jpeg",
        "image/jpg",
        "image/png",
        "application/pdf"
    );
    
    // Allowed file extensions
    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList(
        "jpg", "jpeg", "png", "pdf"
    );

    private final FileUploadConfig fileUploadConfig;

    public FileService(FileUploadConfig fileUploadConfig) {
        this.fileUploadConfig = fileUploadConfig;
        initializeUploadDirectory();
    }

    /**
     * Initialize upload directory if it doesn't exist
     */
    private void initializeUploadDirectory() {
        try {
            Path uploadDir = Paths.get(fileUploadConfig.getDir());
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
                logger.info("Created upload directory: {}", uploadDir.toAbsolutePath());
            }
        } catch (IOException e) {
            logger.error("Failed to create upload directory: {}", fileUploadConfig.getDir(), e);
            throw new FileUploadException("Failed to initialize upload directory", e);
        }
    }

    /**
     * Upload a file and return its public URL
     * @param file The multipart file to upload
     * @return Public URL of the uploaded file
     */
    public String uploadFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new FileUploadException("File is empty or null");
        }

        // Validate file size
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new FileUploadException(
                String.format("File size exceeds maximum allowed size of %d MB", MAX_FILE_SIZE / (1024 * 1024))
            );
        }

        // Validate MIME type
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType.toLowerCase())) {
            throw new FileUploadException(
                String.format("File type not allowed. Allowed types: %s", String.join(", ", ALLOWED_MIME_TYPES))
            );
        }

        // Get original filename and extract extension
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.trim().isEmpty()) {
            throw new FileUploadException("Original filename is empty");
        }

        // Sanitize filename and extract extension
        String extension = getFileExtension(originalFilename);
        if (extension == null || !ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
            throw new FileUploadException(
                String.format("File extension not allowed. Allowed extensions: %s", String.join(", ", ALLOWED_EXTENSIONS))
            );
        }

        // Generate UUID filename while preserving extension
        String uuidFilename = UUID.randomUUID().toString() + "." + extension.toLowerCase();

        try {
            // Save file to upload directory
            Path targetPath = Paths.get(fileUploadConfig.getDir(), uuidFilename);
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            
            logger.info("File uploaded successfully: {} (original: {})", uuidFilename, originalFilename);
            
            // Return public URL
            return fileUploadConfig.getBaseUrl() + "/" + uuidFilename;
        } catch (IOException e) {
            logger.error("Failed to save file: {}", originalFilename, e);
            throw new FileUploadException("Failed to save file", e);
        }
    }

    /**
     * Get file extension from filename
     */
    private String getFileExtension(String filename) {
        if (filename == null || filename.isEmpty()) {
            return null;
        }
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == filename.length() - 1) {
            return null;
        }
        return filename.substring(lastDotIndex + 1);
    }

    /**
     * Serve a file by filename
     * @param filename The UUID filename
     * @return Path to the file
     * @throws FileUploadException if file not found or invalid filename
     */
    public Path getFile(String filename) {
        // Security: Validate filename to prevent path traversal
        if (filename == null || filename.trim().isEmpty()) {
            throw new FileUploadException("Filename is empty");
        }

        // Sanitize filename - only allow alphanumeric, dots, hyphens, underscores
        if (!filename.matches("^[a-zA-Z0-9._-]+$")) {
            throw new FileUploadException("Invalid filename format");
        }

        // Prevent path traversal
        if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            throw new FileUploadException("Invalid filename: path traversal detected");
        }

        Path filePath = Paths.get(fileUploadConfig.getDir(), filename);
        
        if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
            throw new FileUploadException("File not found: " + filename);
        }

        return filePath;
    }

    /**
     * Get content type for a file based on its extension
     */
    public String getContentType(String filename) {
        String extension = getFileExtension(filename);
        if (extension == null) {
            return "application/octet-stream";
        }
        
        return switch (extension.toLowerCase()) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "pdf" -> "application/pdf";
            default -> "application/octet-stream";
        };
    }
}

