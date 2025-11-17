package com.trsang.doan2.dtos.mqtt;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotBlank;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MqttCredentialsRequest {
    @NotBlank(message = "Username is required")
    private String mqttUsername;
    
    @NotBlank(message = "Password is required")
    private String mqttPassword;
    
    private String brokerUrl;
}
