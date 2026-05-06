package com.trsang.doan2.services.implementation;

import com.trsang.doan2.dtos.requests.RfidScanRequest;
import com.trsang.doan2.entities.EslTag;
import com.trsang.doan2.entities.Product;
import com.trsang.doan2.entities.RfidTag;
import com.trsang.doan2.repositories.IEslTagRepository;
import com.trsang.doan2.repositories.IProductRepository;
import com.trsang.doan2.repositories.IRfidTagRepository;
import com.trsang.doan2.services.interfaces.IRfidService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class RfidService implements IRfidService {

    private final IRfidTagRepository rfidTagRepository;
    private final IEslTagRepository eslTagRepository;
    private final IProductRepository productRepository;

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

        Product scannedProduct = rfidTag.getProduct();

        // Find the shelf (EslTag) by location
        Optional<EslTag> eslTagOpt = eslTagRepository.findByLocation(request.getLocation());
        if (eslTagOpt.isPresent()) {
            EslTag eslTag = eslTagOpt.get();
            // Optional: If there was a previous product on this shelf, maybe its status should change?
            eslTag.setProduct(scannedProduct);
            eslTag.setLastSeen(Instant.now());
            eslTagRepository.save(eslTag);
            log.info("Updated Shelf {} with Product ID {}", request.getLocation(), scannedProduct.getId());
        } else {
            log.warn("Shelf location {} not found in EslTag database.", request.getLocation());
        }
    }

    @Override
    public RfidTag registerRfidTag(String epc, Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        RfidTag rfidTag = RfidTag.builder()
                .epc(epc)
                .product(product)
                .build();
        return rfidTagRepository.save(rfidTag);
    }
}
