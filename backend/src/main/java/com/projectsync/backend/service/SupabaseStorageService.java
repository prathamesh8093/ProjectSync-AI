package com.projectsync.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriUtils;

import java.net.URI;
import java.nio.charset.StandardCharsets;

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

        this.restClient = RestClient.builder().build();
        this.storageUrl = storageUrl;
        this.bucket = bucket;
        this.serviceRoleKey = serviceRoleKey;
    }

    public String getBucket() {
        return bucket;
    }

    public String getStorageUrl() {
        return storageUrl;
    }

    public boolean isConfigured() {
        return serviceRoleKey != null
                && !serviceRoleKey.isBlank();
    }

    public void upload(
            String storagePath,
            byte[] fileBytes,
            String contentType) {

        validateConfiguration();

        if (fileBytes == null) {
            throw new IllegalArgumentException(
                    "File content is required");
        }

        URI uri = buildObjectUri(storagePath);

        MediaType mediaType = resolveContentType(contentType);

        restClient
                .post()
                .uri(uri)
                .header(
                        HttpHeaders.AUTHORIZATION,
                        "Bearer " + serviceRoleKey
                )
                .header(
                        "apikey",
                        serviceRoleKey
                )
                .header(
                        "x-upsert",
                        "false"
                )
                .contentType(mediaType)
                .body(fileBytes)
                .retrieve()
                .toBodilessEntity();
    }

    public byte[] download(
            String storagePath) {

        validateConfiguration();

        URI uri = buildObjectUri(storagePath);

        byte[] response = restClient
                .get()
                .uri(uri)
                .header(
                        HttpHeaders.AUTHORIZATION,
                        "Bearer " + serviceRoleKey
                )
                .header(
                        "apikey",
                        serviceRoleKey
                )
                .retrieve()
                .body(byte[].class);

        if (response == null) {
            throw new IllegalStateException(
                    "Supabase Storage returned an empty response");
        }

        return response;
    }

    public void delete(
            String storagePath) {

        validateConfiguration();

        URI uri = buildObjectUri(storagePath);

        restClient
                .delete()
                .uri(uri)
                .header(
                        HttpHeaders.AUTHORIZATION,
                        "Bearer " + serviceRoleKey
                )
                .header(
                        "apikey",
                        serviceRoleKey
                )
                .retrieve()
                .toBodilessEntity();
    }

    private URI buildObjectUri(
            String storagePath) {

        if (storagePath == null || storagePath.isBlank()) {
            throw new IllegalArgumentException(
                    "Storage path is required");
        }

        String encodedPath =
                UriUtils.encodePath(
                        storagePath,
                        StandardCharsets.UTF_8
                );

        String encodedBucket =
                UriUtils.encodePathSegment(
                        bucket,
                        StandardCharsets.UTF_8
                );

        return URI.create(
                storageUrl
                        + "/storage/v1/object/"
                        + encodedBucket
                        + "/"
                        + encodedPath
        );
    }

    private MediaType resolveContentType(
            String contentType) {

        if (contentType == null
                || contentType.isBlank()) {

            return MediaType.APPLICATION_OCTET_STREAM;
        }

        try {
            return MediaType.parseMediaType(
                    contentType.trim()
            );
        } catch (IllegalArgumentException exception) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }

    private void validateConfiguration() {

        if (serviceRoleKey == null
                || serviceRoleKey.isBlank()) {

            throw new IllegalStateException(
                    "Supabase Storage service role key is not configured");
        }

        if (storageUrl == null
                || storageUrl.isBlank()) {

            throw new IllegalStateException(
                    "Supabase Storage URL is not configured");
        }

        if (bucket == null
                || bucket.isBlank()) {

            throw new IllegalStateException(
                    "Supabase Storage bucket is not configured");
        }
    }
}