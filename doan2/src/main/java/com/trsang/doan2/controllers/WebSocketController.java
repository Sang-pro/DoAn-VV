package com.trsang.doan2.controllers;

import com.trsang.doan2.dtos.responses.MessageResponse;
import com.trsang.doan2.services.interfaces.IWebSocketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/websocket")
@RequiredArgsConstructor
public class WebSocketController {

    private final IWebSocketService webSocketService;

    @GetMapping("/connected-users")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<Map<String, Object>> getConnectedUsers() {
        try {
            List<String> users = webSocketService.getConnectedUsers();
            int activeCount = webSocketService.getActiveConnectionCount();
            
            Map<String, Object> response = new HashMap<>();
            response.put("users", users);
            response.put("count", users.size());
            response.put("activeConnections", activeCount);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching connected users: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/is-connected/{username}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<Map<String, Object>> isUserConnected(@PathVariable String username) {
        try {
            boolean isConnected = webSocketService.isUserConnected(username);
            
            Map<String, Object> response = new HashMap<>();
            response.put("username", username);
            response.put("isConnected", isConnected);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error checking user connection: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/broadcast")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<MessageResponse> broadcastMessage(
            @RequestParam String topic,
            @RequestBody String message) {
        try {
            webSocketService.broadcastToTopic(topic, message);
            return ResponseEntity.ok(MessageResponse.builder()
                    .message("Message broadcasted successfully to topic: " + topic)
                    .success(true)
                    .build());
        } catch (Exception e) {
            log.error("Error broadcasting message: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(MessageResponse.builder()
                            .message("Failed to broadcast message: " + e.getMessage())
                            .success(false)
                            .build());
        }
    }

    @PostMapping("/send-to-user/{username}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<MessageResponse> sendToUser(
            @PathVariable String username,
            @RequestParam String topic,
            @RequestBody String message) {
        try {
            if (!webSocketService.isUserConnected(username)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(MessageResponse.builder()
                                .message("User not connected: " + username)
                                .success(false)
                                .build());
            }
            
            webSocketService.sendToUser(username, topic, message);
            return ResponseEntity.ok(MessageResponse.builder()
                    .message("Message sent to user: " + username)
                    .success(true)
                    .build());
        } catch (Exception e) {
            log.error("Error sending message to user {}: {}", username, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(MessageResponse.builder()
                            .message("Failed to send message: " + e.getMessage())
                            .success(false)
                            .build());
        }
    }

    @GetMapping("/stats")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<Map<String, Object>> getWebSocketStats() {
        try {
            Map<String, Object> stats = new HashMap<>();
            stats.put("activeConnections", webSocketService.getActiveConnectionCount());
            stats.put("connectedUsers", webSocketService.getConnectedUsers().size());
            stats.put("usersList", webSocketService.getConnectedUsers());
            
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("Error fetching WebSocket stats: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
