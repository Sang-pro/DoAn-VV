package com.trsang.doan2.controllers;

import com.trsang.doan2.dtos.mqtt.MqttCredentialsRequest;
import com.trsang.doan2.dtos.mqtt.MqttCredentialsResponse;
import com.trsang.doan2.dtos.responses.MessageResponse;
import com.trsang.doan2.entities.Mqtt;
import com.trsang.doan2.exceptions.RunTimeException;
import com.trsang.doan2.services.interfaces.IMqttService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/mqtt")
@RequiredArgsConstructor
public class MqttController {

    private final IMqttService mqttService;

    @PostMapping("/register")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<MqttCredentialsResponse> registerMqttDevice(
            @Valid @RequestBody MqttCredentialsRequest request) {
        try {
            log.info("Registering MQTT device: {}", request.getMqttUsername());
            
            Mqtt mqtt = mqttService.registerMqttDevice(
                    request.getMqttUsername(),
                    request.getMqttPassword(),
                    request.getBrokerUrl()
            );
            
            MqttCredentialsResponse response = MqttCredentialsResponse.builder()
                    .id(mqtt.getId().toString())
                    .mqttUsername(mqtt.getMqttUsername())
                    .brokerUrl(mqtt.getBrokerUrl())
                    .isActive(mqtt.isActive())
                    .createdAt(mqtt.getCreatedAt())
                    .build();
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            log.error("Error registering MQTT device: {}", e.getMessage(), e);
            throw new RunTimeException(e.getMessage());
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<MqttCredentialsResponse> getMqttDevice(@PathVariable UUID id) {
        try {
            Mqtt mqtt = mqttService.getMqttDeviceById(id)
                    .orElseThrow(() -> new RuntimeException("MQTT device not found: " + id));
            
            MqttCredentialsResponse response = MqttCredentialsResponse.builder()
                    .id(mqtt.getId().toString())
                    .mqttUsername(mqtt.getMqttUsername())
                    .brokerUrl(mqtt.getBrokerUrl())
                    .isActive(mqtt.isActive())
                    .createdAt(mqtt.getCreatedAt())
                    .updatedAt(mqtt.getUpdatedAt())
                    .build();
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching MQTT device {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @GetMapping("/username/{username}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<MqttCredentialsResponse> getMqttDeviceByUsername(@PathVariable String username) {
        try {
            Mqtt mqtt = mqttService.getMqttDeviceByUsername(username)
                    .orElseThrow(() -> new RuntimeException("MQTT device not found for username: " + username));
            
            MqttCredentialsResponse response = MqttCredentialsResponse.builder()
                    .id(mqtt.getId().toString())
                    .mqttUsername(mqtt.getMqttUsername())
                    .brokerUrl(mqtt.getBrokerUrl())
                    .isActive(mqtt.isActive())
                    .createdAt(mqtt.getCreatedAt())
                    .updatedAt(mqtt.getUpdatedAt())
                    .build();
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching MQTT device for username {}: {}", username, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @GetMapping("/active")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<List<MqttCredentialsResponse>> getActiveMqttDevices() {
        try {
            List<Mqtt> devices = mqttService.getActiveMqttDevices();
            List<MqttCredentialsResponse> responses = devices.stream()
                    .map(mqtt -> MqttCredentialsResponse.builder()
                            .id(mqtt.getId().toString())
                            .mqttUsername(mqtt.getMqttUsername())
                            .brokerUrl(mqtt.getBrokerUrl())
                            .isActive(mqtt.isActive())
                            .createdAt(mqtt.getCreatedAt())
                            .updatedAt(mqtt.getUpdatedAt())
                            .build())
                    .toList();
            
            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            log.error("Error fetching active MQTT devices: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<MqttCredentialsResponse> updateMqttDevice(
            @PathVariable UUID id,
            @RequestBody Map<String, Object> updates) {
        try {
            String password = (String) updates.get("password");
            Boolean isActive = (Boolean) updates.get("isActive");
            
            if (isActive == null) {
                isActive = true;
            }
            
            Mqtt mqtt = mqttService.updateMqttDevice(id, password, isActive);
            
            MqttCredentialsResponse response = MqttCredentialsResponse.builder()
                    .id(mqtt.getId().toString())
                    .mqttUsername(mqtt.getMqttUsername())
                    .brokerUrl(mqtt.getBrokerUrl())
                    .isActive(mqtt.isActive())
                    .createdAt(mqtt.getCreatedAt())
                    .updatedAt(mqtt.getUpdatedAt())
                    .build();
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error updating MQTT device {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<MessageResponse> deleteMqttDevice(@PathVariable UUID id) {
        try {
            mqttService.deleteMqttDevice(id);
            return ResponseEntity.ok(MessageResponse.builder()
                    .message("MQTT device deleted successfully")
                    .success(true)
                    .build());
        } catch (Exception e) {
            log.error("Error deleting MQTT device {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(MessageResponse.builder()
                            .message("Failed to delete MQTT device: " + e.getMessage())
                            .success(false)
                            .build());
        }
    }

    @PostMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<MessageResponse> deactivateMqttDevice(@PathVariable UUID id) {
        try {
            mqttService.deactivateMqttDevice(id);
            return ResponseEntity.ok(MessageResponse.builder()
                    .message("MQTT device deactivated successfully")
                    .success(true)
                    .build());
        } catch (Exception e) {
            log.error("Error deactivating MQTT device {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(MessageResponse.builder()
                            .message("Failed to deactivate MQTT device: " + e.getMessage())
                            .success(false)
                            .build());
        }
    }

    @PostMapping("/publish")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<MessageResponse> publishMessage(
            @RequestParam String topic,
            @RequestParam String message,
            @RequestParam(defaultValue = "1") int qos) {
        try {
            mqttService.publishMessage(topic, message, qos);
            return ResponseEntity.ok(MessageResponse.builder()
                    .message("Message published to topic: " + topic)
                    .success(true)
                    .build());
        } catch (Exception e) {
            log.error("Error publishing MQTT message: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(MessageResponse.builder()
                            .message("Failed to publish message: " + e.getMessage())
                            .success(false)
                            .build());
        }
    }

    @GetMapping("/broker-status")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<Map<String, Object>> getBrokerStatus() {
        try {
            boolean isConnected = mqttService.isBrokerConnected();
            
            Map<String, Object> status = new HashMap<>();
            status.put("isConnected", isConnected);
            status.put("status", isConnected ? "Connected" : "Disconnected");
            
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            log.error("Error checking broker status: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
