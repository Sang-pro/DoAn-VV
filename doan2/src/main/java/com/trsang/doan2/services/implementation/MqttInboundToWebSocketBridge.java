package com.trsang.doan2.services.implementation;

import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class MqttInboundToWebSocketBridge {

    private final SimpMessagingTemplate simpMessagingTemplate;

    public MqttInboundToWebSocketBridge(SimpMessagingTemplate simpMessagingTemplate) {
        this.simpMessagingTemplate = simpMessagingTemplate;
    }

    @ServiceActivator(inputChannel = "mqttInputChannel")
    public void handleMqttMessage(Message<?> message) {
        String topic = (String) message.getHeaders().get(MqttHeaders.RECEIVED_TOPIC);
        Object payload = message.getPayload();

        // topic pattern is expected: data/{mqttUsername}/sensors
        String mqttUsername = "unknown";
        if (topic != null) {
            String[] parts = topic.split("/");
            if (parts.length >= 2) {
                mqttUsername = parts[1];
            }
        }

        // Forward payload to STOMP destination for that device/user
        String destination = "/topic/sensors/" + mqttUsername;
        simpMessagingTemplate.convertAndSend(destination, payload);
    }

}
