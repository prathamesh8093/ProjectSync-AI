package com.projectsync.backend.vcs.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.projectsync.backend.vcs.model.RepositoryMetadata;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.*;

/**
 * Local JSON file implementation of RepositoryMetadataStore.
 * Persists repository metadata in <storagePath>/metadata.json.
 */
@Repository
@ConditionalOnProperty(name = "projectsync.vcs.storage-mode", havingValue = "local", matchIfMissing = true)
public class LocalRepositoryMetadataStore implements RepositoryMetadataStore {

    private final Path metadataFilePath;
    private final ObjectMapper objectMapper;
    private final Object lock = new Object();

    public LocalRepositoryMetadataStore(
            @Value("${projectsync.vcs.storage-path:${user.home}/.projectsync/repositories}") String storagePath) {
        Path basePath = Paths.get(storagePath).toAbsolutePath().normalize();
        this.metadataFilePath = basePath.resolve("metadata.json");
        this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        initStorage();
    }

    private void initStorage() {
        try {
            Path parent = metadataFilePath.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }
            if (!Files.exists(metadataFilePath)) {
                writeEntries(Collections.emptyList());
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize VCS local metadata storage at " + metadataFilePath, e);
        }
    }

    private List<RepositoryMetadata> readEntries() {
        synchronized (lock) {
            File file = metadataFilePath.toFile();
            if (!file.exists() || file.length() == 0) {
                return new ArrayList<>();
            }
            try {
                return objectMapper.readValue(file, new TypeReference<List<RepositoryMetadata>>() {});
            } catch (IOException e) {
                System.err.println("Warning: failed to parse metadata.json, creating new list: " + e.getMessage());
                return new ArrayList<>();
            }
        }
    }

    private void writeEntries(List<RepositoryMetadata> entries) {
        synchronized (lock) {
            try {
                objectMapper.writerWithDefaultPrettyPrinter().writeValue(metadataFilePath.toFile(), entries);
            } catch (IOException e) {
                throw new RuntimeException("Failed to write VCS metadata to " + metadataFilePath, e);
            }
        }
    }

    @Override
    public List<RepositoryMetadata> findAll() {
        return readEntries();
    }

    @Override
    public Optional<RepositoryMetadata> findById(String id) {
        if (id == null) return Optional.empty();
        return readEntries().stream()
                .filter(r -> id.equalsIgnoreCase(r.getId()))
                .findFirst();
    }

    @Override
    public Optional<RepositoryMetadata> findByName(String name) {
        if (name == null) return Optional.empty();
        return readEntries().stream()
                .filter(r -> name.equalsIgnoreCase(r.getName()))
                .findFirst();
    }

    @Override
    public RepositoryMetadata save(RepositoryMetadata metadata) {
        synchronized (lock) {
            List<RepositoryMetadata> list = readEntries();
            Instant now = Instant.now();

            if (metadata.getId() == null || metadata.getId().isBlank()) {
                metadata.setId(UUID.randomUUID().toString());
                metadata.setCreatedAt(now);
                metadata.setUpdatedAt(now);
                list.add(metadata);
            } else {
                boolean found = false;
                for (int i = 0; i < list.size(); i++) {
                    if (list.get(i).getId().equalsIgnoreCase(metadata.getId())) {
                        metadata.setUpdatedAt(now);
                        if (metadata.getCreatedAt() == null) {
                            metadata.setCreatedAt(list.get(i).getCreatedAt());
                        }
                        list.set(i, metadata);
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    if (metadata.getCreatedAt() == null) metadata.setCreatedAt(now);
                    metadata.setUpdatedAt(now);
                    list.add(metadata);
                }
            }

            writeEntries(list);
            return metadata;
        }
    }

    @Override
    public void deleteById(String id) {
        synchronized (lock) {
            List<RepositoryMetadata> list = readEntries();
            boolean removed = list.removeIf(r -> r.getId().equalsIgnoreCase(id));
            if (removed) {
                writeEntries(list);
            }
        }
    }

    @Override
    public boolean existsByName(String name) {
        if (name == null) return false;
        return readEntries().stream().anyMatch(r -> name.equalsIgnoreCase(r.getName()));
    }
}
