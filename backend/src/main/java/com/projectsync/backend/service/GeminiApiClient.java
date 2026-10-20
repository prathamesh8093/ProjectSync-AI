package com.projectsync.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Service
public class GeminiApiClient {

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.url:https://generativelanguage.googleapis.com/v1beta/models/gemini-flash-latest:generateContent}")
    private String apiUrl;

    private final RestClient restClient;

    public GeminiApiClient() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(5));
        requestFactory.setReadTimeout(Duration.ofSeconds(12));
        this.restClient = RestClient.builder()
                .requestFactory(requestFactory)
                .build();
    }

    /** Sends the prompt and returns the model's text. Asks Gemini for JSON output. */
    public String generateJson(String prompt) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("GEMINI_API_KEY is not configured.");
        }

        Map<String, Object> body = Map.of(
                "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))),
                "generationConfig", Map.of("responseMimeType", "application/json"));

        try {
            return executeCall(apiUrl, body);
        } catch (Exception e) {
            String fallbackUrl = apiUrl.contains("gemini-flash-latest")
                    ? "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent"
                    : "https://generativelanguage.googleapis.com/v1beta/models/gemini-flash-latest:generateContent";
            try {
                return executeCall(fallbackUrl, body);
            } catch (Exception fallbackEx) {
                throw new IllegalStateException("Gemini API call failed on primary (" + e.getMessage() + ") and fallback (" + fallbackEx.getMessage() + ")", e);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private String executeCall(String url, Map<String, Object> body) {
        Map<String, Object> response = restClient.post()
                .uri(url)
                .header("x-goog-api-key", apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(Map.class);

        try {
            var candidate = ((List<Map<String, Object>>) response.get("candidates")).get(0);
            var content = (Map<String, Object>) candidate.get("content");
            var part = ((List<Map<String, Object>>) content.get("parts")).get(0);
            String text = (String) part.get("text");
            if (text != null) {
                text = text.trim();
                if (text.startsWith("```")) {
                    text = text.replaceAll("^```[a-zA-Z]*\\r?\\n?", "").replaceAll("\\r?\\n?```$", "").trim();
                }
            }
            return text;
        } catch (RuntimeException e) {
            throw new IllegalStateException("Unexpected Gemini response from " + url, e);
        }
    }
}

