package com.trsang.doan2.controllers;

import com.trsang.doan2.entities.RfidTag;
import com.trsang.doan2.repositories.IRfidTagRepository;
import com.trsang.doan2.services.interfaces.IRfidService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rfid")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
@PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
public class RfidController {

    private final IRfidTagRepository rfidTagRepository;
    private final IRfidService rfidService;

    @GetMapping
    public List<RfidTag> getAllRfidTags() {
        return rfidTagRepository.findAll();
    }

    @PostMapping("/register")
    public ResponseEntity<RfidTag> registerRfidTag(@RequestBody RfidRegisterRequest request) {
        log.info("Registering RFID tag: EPC={}, bookId={}", request.getEpc(), request.getBookId());
        RfidTag registered = rfidService.registerRfidTag(request.getEpc(), request.getBookId());
        return ResponseEntity.ok(registered);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRfidTag(@PathVariable Long id) {
        log.info("Deleting RFID tag ID: {}", id);
        rfidTagRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }

    @lombok.Data
    public static class RfidRegisterRequest {
        private String epc;
        private Long bookId;
    }
}
