package com.resolveit.attachment;

import com.resolveit.config.AppProperties;
import com.resolveit.exception.ForbiddenException;
import com.resolveit.exception.ResourceNotFoundException;
import jakarta.annotation.PostConstruct;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

@Service
public class LocalStorageService implements StorageService {

    private final Path rootLocation;

    public LocalStorageService(AppProperties appProperties) {
        this.rootLocation = Paths.get(appProperties.fileStorage().path()).toAbsolutePath().normalize();
    }

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(rootLocation);
        } catch (IOException e) {
            throw new IllegalStateException("Could not initialize file storage directory", e);
        }
    }

    @Override
    public String store(InputStream inputStream, String extension) {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        String relativeFolder = String.format("%04d/%02d", today.getYear(), today.getMonthValue());
        String filename = UUID.randomUUID() + "." + extension;
        String storageKey = relativeFolder + "/" + filename;

        try {
            Path target = resolveSafe(storageKey);
            Files.createDirectories(target.getParent());
            Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
            return storageKey;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to store attachment", e);
        }
    }

    @Override
    public Resource loadAsResource(String storageKey) {
        try {
            Path file = resolveSafe(storageKey);
            Resource resource = new UrlResource(file.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            }
            throw new ResourceNotFoundException("Attachment file not found");
        } catch (MalformedURLException e) {
            throw new ResourceNotFoundException("Attachment file not found");
        }
    }

    private Path resolveSafe(String storageKey) {
        if (storageKey == null || storageKey.isBlank() || storageKey.contains("..")) {
            throw new ForbiddenException("Invalid storage key");
        }
        Path resolved = rootLocation.resolve(storageKey).normalize();
        if (!resolved.startsWith(rootLocation)) {
            throw new ForbiddenException("Path traversal detected");
        }
        return resolved;
    }
}

