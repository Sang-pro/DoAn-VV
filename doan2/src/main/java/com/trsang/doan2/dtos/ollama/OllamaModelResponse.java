package com.trsang.doan2.dtos.ollama;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OllamaModelResponse {
    private String name;
    private String model;
    private Long size;
    private String digest;
    
    @JsonProperty("modified_at")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSZ")
    private String modifiedAt;
    
    private OllamaModelDetails details;
    
    @JsonProperty("expires_at")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSZ")
    private String expiresAt;
    
    @JsonProperty("size_vram")
    private Long sizeVram;
}
