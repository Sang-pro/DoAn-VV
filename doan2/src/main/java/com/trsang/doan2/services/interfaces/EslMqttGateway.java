package com.trsang.doan2.services.interfaces;

import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.integration.mqtt.support.MqttHeaders;

@MessagingGateway(defaultRequestChannel = "mqttOutboundChannel")
public interface EslMqttGateway {
    void sendToMqtt(@Header(MqttHeaders.TOPIC) String topic, String data);
}
