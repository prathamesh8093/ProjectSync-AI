package com.projectsync.backend.vcs.repository;

import com.projectsync.backend.vcs.model.RepositoryMetadata;

import java.util.List;
import java.util.Optional;

/**
 * Storage abstraction for repository metadata.
 * Allows switching between local JSON/file storage and Supabase PostgreSQL.
 */
public interface RepositoryMetadataStore {

    List<RepositoryMetadata> findAll();

    Optional<RepositoryMetadata> findById(String id);

    Optional<RepositoryMetadata> findByName(String name);

    RepositoryMetadata save(RepositoryMetadata metadata);

    void deleteById(String id);

    boolean existsByName(String name);
}
