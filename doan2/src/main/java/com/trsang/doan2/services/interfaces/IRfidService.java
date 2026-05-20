package com.trsang.doan2.services.interfaces;

import com.trsang.doan2.dtos.requests.RfidScanRequest;
import com.trsang.doan2.entities.RfidTag;

public interface IRfidService {
    void processRfidScan(RfidScanRequest request);
    RfidTag registerRfidTag(String epc, Long bookId);
}
