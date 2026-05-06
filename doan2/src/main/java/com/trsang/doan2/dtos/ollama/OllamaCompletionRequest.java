package com.trsang.doan2.dtos.ollama;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OllamaCompletionRequest {
    private String model;
    private List<OllamaMessage> messages;

    @JsonProperty("stream")
    private Boolean stream;

    private Map<String, Object> options;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class OllamaMessage {
        private String role;
        private String content;

    }
}
