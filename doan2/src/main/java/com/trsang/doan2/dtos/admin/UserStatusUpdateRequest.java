package com.trsang.doan2.dtos.admin;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserStatusUpdateRequest {
    private String reason;
    private Instant lockedUntil;
    private Boolean isActive;
}