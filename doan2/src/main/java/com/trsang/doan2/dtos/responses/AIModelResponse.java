package com.trsang.doan2.dtos.responses;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AIModelResponse {
    private UUID id;
    private String name;
    private String displayName;
    private String description;
    private String category;
    private Long contextLength;
    private Boolean isActive;
}