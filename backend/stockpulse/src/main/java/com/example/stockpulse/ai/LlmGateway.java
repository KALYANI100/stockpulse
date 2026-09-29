package com.example.stockpulse.ai;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Component
public class LlmGateway {
    private final RestClient client;
    private final String baseUrl;
    private final String path;
    private final String apiKey;
    private final String model;
    private final String productHeader;

    public LlmGateway(@Value("${llm.base-url}") String baseUrl,
                      @Value("${llm.chat-completions-path}") String path,
                      @Value("${llm.api-key:}") String apiKey,
                      @Value("${llm.model}") String model,
                      @Value("${llm.product-header:}") String productHeader) {
        var requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(4));
        requestFactory.setReadTimeout(Duration.ofSeconds(15));
        this.client = RestClient.builder().requestFactory(requestFactory).build();
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        this.path = path.startsWith("/") ? path : "/" + path;
        this.apiKey = apiKey;
        this.model = model;
        this.productHeader = productHeader;
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
}
