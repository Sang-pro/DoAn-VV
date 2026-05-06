package com.trsang.doan2.services.implementation;

import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;

import com.trsang.doan2.services.interfaces.ISocketService;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trsang.doan2.dtos.requests.RfidScanRequest;
import com.trsang.doan2.services.interfaces.IRfidService;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class MqttInboundToWebSocketBridge {

    private final ISocketService socketService;
    private final IRfidService rfidService;
    private final ObjectMapper objectMapper;

    public MqttInboundToWebSocketBridge(ISocketService socketService, IRfidService rfidService, ObjectMapper objectMapper) {
        this.socketService = socketService;
        this.rfidService = rfidService;
        this.objectMapper = objectMapper;
    }

    @ServiceActivator(inputChannel = "mqttInputChannel")
    public void handleMqttMessage(Message<?> message) {
        String topic = (String) message.getHeaders().get(MqttHeaders.RECEIVED_TOPIC);
        Object payload = message.getPayload();

        if (topic != null) {
            socketService.sendMessage(topic, payload);
            
            // Handle RFID scan topic
            if ("warehouse/rfid/scan".equals(topic)) {
                try {
                    String payloadStr = payload instanceof byte[] ? new String((byte[]) payload) : payload.toString();
                    RfidScanRequest scanReq = objectMapper.readValue(payloadStr, RfidScanRequest.class);
                    rfidService.processRfidScan(scanReq);
                } catch (Exception e) {
                    log.error("Failed to parse RFID scan request: ", e);
                }
            }
        }
    }

}
