package com.trsang.doan2.listeners;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.trsang.doan2.events.AuthenticationEvent;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class AuthenticationListener {
    @EventListener
    public void handleAuthenticationEvent(AuthenticationEvent event) {
        switch (event.getEventType()) {
            case LOGIN_SUCCESS:
                log.info("User {} logged in successfully from IP {}", 
                            event.getUsername(), event.getIpAddress());
                break;
            case LOGIN_FAILED:
                log.warn("Failed login attempt for user {} from IP {} reason {}", 
                            event.getUsername(), event.getIpAddress(), event.getMessage());
                break;
            case LOGOUT:
                log.info("User {} logged out from IP {}", 
                            event.getUsername(), event.getIpAddress());
                break;
            case REGISTER_SUCCESS:
                log.info("User {} registered successfully from IP {}",
                            event.getUsername(), event.getIpAddress());
                break;
            case REFRESH_TOKEN:
                log.info("Token refreshed for user {} from IP {}", 
                            event.getUsername(), event.getIpAddress());
                break;
            case INVALID_TOKEN:
                log.warn("Invalid token for user {} from IP {} reason {}", 
                            event.getUsername(), event.getIpAddress(), event.getMessage());
                break;
            case ACCOUNT_LOCKED:
                log.warn("Account locked for user {} from IP {} reason {}", 
                            event.getUsername(), event.getIpAddress(), event.getMessage());
                break;
        }
    }
}
