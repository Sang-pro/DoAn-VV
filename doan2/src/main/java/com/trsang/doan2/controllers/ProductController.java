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

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*") // In modern spring this might be overridden, but matches standard template
public class ProductController {

    private final IProductRepository productRepository;
    private final IEslTagRepository eslTagRepository;
    private final EslMqttGateway eslMqttGateway;

    @GetMapping
    public List<Product> getAllProducts() {
        return productRepository.findAll();
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
                String payload = String.format("{\"price\": %.2f, \"stock\": %d, \"qr\": \"%s\"}", 
                        updatedProduct.getPrice(), 
                        updatedProduct.getStockQuantity(), 
                        updatedProduct.getQrCodeUrl() != null ? updatedProduct.getQrCodeUrl() : "");
                
                log.info("Publishing update to ESL Tag {}: {}", tag.getMacAddress(), payload);
                eslMqttGateway.sendToMqtt("esl/update/" + tag.getMacAddress(), payload);
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
