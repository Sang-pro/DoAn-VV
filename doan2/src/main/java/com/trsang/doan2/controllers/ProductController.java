package com.trsang.doan2.controllers;

import com.trsang.doan2.entities.Product;
import com.trsang.doan2.repositories.IProductRepository;
import com.trsang.doan2.repositories.IEslTagRepository;
import com.trsang.doan2.services.interfaces.EslMqttGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.HashMap;
import java.text.Normalizer;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*") // In modern spring this might be overridden, but matches standard template
public class ProductController {

    private final IProductRepository productRepository;
    private final IEslTagRepository eslTagRepository;
    private final EslMqttGateway eslMqttGateway;
    private final ObjectMapper objectMapper;

    @GetMapping
    public Page<Product> getAllProducts(@PageableDefault(size = 20) Pageable pageable) {
        return productRepository.findAll(pageable);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Product createProduct(@RequestBody Product product) {
        return productRepository.save(product);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CASHIER')")
    public ResponseEntity<Product> updateProduct(@PathVariable Long id, @RequestBody Product productDetails) {
        return productRepository.findById(id).map(product -> {
            product.setName(productDetails.getName());
            product.setPrice(productDetails.getPrice());
            product.setStockQuantity(productDetails.getStockQuantity());
            product.setDescription(productDetails.getDescription());
            product.setQrCodeUrl(productDetails.getQrCodeUrl());
            
            Product updatedProduct = productRepository.save(product);

            // If product is linked to an ESL tag, push MQTT update
            // ESL tag has a reference to product, so we look it up
            eslTagRepository.findByProductId(updatedProduct.getId()).ifPresent(tag -> {
                // Tự động loại bỏ dấu Tiếng Việt (Ví dụ: "Mỳ tôm" -> "My tom")
                String temp = Normalizer.normalize(updatedProduct.getName(), Normalizer.Form.NFD);
                String normalizedName = temp.replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                                            .replaceAll("Đ", "D").replaceAll("đ", "d");
                                            
                // Định dạng giá tiền (Ví dụ: 25000 -> 25,000)
                String formattedPrice = String.format("%,.0f", updatedProduct.getPrice());

                try {
                    Map<String, String> payloadMap = new HashMap<>();
                    payloadMap.put("action", "update");
                    payloadMap.put("name", normalizedName);
                    payloadMap.put("price", formattedPrice);
                    String payload = objectMapper.writeValueAsString(payloadMap);
                    
                    log.info("Publishing update to ESL Tag {}: {}", tag.getMacAddress(), payload);
                    eslMqttGateway.sendToMqtt("esl/update/" + tag.getMacAddress(), payload);
                } catch (Exception e) {
                    log.error("Error creating MQTT payload for ESL Tag {}", tag.getMacAddress(), e);
                }
            });

            return ResponseEntity.ok(updatedProduct);
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        productRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }
}
