package com.trsang.doan2.controllers;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.trsang.doan2.dtos.ollama.OllamaModelDetails;
import com.trsang.doan2.dtos.ollama.OllamaModelResponse;
import com.trsang.doan2.services.interfaces.IOllamaModelService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequestMapping("/api/ollama/models")
@RequiredArgsConstructor
@Tag(name = "Ollama Models", description = "Endpoints for managing and interacting with Ollama models")
@SecurityRequirement(name = "bearerAuth")
@RestController
public class OllamaModelController {

    private final IOllamaModelService ollamaModelService;

    @Data
    public static class ModelRequest {
        private String model;
        private boolean insecure;
        private boolean stream = false;
    }

    @Data
    public static class CopyModelRequest {
        private String source;
        private String destination;
    }

    @GetMapping
    @Operation(
            summary = "List local models",
            description = "List all models available locally",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Models retrieved successfully",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = OllamaModelResponse.class)))
                    )
            }
    )
    public ResponseEntity<List<OllamaModelResponse>> listLocalModels() {
        try {
            List<OllamaModelResponse> models = ollamaModelService.listLocalModels();
            return ResponseEntity.ok(models);
        } catch (Exception e) {
            log.error("Error listing local models", e);
            return ResponseEntity.ok(Collections.emptyList());
        }
    }

    @GetMapping("/running")
    @Operation(
            summary = "List running models",
            description = "List all models currently running",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Running models retrieved successfully",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = OllamaModelResponse.class)))
                    )
            }
    )
    public ResponseEntity<List<OllamaModelResponse>> listRunningModels() {
        try {
            List<OllamaModelResponse> models = ollamaModelService.listRunningModels();
            return ResponseEntity.ok(models);
        } catch (Exception e) {
            log.error("Error listing running models", e);
            return ResponseEntity.ok(Collections.emptyList());
        }
    }

    @GetMapping("/{model}")
    @Operation(
            summary = "Get model details",
            description = "Retrieve details for a specific model",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Model details retrieved successfully",
                            content = @Content(schema = @Schema(implementation = OllamaModelResponse.class))
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Model not found"
                    )
            }
    )
    public ResponseEntity<OllamaModelDetails> getModelDetails(@PathVariable String model) {
        try {
            OllamaModelDetails modelDetails = ollamaModelService.getModelDetails(model);
            if (modelDetails == null) {
                return ResponseEntity.notFound().build();
            } else {
                return ResponseEntity.ok(modelDetails);
            }
        } catch (Exception e) {
            log.error("Error retrieving model details for {}", model, e);
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/copy")
    @Operation(
            summary = "Copy a model",
            description = "Copy a model from one location to another",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Model copied successfully"
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Invalid request"
                    )
            }
    )
    public ResponseEntity<Map<String, String>> copyModel(@RequestBody CopyModelRequest request) {
        try {
            boolean result = ollamaModelService.copyModel(request.getSource(), request.getDestination());
            if (result) {
                return ResponseEntity.ok(Map.of("message", "Model copied successfully"));
            } else {
                return ResponseEntity.badRequest().body(Map.of("message" ,"Failed to copy model"));
            }
        } catch (Exception e) {
            log.error("Error copying model from {} to {}", request.getSource(), request.getDestination(), e);
            return ResponseEntity.badRequest().body(Map.of("message", "Error" + e.getMessage()));
        }
    }

    @DeleteMapping("/{model}")
    @Operation(
            summary = "Delete a model",
            description = "Delete a specific model",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Model deleted successfully"
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Model not found"
                    )
            }
    )
    public ResponseEntity<Map<String, String>> deleteModel(@PathVariable String model) {
        try {
            boolean result = ollamaModelService.deleteModel(model);
            if (result) {
                return ResponseEntity.ok(Map.of("message", "Model deleted successfully"));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Model not found"));
            }
        } catch (Exception e) {
            log.error("Error deleting model {}", model, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body(Map.of("message", "Error: " + e.getMessage()));
        }
    }

    @PostMapping("/push")
    @Operation(
            summary = "Push a model",
            description = "Upload a model to Ollama model library",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Model pushed successfully"
                    )
            }
    )
    public ResponseEntity<Map<String, String>> pushModel(@RequestBody ModelRequest request) {
        try {
            String status = ollamaModelService.pushModel(request.getModel(), request.isInsecure(), request.isStream());
            return ResponseEntity.ok(Map.of("status", status));
        } catch (Exception e) {
            log.error("Error pushing model {}", request.getModel(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body(Map.of("message", "error: " + e.getMessage()));
        }
    }

    @PostMapping("/pull")
    @Operation(
            summary = "Pull a model",
            description = "Download a model from Ollama model library",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Model pulled successfully"
                    )
            }
    )
    public ResponseEntity<Map<String, String>> pullModel(@RequestBody ModelRequest request) {
        try {
            String status = ollamaModelService.pullModel(request.getModel(), request.isInsecure(), request.isStream());
            return ResponseEntity.ok(Map.of("status", status));
        } catch (Exception e) {
            log.error("Error pulling model {}", request.getModel(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body(Map.of("message", "error: " + e.getMessage()));
        }
    }

    @GetMapping("/available")
    @Operation(
            summary = "List available models",
            description = "List all models available in the Ollama model library",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Available models retrieved successfully",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = OllamaModelResponse.class)))
                    )
            }
    )
    public ResponseEntity<Map<String, Object>> getAvailableModels() {
        try {
            List<OllamaModelResponse> models = ollamaModelService.listLocalModels();
            List<Map<String, Object>> formattedModels = models.stream()
                    .map(model -> {
                        Map<String, Object> modelMap = new HashMap<>();
                        modelMap.put("name", model.getName());
                        modelMap.put("displayName", model.getName().replace(":", " "));
                        
                        // Extract parameter size if available
                        if (model.getDetails() != null && model.getDetails().getParameterSize() != null) {
                            modelMap.put("size", model.getDetails().getParameterSize());
                        }
                        
                        // Add other relevant details
                        modelMap.put("modified", model.getModifiedAt());
                        
                        return modelMap;
                    })
                    .toList();
                    
            Map<String, Object> response = new HashMap<>();
            response.put("models", formattedModels);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting available models", e);
            return ResponseEntity.ok(Map.of("models", Collections.emptyList()));
        }
    }
}
