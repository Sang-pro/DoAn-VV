package com.trsang.doan2.services.implementation;

import com.trsang.doan2.config.OllamaConfig;
import com.trsang.doan2.dtos.ollama.OllamaCompletionRequest;
import com.trsang.doan2.dtos.ollama.OllamaCompletionResponse;
import com.trsang.doan2.services.interfaces.IOllamaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OllamaService implements IOllamaService {

    private final WebClient ollamaWebClient;
    private final OllamaConfig ollamaConfig;
    
    @Override
    public OllamaCompletionResponse generateCompletion(String model, List<Map<String, String>> messages, Map<String, Object> options) {
        log.info("Generating completion for model: {}", model);
        
        // Use default model if not provided
        final String finalModel = model == null || model.isEmpty() ? ollamaConfig.getDefaultModel() : model;
        if (!finalModel.equals(model)) {
            log.info("Using default model: {}", finalModel);
        }
        
        // Convert messages to the format expected by Ollama
        List<OllamaCompletionRequest.OllamaMessage> ollamaMessages = messages.stream()
                .map(msg -> OllamaCompletionRequest.OllamaMessage.builder()
                        .role(msg.get("role"))
                        .content(msg.get("content"))
                        .build())
                .collect(Collectors.toList());
        
        // Prepare options with defaults if not provided
        Map<String, Object> requestOptions = new HashMap<>();
        if (options == null) {
            requestOptions.put("temperature", ollamaConfig.getDefaultTemperature());
            requestOptions.put("repeat_penalty", ollamaConfig.getDefaultRepeatPenalty());
            requestOptions.put("numa", ollamaConfig.isDefaultNuma());
        } else {
            requestOptions = options;
        }
                
        // Build request
        OllamaCompletionRequest request = OllamaCompletionRequest.builder()
                .model(finalModel)
                .messages(ollamaMessages)
                .stream(false) // Set to false for non-streaming response
                .options(requestOptions)
                .build();
                
        try {
            // Log the request body for debugging
            log.debug("Sending request to Ollama API: {}", request);
            
            // Make API call to Ollama with improved error handling and timeout
            return ollamaWebClient.post()
                    .uri("/chat")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, 
                            response -> response.bodyToMono(String.class)
                                    .flatMap(error -> {
                                        log.error("Ollama API error: Status {}, Body {}", response.statusCode(), error);
                                        return Mono.error(new RuntimeException("Ollama API error: " + error));
                                    }))
                    .bodyToMono(OllamaCompletionResponse.class)
                    .timeout(Duration.ofSeconds(ollamaConfig.getTimeoutSeconds()))
                    .doOnError(WebClientResponseException.class, e -> 
                            log.error("Ollama API error: Status {}, Body {}", e.getStatusCode(), e.getResponseBodyAsString()))
                    .doOnError(e -> log.error("Error calling Ollama API", e))
                    .onErrorResume(e -> {
                        // Create fallback response
                        OllamaCompletionResponse.OllamaMessage errorMessage = new OllamaCompletionResponse.OllamaMessage(
                            "assistant", 
                            "I'm sorry, I encountered an error while processing your request. Please try again later."
                        );
                        
                        OllamaCompletionResponse fallback = OllamaCompletionResponse.builder()
                                .model(finalModel)
                                .message(errorMessage)
                                .done(true)
                                .build();
                        
                        return Mono.just(fallback);
                    })
                    .block();
        } catch (Exception e) {
            log.error("Unexpected error calling Ollama API: ", e);
            
            // Create a fallback response in case of error
            OllamaCompletionResponse.OllamaMessage errorMessage = new OllamaCompletionResponse.OllamaMessage(
                "assistant", 
                "I'm sorry, I encountered an error while processing your request. Please try again later."
            );
            
            return OllamaCompletionResponse.builder()
                    .model(finalModel)
                    .message(errorMessage)
                    .done(true)
                    .build();
        }
    }
    
    @Override
    public Flux<ServerSentEvent<Object>> streamCompletion(
            String model, 
            List<Map<String, String>> messages, 
            boolean streaming,
            Map<String, Object> options
    ) {
        log.info("Streaming completion for model: {}", model);
        
        // Use default model if not provided
        final String finalModel = model == null || model.isEmpty() ? ollamaConfig.getDefaultModel() : model;
        if (finalModel != model) {
            log.info("Using default model: {}", finalModel);
        }
        
        // Convert messages to the format expected by Ollama
        List<OllamaCompletionRequest.OllamaMessage> ollamaMessages = messages.stream()
                .map(msg -> OllamaCompletionRequest.OllamaMessage.builder()
                        .role(msg.get("role"))
                        .content(msg.get("content"))
                        .build())
                .collect(Collectors.toList());
        
        // Prepare options with defaults if not provided
        Map<String, Object> requestOptions = new HashMap<>();
        if (options == null) {
            requestOptions.put("temperature", ollamaConfig.getDefaultTemperature());
            requestOptions.put("repeat_penalty", ollamaConfig.getDefaultRepeatPenalty());
            requestOptions.put("numa", ollamaConfig.isDefaultNuma());
        } else {
            requestOptions = options;
        }
                
        // Build request with streaming enabled
        OllamaCompletionRequest request = OllamaCompletionRequest.builder()
                .model(finalModel)
                .messages(ollamaMessages)
                .stream(streaming)
                .options(requestOptions)
                .build();
        
        log.debug("Sending streaming request to Ollama API: {}", request);
        
        // Generate a unique event ID for this streaming session
        final String eventId = UUID.randomUUID().toString();
        
        return ollamaWebClient.post()
                .uri("/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToFlux(String.class) // Change to String to handle raw JSON
                .doOnNext(chunk -> log.debug("Raw response chunk: {}", chunk))
                .map(rawJson -> {
                    // Pass the raw JSON directly to the client
                    return ServerSentEvent.<Object>builder()
                            .id(eventId)
                            .event("message")
                            .data(rawJson)
                            .build();
                })
                .onErrorResume(e -> {
                    log.error("Error in streaming response: {}", e.getMessage());
                    
                    // Create an error event
                    Map<String, Object> errorMap = new HashMap<>();
                    errorMap.put("model", finalModel);
                    errorMap.put("error", e.getMessage());
                    errorMap.put("done", true);
                    
                    OllamaCompletionResponse.OllamaMessage errorMessage = new OllamaCompletionResponse.OllamaMessage(
                        "assistant", 
                        "I'm sorry, I encountered an error while processing your request. Please try again later."
                    );
                    
                    errorMap.put("message", errorMessage);
                    
                    return Flux.just(ServerSentEvent.<Object>builder()
                            .id(eventId)
                            .event("error")
                            .data(errorMap)
                            .build());
                })
                .doOnComplete(() -> log.debug("Streaming completed for event ID: {}", eventId));
    }
}
