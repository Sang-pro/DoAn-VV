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
            String payloadStr = payload instanceof byte[] ? new String((byte[]) payload) : payload.toString();
            
            Object wsPayload = payloadStr;
            try {
                // Try to parse the payload as JSON so that SocketService serializes it as a proper JSON object
                wsPayload = objectMapper.readTree(payloadStr);
            } catch (Exception e) {
                // Fallback to raw string if it's not valid JSON
            }
            
            socketService.sendMessage(topic, wsPayload);

            // Handle RFID scan at entrance (inventory/location tracking)
            if ("warehouse/rfid/scan".equals(topic)) {
                try {
                    RfidScanRequest scanReq = objectMapper.readValue(payloadStr, RfidScanRequest.class);
                    rfidService.processRfidScan(scanReq);
                } catch (Exception e) {
                    log.error("Failed to parse RFID scan request: ", e);
                }
            }

            // Handle RFID scan at exit gate (security check)
            if ("warehouse/rfid/gate".equals(topic)) {
                try {
                    var jsonNode = objectMapper.readTree(payloadStr);
                    String epc = jsonNode.get("epc").asText();
                    String deviceId = jsonNode.has("deviceId") ? jsonNode.get("deviceId").asText() : "UNKNOWN";
                    rfidService.processGateScan(epc, deviceId);
                } catch (Exception e) {
                    log.error("Failed to parse gate scan request: ", e);
                }
            }
        }
    }

}
