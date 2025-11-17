package com.trsang.doan2.dtos.mqtt;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MqttCredentialsResponse {
    private String id;
    private String mqttUsername;
    private String brokerUrl;
    private boolean isActive;
    private Instant createdAt;
    private Instant updatedAt;
}
