package com.projectsync.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

@Service
public class SupabaseStorageService {

    private final RestClient restClient;
    private final String storageUrl;
    private final String bucket;
    private final String serviceRoleKey;

    public SupabaseStorageService(
            @Value("${supabase.storage.url}") String storageUrl,
            @Value("${supabase.storage.bucket}") String bucket,
            @Value("${supabase.storage.service-role-key}") String serviceRoleKey) {

        this.storageUrl = Objects.requireNonNull(storageUrl).replaceAll("/+$", "");
        this.bucket = Objects.requireNonNull(bucket);
        this.serviceRoleKey = Objects.requireNonNull(serviceRoleKey);

        this.restClient = RestClient.builder()
                .baseUrl(this.storageUrl + "/storage/v1")
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + this.serviceRoleKey)
                .defaultHeader("apikey", this.serviceRoleKey)
                .build();
    }

    public void upload(
            String storagePath,
            byte[] content,
            String contentType) {

        if (storagePath == null || storagePath.isBlank()) {
            throw new IllegalArgumentException("Storage path is required");
        }

        if (content == null) {
            throw new IllegalArgumentException("File content is required");
        }

        MediaType mediaType = resolveMediaType(contentType);

        try {
            restClient.post()
                    .uri("/object/{bucket}/{path}", bucket, storagePath)
                    .contentType(mediaType)
                    .header("x-upsert", "false")
                    .body(content)
                    .retrieve()
                    .toBodilessEntity();

        } catch (RestClientException exception) {
            throw new IllegalStateException(
                    "Failed to upload file to Supabase Storage",
                    exception
            );
        }
    }

    public byte[] download(String storagePath) {

        if (storagePath == null || storagePath.isBlank()) {
            throw new IllegalArgumentException("Storage path is required");
        }

        try {
            ResponseEntity<byte[]> response = restClient.get()
                    .uri("/object/{bucket}/{path}", bucket, storagePath)
                    .retrieve()
                    .toEntity(byte[].class);

            return response.getBody() == null
                    ? new byte[0]
                    : response.getBody();

        } catch (RestClientException exception) {
            throw new IllegalStateException(
                    "Failed to download file from Supabase Storage",
                    exception
            );
        }
    }

    public void delete(String storagePath) {

        if (storagePath == null || storagePath.isBlank()) {
            throw new IllegalArgumentException("Storage path is required");
        }

        try {
            restClient.delete()
                    .uri("/object/{bucket}/{path}", bucket, storagePath)
                    .retrieve()
                    .toBodilessEntity();

        } catch (RestClientException exception) {
            throw new IllegalStateException(
                    "Failed to delete file from Supabase Storage",
                    exception
            );
        }
    }

    private MediaType resolveMediaType(String contentType) {

        if (contentType == null || contentType.isBlank()) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }

        try {
            return MediaType.parseMediaType(contentType);
        } catch (IllegalArgumentException exception) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }
}