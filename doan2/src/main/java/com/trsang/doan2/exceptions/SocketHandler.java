package com.trsang.doan2.exceptions;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;

import com.trsang.doan2.services.interfaces.ISocketService;

@Component
@RequiredArgsConstructor
public class SocketHandler implements WebSocketHandler {

    private final ISocketService socketService;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        socketService.addSession(session);
    }

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        // Not implemented
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        // Not implemented
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
        socketService.removeSession(session);
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }
}
