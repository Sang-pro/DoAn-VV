package com.trsang.doan2.services.interfaces;

import org.springframework.web.socket.WebSocketSession;

public interface ISocketService {
    void addSession(WebSocketSession session);

    void removeSession(WebSocketSession session);

    void sendMessage(String topic, Object data);
}
