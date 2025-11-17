package com.trsang.doan2.services.implementation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import com.trsang.doan2.services.interfaces.IWebSocketService;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketService implements IWebSocketService {

    private final SimpMessagingTemplate simpMessagingTemplate;

    // Map to track user sessions: username -> Set of session IDs
    private final Map<String, Set<String>> userSessions = new ConcurrentHashMap<>();

    @Override
    public void broadcastToTopic(String topic, Object message) {
        try {
            log.debug("Broadcasting to topic: {}", topic);
            simpMessagingTemplate.convertAndSend(topic, message);
        } catch (Exception e) {
            log.error("Error broadcasting to topic {}: {}", topic, e.getMessage(), e);
        }
    }

    @Override
    public void sendToUser(String username, String topic, Object message) {
        try {
            if (!isUserConnected(username)) {
                log.warn("User {} is not connected", username);
                return;
            }
            
            String destination = String.format("/user/%s%s", username, topic);
            log.debug("Sending message to user {} on topic: {}", username, destination);
            simpMessagingTemplate.convertAndSendToUser(username, topic, message);
        } catch (Exception e) {
            log.error("Error sending message to user {}: {}", username, e.getMessage(), e);
        }
    }

    @Override
    public void registerUserSession(String username, String sessionId) {
        userSessions.computeIfAbsent(username, k -> ConcurrentHashMap.newKeySet()).add(sessionId);
        log.info("Registered user session: {} -> {}", username, sessionId);
    }

    @Override
    public void unregisterUserSession(String username, String sessionId) {
        Set<String> sessions = userSessions.get(username);
        if (sessions != null) {
            sessions.remove(sessionId);
            if (sessions.isEmpty()) {
                userSessions.remove(username);
                log.info("Unregistered last session for user: {}", username);
            } else {
                log.info("Unregistered session for user {}: {}", username, sessionId);
            }
        }
    }

    @Override
    public List<String> getConnectedUsers() {
        return new ArrayList<>(userSessions.keySet());
    }

    @Override
    public boolean isUserConnected(String username) {
        Set<String> sessions = userSessions.get(username);
        return sessions != null && !sessions.isEmpty();
    }

    @Override
    public int getActiveConnectionCount() {
        return userSessions.values().stream()
                .mapToInt(Set::size)
                .sum();
    }
}
