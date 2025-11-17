package com.trsang.doan2.dtos.admin;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserAdminResponse {
    private UUID id;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String displayName;
    private String phoneNumber;
    private Boolean isActive;
    private List<String> roles;
    private Instant lockedUntil;
    private Instant createdAt;
    private Instant updatedAt;

    private Integer chatCount;
    private Integer messageCount;
    private Instant lastActivity;
}
