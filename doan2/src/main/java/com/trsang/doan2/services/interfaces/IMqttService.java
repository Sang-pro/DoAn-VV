package com.trsang.doan2.services.interfaces;

import com.trsang.doan2.entities.Mqtt;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IMqttService {
    /**
     * Register a new MQTT device/credential
     */
    Mqtt registerMqttDevice(String username, String password, String brokerUrl);

    /**
     * Update MQTT device credentials
     */
    Mqtt updateMqttDevice(UUID id, String password, boolean isActive);

    /**
     * Get MQTT device by username
     */
    Optional<Mqtt> getMqttDeviceByUsername(String username);

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
