package com.trsang.doan2.services.interfaces;

import java.util.List;
import java.util.Map;
import org.springframework.http.codec.ServerSentEvent;
import com.trsang.doan2.dtos.ollama.OllamaCompletionResponse;
import reactor.core.publisher.Flux;

public interface IOllamaService {
    default OllamaCompletionResponse generateCompletion(String model, List<Map<String, String>> messages) {
        return generateCompletion(model, messages, null);
    }
    
    OllamaCompletionResponse generateCompletion(String model, List<Map<String, String>> messages, Map<String, Object> options);
    
    Flux<ServerSentEvent<Object>> streamCompletion(
            String model, 
            List<Map<String, String>> messages, 
            boolean streaming,
            Map<String, Object> options
    );
}
