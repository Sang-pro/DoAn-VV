package com.trsang.doan2.controllers;

import com.trsang.doan2.entities.EslTag;
import com.trsang.doan2.repositories.IEslTagRepository;
import com.trsang.doan2.repositories.IProductRepository;
import com.trsang.doan2.services.interfaces.EslMqttGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/esl")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class EslTagController {

    private final IEslTagRepository eslTagRepository;
    private final IProductRepository productRepository;
    private final EslMqttGateway eslMqttGateway;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<EslTag> getAllTags() {
        return eslTagRepository.findAll();
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public EslTag createTag(@RequestBody EslTag tag) {
        return eslTagRepository.save(tag);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EslTag> updateTag(@PathVariable Long id, @RequestBody EslTag tagDetails) {
        return eslTagRepository.findById(id).map(tag -> {
            tag.setMacAddress(tagDetails.getMacAddress());
            tag.setLocation(tagDetails.getLocation());
            tag.setBatteryLevel(tagDetails.getBatteryLevel());
            tag.setIsOnline(tagDetails.getIsOnline());
            tag.setStatus(tagDetails.getStatus());

            if (tagDetails.getProduct() != null && tagDetails.getProduct().getId() != null) {
                productRepository.findById(tagDetails.getProduct().getId()).ifPresent(tag::setProduct);
            } else {
                tag.setProduct(null);
            }

            EslTag updated = eslTagRepository.save(tag);
            return ResponseEntity.ok(updated);
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteTag(@PathVariable Long id) {
        eslTagRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }

    // "Pick-to-Light" Feature
    @PostMapping("/find/{productId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<String> pickToLight(@PathVariable Long productId) {
        return eslTagRepository.findByProductId(productId).map(tag -> {
            String payload = "{\"action\": \"blink\"}";
            eslMqttGateway.sendToMqtt("esl/find/" + tag.getMacAddress(), payload);
            log.info("Triggered Pick-to-light for ESL Tag {}", tag.getMacAddress());
            return ResponseEntity.ok("Pick-to-light activated for " + tag.getMacAddress());
        }).orElseGet(() -> ResponseEntity.status(404).body("No assigned ESL tag found for this product."));
    }
}
