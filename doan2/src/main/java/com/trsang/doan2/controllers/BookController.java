package com.trsang.doan2.controllers;

import com.trsang.doan2.entities.Book;
import com.trsang.doan2.repositories.IBookRepository;
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
@RequestMapping("/api/books")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*") // In modern spring this might be overridden, but matches standard template
public class BookController {

    private final IBookRepository bookRepository;
    private final IEslTagRepository eslTagRepository;
    private final EslMqttGateway eslMqttGateway;
    private final ObjectMapper objectMapper;

    @GetMapping
    public Page<Book> getAllBooks(@PageableDefault(size = 20) Pageable pageable) {
        return bookRepository.findAll(pageable);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Book createBook(@RequestBody Book book) {
        return bookRepository.save(book);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public ResponseEntity<Book> updateBook(@PathVariable Long id, @RequestBody Book bookDetails) {
        return bookRepository.findById(id).map(book -> {
            book.setTitle(bookDetails.getTitle());
            book.setAuthor(bookDetails.getAuthor());
            book.setAvailableCopies(bookDetails.getAvailableCopies());
            book.setSummary(bookDetails.getSummary());
            book.setCoverImageUrl(bookDetails.getCoverImageUrl());
            
            Book updatedBook = bookRepository.save(book);

            // If book is linked to an ESL tag, push MQTT update
            eslTagRepository.findByBookId(updatedBook.getId()).ifPresent(tag -> {
                // Tự động loại bỏ dấu Tiếng Việt
                String temp = Normalizer.normalize(updatedBook.getTitle(), Normalizer.Form.NFD);
                String normalizedName = temp.replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                                            .replaceAll("Đ", "D").replaceAll("đ", "d");

                // Bỏ dấu cho tác giả luôn để hiển thị OLED không bị lỗi
                String tempAuthor = Normalizer.normalize(updatedBook.getAuthor(), Normalizer.Form.NFD);
                String normalizedAuthor = tempAuthor.replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                                            .replaceAll("Đ", "D").replaceAll("đ", "d");

                try {
                    Map<String, String> payloadMap = new HashMap<>();
                    payloadMap.put("action", "update");
                    payloadMap.put("name", normalizedName);
                    payloadMap.put("author", normalizedAuthor); // Gửi tác giả thay vì giá
                    String payload = objectMapper.writeValueAsString(payloadMap);
                    
                    log.info("Publishing update to ESL Tag {}: {}", tag.getMacAddress(), payload);
                    eslMqttGateway.sendToMqtt("esl/update/" + tag.getMacAddress(), payload);
                } catch (Exception e) {
                    log.error("Error creating MQTT payload for ESL Tag {}", tag.getMacAddress(), e);
                }
            });

            return ResponseEntity.ok(updatedBook);
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteBook(@PathVariable Long id) {
        bookRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }
}
