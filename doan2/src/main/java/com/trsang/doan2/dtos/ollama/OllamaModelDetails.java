package com.trsang.doan2.dtos.ollama;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Builder;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OllamaModelDetails {
    private String format;
    private String family;
    private List<String> familes;

    @JsonProperty("parameter_size")
    private String parameterSize;

    @JsonProperty("quantization_level")
    private String quantizationLevel;

    @JsonProperty("parent_model")
    private String parentModel;
}
