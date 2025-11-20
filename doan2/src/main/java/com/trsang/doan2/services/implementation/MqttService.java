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
    public Mqtt registerMqttDevice(String username, String password, String brokerUrl) {
        log.info("Registering MQTT device: {}", username);
        
        // Check if device already exists
        Optional<Mqtt> existing = getMqttDeviceByUsername(username);
        if (existing.isPresent()) {
            throw new RuntimeException("MQTT device already exists for username: " + username);
        }
        
        Mqtt mqtt = Mqtt.builder()
                .username(username)
                .password(passwordEncoder.encode(password))
                .brokerUrl(brokerUrl)
                .isActive(true)
                .build();
        
        return mqttRepository.save(mqtt);
    }

    @Override
    public Mqtt updateMqttDevice(UUID id, String password, boolean isActive) {
        log.info("Updating MQTT device: {}", id);
        
        Mqtt mqtt = getMqttDeviceById(id)
                .orElseThrow(() -> new RuntimeException("MQTT device not found: " + id));

        if (password != null && !password.isEmpty()) {
            mqtt.setPassword(passwordEncoder.encode(password));
        }
        mqtt.setActive(isActive);
        mqtt.setUpdatedAt(Instant.now());

        return mqttRepository.save(mqtt);
    }

    @Override
    public Optional<Mqtt> getMqttDeviceByUsername(String username) {
        return mqttRepository.findByUsername(username);
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
