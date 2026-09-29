package com.example.stockpulse.ai;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Component
public class LlmGateway {
    private final RestClient client;
    private final String baseUrl;
    private final String path;
    private final String apiKey;
    private final String model;
    private final String productHeader;
    private final HttpClient streamingClient;
    private final ObjectMapper objectMapper;

    public LlmGateway(@Value("${llm.base-url}") String baseUrl,
                      @Value("${llm.chat-completions-path}") String path,
                      @Value("${llm.api-key:}") String apiKey,
                      @Value("${llm.model}") String model,
                      @Value("${llm.product-header:}") String productHeader,
                      ObjectMapper objectMapper) {
        var requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(4));
        requestFactory.setReadTimeout(Duration.ofSeconds(15));
        this.client = RestClient.builder().requestFactory(requestFactory).build();
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        this.path = path.startsWith("/") ? path : "/" + path;
        this.apiKey = apiKey;
        this.model = model;
        this.productHeader = productHeader;
        this.streamingClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(4))
            .build();
        this.objectMapper = objectMapper;
    }

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    public String complete(String prompt) {
        if (!isConfigured()) {
            throw new IllegalStateException("LLM_API_KEY is not configured");
        }

        var request = client.post().uri(URI.create(baseUrl + path))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json");
        if (productHeader != null && !productHeader.isBlank()) {
            request.header("product", productHeader);
        }

        Map<?, ?> response = request.body(Map.of(
                        "model", model,
                        "messages", List.of(Map.of("role", "user", "content", prompt))))
                .retrieve()
                .body(Map.class);
        if (response == null || !(response.get("choices") instanceof List<?> choices)
                || choices.isEmpty() || !(choices.get(0) instanceof Map<?, ?> choice)
                || !(choice.get("message") instanceof Map<?, ?> message)
                || !(message.get("content") instanceof String content)
                || content.isBlank()) {
            throw new IllegalStateException("LLM response did not contain message content");
        }
        return content;
    }

    public void streamCompletion(String prompt, Consumer<String> onToken) {
        if (!isConfigured()) {
            throw new IllegalStateException("LLM_API_KEY is not configured");
        }

        try {
            String requestBody = objectMapper.writeValueAsString(Map.of(
                    "model", model,
                    "messages", List.of(Map.of("role", "user", "content", prompt)),
                    "stream", true));
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(URI.create(baseUrl + path))
                    .timeout(Duration.ofMinutes(2))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .header("Accept", "text/event-stream")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody));
            if (productHeader != null && !productHeader.isBlank()) {
                requestBuilder.header("product", productHeader);
            }

            HttpResponse<java.util.stream.Stream<String>> response = streamingClient.send(
                    requestBuilder.build(), HttpResponse.BodyHandlers.ofLines());
            try (var lines = response.body()) {
                if (response.statusCode() < 200 || response.statusCode() >= 300) {
                    throw new IllegalStateException("LLM gateway returned HTTP " + response.statusCode());
                }
                var iterator = lines.iterator();
                while (iterator.hasNext()) {
                    String line = iterator.next();
                    if (!line.startsWith("data:")) {
                        continue;
                    }
                    String data = line.substring("data:".length()).trim();
                    if ("[DONE]".equals(data)) {
                        break;
                    }
                    emitContentToken(data, onToken);
                }
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("LLM streaming request was interrupted", exception);
        } catch (IOException exception) {
            throw new IllegalStateException("LLM streaming request failed", exception);
        }
    }

    private void emitContentToken(String data, Consumer<String> onToken) {
        try {
            Map<?, ?> chunk = objectMapper.readValue(data, Map.class);
            if (!(chunk.get("choices") instanceof List<?> choices) || choices.isEmpty()
                    || !(choices.get(0) instanceof Map<?, ?> choice)
                    || !(choice.get("delta") instanceof Map<?, ?> delta)
                    || !(delta.get("content") instanceof String content)
                    || content.isEmpty()) {
                return;
            }
            onToken.accept(content);
        } catch (RuntimeException exception) {
            throw new IllegalStateException("LLM gateway sent an invalid streaming event", exception);
        }
    }
}
