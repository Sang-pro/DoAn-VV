package com.trsang.doan2.controllers;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.trsang.doan2.dtos.ollama.OllamaCompletionResponse;
import com.trsang.doan2.services.interfaces.IOllamaService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

@Slf4j
@RestController
@RequestMapping("/api/ollama/chat")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", allowCredentials = "false")
@Tag(name = "Ollama Chat", description = "Direct API for Ollama Chat completions")
public class OllamaChatController {

    private final IOllamaService ollamaService;

    @Data
    public static class ChatRequest {
        private String model;
        private List<Map<String, String>> messages;
        private Map<String, Object> options;
        private Boolean streaming = false;
    }

    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Generate chat completion",
            description = "Generate a standard (non-streaming) chat completion from Ollama",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Completion generated successfully",
                            content = @Content(schema = @Schema(implementation = OllamaCompletionResponse.class))
                    )
            }
    )
    public ResponseEntity<OllamaCompletionResponse> generateCompletion(@RequestBody ChatRequest request) {
        try {
            OllamaCompletionResponse response = ollamaService.generateCompletion(
                request.getModel(), 
                request.getMessages(),
                request.getOptions()
            );
            return ResponseEntity.ok(response);
        } 
        catch (Exception e) {
            log.error("Error generating completion", e);
            
            final String finalModel = request.getModel();
            
            OllamaCompletionResponse.OllamaMessage errorMessage = new OllamaCompletionResponse.OllamaMessage(
                "assistant", 
                "I'm sorry, I encountered an error while processing your request. Please try again later."
            );
            
            OllamaCompletionResponse errorResponse = OllamaCompletionResponse.builder()
                .model(finalModel)
                .message(errorMessage)
                .done(true)
                .build();
                
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @PostMapping(path = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(
            summary = "Stream chat completion",
            description = "Generate a streaming chat completion from Ollama as Server-Sent Events",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "SSE stream of completion tokens"
                    )
            }
    )
    public Flux<ServerSentEvent<Object>> streamCompletion(@RequestBody ChatRequest request) {
        try {
            boolean streaming = Optional.ofNullable(request.getStreaming()).orElse(true);
            log.info("Streaming request with model: {}, streaming: {}, options: {}", 
                    request.getModel(), streaming, request.getOptions());
            
            return ollamaService.streamCompletion(
                    request.getModel(), 
                    request.getMessages(), 
                    streaming,
                    request.getOptions()
            )
            .onErrorResume(e -> {
                log.error("Error in streaming: ", e);
                
                Map<String, Object> errorData = Map.of(
                    "error", e.getMessage() != null ? e.getMessage() : "Unknown error",
                    "done", true
                );
                
                return Flux.just(ServerSentEvent.builder()
                    .event("error")
                    .data(errorData)
                    .build());
            });
        } catch (Exception e) {
            log.error("Error setting up stream", e);
            
            Map<String, Object> errorData = Map.of(
                "error", "Failed to initialize streaming",
                "done", true
            );
            
            return Flux.just(ServerSentEvent.builder()
                .event("error")
                .data(errorData)
                .build());
        }
    }
}
