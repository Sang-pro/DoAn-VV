package com.trsang.doan2.services.interfaces;

import com.trsang.doan2.entities.Mqtt;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IMqttService {
    /**
     * Register a new MQTT device/credential
     */
    Mqtt registerMqttDevice(String mqttUsername, String mqttPassword, String brokerUrl);

    /**
     * Update MQTT device credentials
     */
    Mqtt updateMqttDevice(UUID id, String mqttPassword, boolean isActive);

    /**
     * Get MQTT device by username
     */
    Optional<Mqtt> getMqttDeviceByUsername(String mqttUsername);

    /**
     * Get all active MQTT devices
     */
    List<Mqtt> getActiveMqttDevices();

    /**
     * Get MQTT device by ID
     */
    Optional<Mqtt> getMqttDeviceById(UUID id);

    /**
     * Deactivate MQTT device
     */
    void deactivateMqttDevice(UUID id);

    /**
     * Delete MQTT device
     */
    void deleteMqttDevice(UUID id);

    /**
     * Publish message to MQTT topic
     */
    void publishMessage(String topic, String message, int qos);

    /**
     * Subscribe to MQTT topic
     */
    void subscribeTopic(String topic, int qos);

    /**
     * Unsubscribe from MQTT topic
     */
    void unsubscribeTopic(String topic);

    /**
     * Check if MQTT broker is connected
     */
    boolean isBrokerConnected();
}
