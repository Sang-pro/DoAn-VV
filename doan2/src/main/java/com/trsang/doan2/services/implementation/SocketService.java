package com.trsang.doan2.services.implementation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trsang.doan2.dtos.responses.WebSocketMessageResponse;
import com.trsang.doan2.services.interfaces.ISocketService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
@RequiredArgsConstructor
public class SocketService implements ISocketService {

    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;

    @Override
    public void addSession(WebSocketSession session) {
        sessions.put(session.getId(), session);
    }

    @Override
    public void removeSession(WebSocketSession session) {
        sessions.remove(session.getId());
    }

    @Override
    public void sendMessage(String topic, Object data) {
        WebSocketMessageResponse message = WebSocketMessageResponse.builder()
                .topic(topic)
                .message(data)
                .timestamp(Instant.now().toString())
                .build();

        try {
            String jsonMessage = objectMapper.writeValueAsString(message);
            for (WebSocketSession session : sessions.values()) {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(jsonMessage));
                }
            }
        } catch (IOException e) {
            log.error("Error sending message to websocket", e);
        }
    }
}
