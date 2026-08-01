package com.trsang.doan2.services.implementation;

import com.trsang.doan2.dtos.requests.RfidScanRequest;
import com.trsang.doan2.entities.BorrowRecord;
import com.trsang.doan2.entities.EslTag;
import com.trsang.doan2.entities.Book;
import com.trsang.doan2.entities.RfidTag;
import com.trsang.doan2.repositories.IBorrowRecordRepository;
import com.trsang.doan2.repositories.IEslTagRepository;
import com.trsang.doan2.repositories.IBookRepository;
import com.trsang.doan2.repositories.IRfidTagRepository;
import com.trsang.doan2.services.interfaces.IRfidService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.trsang.doan2.services.interfaces.MqttGateway;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.Optional;
import java.util.Map;
import java.util.HashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class RfidService implements IRfidService {

    private final IRfidTagRepository rfidTagRepository;
    private final IEslTagRepository eslTagRepository;
    private final IBookRepository bookRepository;
    private final IBorrowRecordRepository borrowRecordRepository;
    private final MqttGateway mqttGateway;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void processRfidScan(RfidScanRequest request) {
        log.info("Processing RFID Scan: EPC={}, Location={}", request.getEpc(), request.getLocation());

        Optional<RfidTag> rfidTagOpt = rfidTagRepository.findByEpc(request.getEpc());
        if (rfidTagOpt.isEmpty()) {
            log.warn("RFID tag with EPC {} not found.", request.getEpc());
            return;
        }

        RfidTag rfidTag = rfidTagOpt.get();
        rfidTag.setCurrentLocation(request.getLocation());
        rfidTag.setLastScannedAt(Instant.now());
        rfidTagRepository.save(rfidTag);

        Book scannedBook = rfidTag.getBook();

        // Find the shelf (EslTag) by location
        Optional<EslTag> eslTagOpt = eslTagRepository.findByLocation(request.getLocation());
        if (eslTagOpt.isPresent()) {
            EslTag eslTag = eslTagOpt.get();
            eslTag.setBook(scannedBook);
            eslTag.setLastSeen(Instant.now());
            eslTagRepository.save(eslTag);
            log.info("Updated Shelf {} with Book ID {}", request.getLocation(), scannedBook.getId());
        } else {
            log.warn("Shelf location {} not found in EslTag database.", request.getLocation());
        }
    }

    /**
     * Xử lý quét RFID tại cổng ra (Exit Gate Security Check).
     * Kiểm tra sách có đang được mượn hợp lệ không.
     * Nếu KHÔNG → gửi lệnh báo động qua MQTT đến ESP32 cổng ra.
     */
    @Transactional(readOnly = true)
    public void processGateScan(String epc, String deviceId) {
        log.info("Gate Scan: EPC={}, Device={}", epc, deviceId);

        // Tìm thẻ RFID trong database
        Optional<RfidTag> rfidTagOpt = rfidTagRepository.findByEpc(epc);
        if (rfidTagOpt.isEmpty()) {
            log.info("Gate: RFID tag EPC {} khong ton tai trong he thong (Chua dang ky). Khong bao dong.", epc);
            sendAlarm(epc, deviceId, false, "Thẻ chưa đăng ký");
            return;
        }

        RfidTag rfidTag = rfidTagOpt.get();
        Book book = rfidTag.getBook();

        if (book == null) {
            log.info("Gate: RFID tag EPC {} ton tai nhung chua duoc gan voi sach. Khong bao dong.", epc);
            sendAlarm(epc, deviceId, false, "Thẻ chưa gắn sách");
            return;
        }

        // Kiểm tra sách có đang được mượn hợp lệ không (status = "BORROWED")
        Optional<BorrowRecord> activeBorrow = borrowRecordRepository
                .findByBookIdAndStatus(book.getId(), "BORROWED");

        if (activeBorrow.isPresent()) {
            // Sách đã được mượn hợp lệ → cho qua
            log.info("Gate: Sach '{}' da duoc muon boi User {}. Cho qua.",
                    book.getTitle(), activeBorrow.get().getUser().getId());
            sendAlarm(epc, deviceId, false, book.getTitle());
        } else {
            // Sách CHƯA được mượn → BÁO ĐỘNG!
            log.warn("Gate: CANH BAO! Sach '{}' (EPC={}) CHUA DUOC MUON! BAO DONG!",
                    book.getTitle(), epc);
            sendAlarm(epc, deviceId, true, book.getTitle());
        }
    }

    /**
     * Gửi lệnh báo động/OK qua MQTT đến ESP32 cổng ra.
     */
    private void sendAlarm(String epc, String deviceId, boolean alarm, String bookTitle) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("alarm", alarm);
            payload.put("epc", epc);
            payload.put("deviceId", deviceId);
            payload.put("bookTitle", bookTitle);

            String json = objectMapper.writeValueAsString(payload);

            mqttGateway.sendToMqtt("warehouse/rfid/alarm", json);

            log.info("Gate: Gui {} den thiet bi {}: {}",
                    alarm ? "BAO DONG" : "OK", deviceId, json);
        } catch (Exception e) {
            log.error("Gate: Loi gui MQTT alarm: ", e);
        }
    }

    @Override
    public RfidTag registerRfidTag(String epc, Long bookId) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new RuntimeException("Book not found"));

        RfidTag rfidTag = RfidTag.builder()
                .epc(epc)
                .book(book)
                .build();
        return rfidTagRepository.save(rfidTag);
    }
}

