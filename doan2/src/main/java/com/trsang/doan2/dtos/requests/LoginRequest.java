package com.trsang.doan2.dtos.requests;

import lombok.Data;
import lombok.NoArgsConstructor;

import com.trsang.doan2.events.AuthProvider;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LoginRequest {
    @NotBlank(message = "Username is required")
    private String username;
    
    @NotBlank(message = "Password is required")
    private String password;

    private AuthProvider provider;
}
