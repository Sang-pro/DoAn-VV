package com.trsang.doan2.dtos.ollama;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OllamaApiResponses {
    private List<OllamaModelResponse> models;
    private String status;

    private String modelfile;
    private String parameter;
    private String template;
    private OllamaModelDetails details;
    private Object model_info;
}
