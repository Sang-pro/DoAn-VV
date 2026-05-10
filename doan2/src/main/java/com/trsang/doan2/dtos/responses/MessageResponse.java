package com.trsang.doan2.dtos.responses;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MessageResponse {
    private String message;
    private boolean success;
    private String token;

    public MessageResponse(String message, boolean success) {
        this.message = message;
        this.success = success;
    }
}
