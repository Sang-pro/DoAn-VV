package com.trsang.doan2.dtos.requests;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ChatRequest {
    @Builder.Default
    private String title = "New Chat";

    private String description;
    @NotBlank(message = "Model is required")
    private String model;
}
