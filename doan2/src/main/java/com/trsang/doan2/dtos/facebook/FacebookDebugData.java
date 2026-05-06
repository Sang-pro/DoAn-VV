package com.trsang.doan2.dtos.facebook;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FacebookDebugData {
    @JsonProperty("app_id")
    private String appId;
    private String type;
    private String application;
    @JsonProperty("is_valid")
    private boolean isValid;
    @JsonProperty("user_id")
    private String userId;
}
