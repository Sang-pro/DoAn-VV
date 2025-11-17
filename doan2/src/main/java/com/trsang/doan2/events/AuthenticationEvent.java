package com.trsang.doan2.events;

import org.springframework.context.ApplicationEvent;

import lombok.Getter;

@Getter
public class AuthenticationEvent extends ApplicationEvent {
    private final String username;
    private final AuthEventType eventType;
    private final String message;
    private final String ipAddress;

    public AuthenticationEvent
        (Object source, String username, AuthEventType eventType, String message, String ipAddress) {
        // Call the constructor of the superclass (ApplicationEvent)
        super(source);
        this.username = username;
        this.eventType = eventType;
        this.message = message;
        this.ipAddress = ipAddress;
    }
    
    public enum AuthEventType {
        LOGIN_SUCCESS,
        LOGIN_FAILED,
        LOGOUT,
        REGISTER_SUCCESS,
        REFRESH_TOKEN,
        INVALID_TOKEN,
        ACCOUNT_LOCKED
    }
}
