package com.trsang.doan2.services.implementation;

import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;

import com.trsang.doan2.services.interfaces.ISocketService;

@Service
public class MqttInboundToWebSocketBridge {

    private final ISocketService socketService;

    public MqttInboundToWebSocketBridge(ISocketService socketService) {
        this.socketService = socketService;
    }

    @ServiceActivator(inputChannel = "mqttInputChannel")
    public void handleMqttMessage(Message<?> message) {
        String topic = (String) message.getHeaders().get(MqttHeaders.RECEIVED_TOPIC);
        Object payload = message.getPayload();

        if (topic != null) {
            socketService.sendMessage(topic, payload);
        }
    }

}
