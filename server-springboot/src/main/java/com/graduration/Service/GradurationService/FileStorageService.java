package com.graduration.Service.GradurationService;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.graduration.exception.AppException;
import com.graduration.exception.ErrorCode;

@Service
public class FileStorageService {
    private final Path root;

    public FileStorageService(@Value("${app.storage.root:./storage}") String root) {
        this.root = Path.of(root).toAbsolutePath().normalize();
    }

    public StoredFile store(MultipartFile file, Long defensePeriodId, Long teamId, Long milestoneId, String extension) {
        try {
            Path directory = root.resolve("defense-periods")
                    .resolve(String.valueOf(defensePeriodId))
                    .resolve("teams")
                    .resolve(String.valueOf(teamId))
                    .resolve("milestones")
                    .resolve(String.valueOf(milestoneId))
                    .normalize();
            requireInsideRoot(directory);
            Files.createDirectories(directory);
            String storedName = UUID.randomUUID() + (extension.isBlank() ? "" : "." + extension);
            Path destination = directory.resolve(storedName).normalize();
            requireInsideRoot(destination);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream input = file.getInputStream();
                    DigestInputStream source = new DigestInputStream(input, digest)) {
                Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
            }
            String relativePath = root.relativize(destination).toString();
            return new StoredFile(relativePath, storedName, HexFormat.of().formatHex(digest.digest()));
        } catch (IOException | NoSuchAlgorithmException exception) {
            throw new AppException(ErrorCode.FILE_STORAGE_ERROR);
        }
    }

    public Resource load(String relativePath) {
        try {
            Path file = root.resolve(relativePath).normalize();
            requireInsideRoot(file);
            Resource resource = new UrlResource(file.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new AppException(ErrorCode.FILE_STORAGE_ERROR);
            }
            return resource;
        } catch (IOException exception) {
            throw new AppException(ErrorCode.FILE_STORAGE_ERROR);
        }
    }

    public void deleteQuietly(String relativePath) {
        if (relativePath == null) {
            return;
        }
        try {
            Path file = root.resolve(relativePath).normalize();
            requireInsideRoot(file);
            Files.deleteIfExists(file);
        } catch (IOException | AppException ignored) {
            // Best-effort cleanup when a database transaction fails.
        }
    }

    private void requireInsideRoot(Path path) {
        if (!path.startsWith(root)) {
            throw new AppException(ErrorCode.FILE_STORAGE_ERROR);
        }
    }

    public record StoredFile(String relativePath, String storedName, String checksum) {}
}
