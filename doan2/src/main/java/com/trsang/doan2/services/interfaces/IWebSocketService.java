package com.trsang.doan2.services.interfaces;

import java.util.List;

public interface IWebSocketService {
    /**
     * Broadcast a message to all connected WebSocket clients on a specific topic
     */
    void broadcastToTopic(String topic, Object message);

    /**
     * Send a message to a specific user (if they have an active WebSocket connection)
     */
    void sendToUser(String username, String topic, Object message);

    /**
     * Register a connected user session
     */
    void registerUserSession(String username, String sessionId);

    /**
     * Unregister a disconnected user session
     */
    void unregisterUserSession(String username, String sessionId);

    /**
     * Get list of connected users
     */
    List<String> getConnectedUsers();

    /**
     * Check if a user is currently connected
     */
    boolean isUserConnected(String username);

    /**
     * Get count of active WebSocket connections
     */
    int getActiveConnectionCount();
}
