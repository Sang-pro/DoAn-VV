package com.trsang.doan2.services.interfaces;

import java.util.List;

import com.trsang.doan2.dtos.ollama.OllamaModelDetails;
import com.trsang.doan2.dtos.ollama.OllamaModelResponse;

public interface IOllamaModelService {

    List<OllamaModelResponse> listLocalModels();
    List<OllamaModelResponse> listRunningModels();

    OllamaModelDetails getModelDetails(String model);
    boolean copyModel(String source, String destination);
    boolean deleteModel(String model);

    String pullModel(String model, boolean insecure, boolean stream);
    String pushModel(String model, boolean insecure, boolean stream);
}
