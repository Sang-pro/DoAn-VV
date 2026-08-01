package com.trsang.doan2.dtos.requests;

import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RfidRegisterRequest {
    private String epc;
    private Long bookId;
}
