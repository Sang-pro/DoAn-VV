package com.trsang.doan2.services.implementation;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.trsang.doan2.dtos.ollama.OllamaApiResponses;
import com.trsang.doan2.dtos.ollama.OllamaModelDetails;
import com.trsang.doan2.dtos.ollama.OllamaModelResponse;
import com.trsang.doan2.services.interfaces.IOllamaModelService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class OllamaModelService implements IOllamaModelService {
    
    private final WebClient ollamaWebClient;
    private static final int DEFAULT_TIMEOUT_SECONDS = 30;

    @Override
    public List<OllamaModelResponse> listLocalModels() {
        try {
            OllamaApiResponses response = ollamaWebClient.get()
                                .uri("/tags")
                                .accept(MediaType.APPLICATION_JSON)
                                .retrieve()
                                .bodyToMono(OllamaApiResponses.class)
                                .block(Duration.ofSeconds(DEFAULT_TIMEOUT_SECONDS));
            
            if (response != null && response.getModels() != null) {
                return response.getModels();
            }
        } catch (Exception e) {
            log.error("Failed to list local models: {}", e != null ? e.toString() : "Unknown error");
        }
        return Collections.emptyList();
    }

    @Override
    public List<OllamaModelResponse> listRunningModels() {
        try {
            OllamaApiResponses response = ollamaWebClient.get()
                    .uri("/ps")
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono(OllamaApiResponses.class)
                    .block(Duration.ofSeconds(DEFAULT_TIMEOUT_SECONDS));

            if (response != null && response.getModels() != null) {
                return response.getModels();
            }
        } catch (Exception e) {
            log.error("Failed to list running models: {}", e != null ? e.toString() : "Unknown error");
        }
        return Collections.emptyList();
    }

    @Override
    public OllamaModelDetails getModelDetails(String model) {
        try {
            OllamaApiResponses response = ollamaWebClient.post()
                    .uri("/show")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(Map.of("model", model))
                    .retrieve()
                    .bodyToMono(OllamaApiResponses.class)
                    .block(Duration.ofSeconds(DEFAULT_TIMEOUT_SECONDS));

            return response != null ? response.getDetails() : null;
        } catch (WebClientResponseException.NotFound e) {
            log.warn("Model {} not found", model);
            return null;
        } catch (Exception e) {
            log.error("Failed to get model details for {}: {}", model, e != null ? e.toString() : "Unknown error");
            return null;
        }
    }

    @Override
    public boolean copyModel(String source, String destination) {
        try {
            return ollamaWebClient.post()
                    .uri("/copy")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(Map.of(
                            "source", source,
                            "destination", destination))
                    .retrieve()
                    .toBodilessEntity()
                    .map(response -> response.getStatusCode().is2xxSuccessful())
                    .block(Duration.ofSeconds(DEFAULT_TIMEOUT_SECONDS));
        } catch (Exception e) {
            log.error("Failed to copy model from {} to {}: {}", 
                source, destination, e != null ? e.toString() : "Unknown error");
            return false;
        }
    }

    @Override
    public boolean deleteModel(String model) {
        try {
            return ollamaWebClient.method(org.springframework.http.HttpMethod.DELETE)
                    .uri("/delete")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(Map.of("model", model))
                    .retrieve()
                    .toBodilessEntity()
                    .map(response -> response.getStatusCode().is2xxSuccessful())
                    .block(Duration.ofSeconds(DEFAULT_TIMEOUT_SECONDS));
        } catch (WebClientResponseException.NotFound e) {
            log.warn("Model {} not found for deletion", model);
            return false;
        } catch (Exception e) {
            log.error("Failed to delete model {}: {}", model, e != null ? e.toString() : "Unknown error");
            return false;
        }
    }

    @Override
    public String pullModel(String model, boolean insecure, boolean stream) {
        Map<String, Object> requestBody;
        if (insecure) {
            requestBody = Map.of(
                    "model", model,
                    "insecure", true,
                    "stream", stream);
        } else {
            requestBody = Map.of(
                    "model", model,
                    "stream", stream);
        }

        try {
            OllamaApiResponses response = ollamaWebClient.post()
                    .uri("/pull")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(OllamaApiResponses.class)
                    .block(Duration.ofSeconds(120)); // Longer timeout for model pulling

            if (response != null && response.getStatus() != null) {
                return response.getStatus();
            }
            return "success";
        } catch (Exception e) {
            log.error("Failed to pull model {}: {}", model, e != null ? e.toString() : "Unknown error");
            return "error: " + (e != null ? e.toString() : "Unknown error");
        }
    }

    @Override
    public String pushModel(String model, boolean insecure, boolean stream) {
        Map<String, Object> requestBody;
        if (insecure) {
            requestBody = Map.of(
                    "model", model,
                    "insecure", true,
                    "stream", stream);
        } else {
            requestBody = Map.of(
                    "model", model,
                    "stream", stream);
        }

        try {
            OllamaApiResponses response = ollamaWebClient.post()
                    .uri("/push")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(OllamaApiResponses.class)
                    .block(Duration.ofSeconds(120)); // Longer timeout for model pushing

            if (response != null && response.getStatus() != null) {
                return response.getStatus();
            }
            return "success";
        } catch (Exception e) {
            log.error("Failed to push model {}: {}", model, e != null ? e.toString() : "Unknown error");
            return "error: " + (e != null ? e.toString() : "Unknown error");
        }
    }

}
