package com.trsang.doan2.dtos.requests;

import com.trsang.doan2.events.AuthProvider;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserOauthRequest {
    @NotBlank(message = "clientId is required")
    private String clientId;

    @NotBlank(message = "token is required")
    private String token;

    @NotNull(message = "provider is required")
    private AuthProvider provider;
}
