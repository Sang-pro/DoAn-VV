package com.trsang.doan2.services.implementation;

import com.trsang.doan2.entities.Mqtt;
import com.trsang.doan2.repositories.IMqttRepository;
import com.trsang.doan2.services.interfaces.IMqttService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MqttService implements IMqttService {

    private final IMqttRepository mqttRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public Mqtt registerMqttDevice(String mqttUsername, String mqttPassword, String brokerUrl) {
        log.info("Registering MQTT device: {}", mqttUsername);
        
        // Check if device already exists
        Optional<Mqtt> existing = getMqttDeviceByUsername(mqttUsername);
        if (existing.isPresent()) {
            throw new RuntimeException("MQTT device already exists for username: " + mqttUsername);
        }
        
        Mqtt mqtt = Mqtt.builder()
                .mqttUsername(mqttUsername)
                .mqttPassword(passwordEncoder.encode(mqttPassword))
                .brokerUrl(brokerUrl)
                .isActive(true)
                .build();
        
        return mqttRepository.save(mqtt);
    }

    @Override
    public Mqtt updateMqttDevice(UUID id, String mqttPassword, boolean isActive) {
        log.info("Updating MQTT device: {}", id);
        
        Mqtt mqtt = getMqttDeviceById(id)
                .orElseThrow(() -> new RuntimeException("MQTT device not found: " + id));

        if (mqttPassword != null && !mqttPassword.isEmpty()) {
            mqtt.setMqttPassword(passwordEncoder.encode(mqttPassword));
        }
        mqtt.setActive(isActive);
        mqtt.setUpdatedAt(Instant.now());

        return mqttRepository.save(mqtt);
    }

    @Override
    public Optional<Mqtt> getMqttDeviceByUsername(String mqttUsername) {
        return mqttRepository.findByMqttUsername(mqttUsername);
    }

    @Override
    public List<Mqtt> getActiveMqttDevices() {
        return mqttRepository.findByIsActiveTrue();
    }

    @Override
    public Optional<Mqtt> getMqttDeviceById(UUID id) {
        return mqttRepository.findById(id);
    }

    @Override
    public void deactivateMqttDevice(UUID id) {
        log.info("Deactivating MQTT device: {}", id);
        
        Mqtt mqtt = getMqttDeviceById(id)
                .orElseThrow(() -> new RuntimeException("MQTT device not found: " + id));
        
        mqtt.setActive(false);
        mqtt.setUpdatedAt(Instant.now());
        mqttRepository.save(mqtt);
    }

    @Override
    public void deleteMqttDevice(UUID id) {
        log.info("Deleting MQTT device: {}", id);
        mqttRepository.deleteById(id);
    }

    @Override
    public void publishMessage(String topic, String message, int qos) {
        log.debug("Publishing to MQTT topic {}: {}", topic, message);
        // Publish via Spring Integration channel if configured
        // Implementation depends on your MQTT broker setup
    }

    @Override
    public void subscribeTopic(String topic, int qos) {
        log.info("Subscribing to MQTT topic: {}", topic);
        // Subscribe via Spring Integration if configured
    }

    @Override
    public void unsubscribeTopic(String topic) {
        log.info("Unsubscribing from MQTT topic: {}", topic);
        // Unsubscribe via Spring Integration if configured
    }

    @Override
    public boolean isBrokerConnected() {
        return true;
    }
}
