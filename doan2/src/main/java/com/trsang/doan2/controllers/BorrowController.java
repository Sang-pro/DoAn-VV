package com.trsang.doan2.controllers;

import com.trsang.doan2.entities.Book;
import com.trsang.doan2.entities.BorrowRecord;
import com.trsang.doan2.dtos.requests.BorrowRequest;
import com.trsang.doan2.entities.User;
import com.trsang.doan2.exceptions.ResourceNotFoundException;
import com.trsang.doan2.exceptions.ServiceException;
import com.trsang.doan2.repositories.IBookRepository;
import com.trsang.doan2.repositories.IBorrowRecordRepository;
import com.trsang.doan2.repositories.IEslTagRepository;
import com.trsang.doan2.repositories.IUserRepository;
import com.trsang.doan2.services.interfaces.MqttGateway;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.text.Normalizer;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/borrow")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class BorrowController {

    private final IBorrowRecordRepository borrowRecordRepository;
    private final IBookRepository bookRepository;
    private final IUserRepository userRepository;
    private final IEslTagRepository eslTagRepository;
    private final MqttGateway mqttGateway;
    private final ObjectMapper objectMapper;

    @PostMapping("/borrow")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN', 'USER')")
    public ResponseEntity<BorrowRecord> borrowBook(@RequestBody BorrowRequest request) {
        log.info("Borrow book request received: bookId={}, userCode={}", request.getBookId(), request.getUserCode());

        Book book = bookRepository.findById(request.getBookId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sách với ID: " + request.getBookId()));

        User user = userRepository.findByUserCode(request.getUserCode())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng với mã số: " + request.getUserCode()));

        if (book.getAvailableCopies() <= 0) {
            throw new ServiceException("Sách này đã được mượn hết!");
        }

        // Check if user already borrowed this book and hasn't returned it yet
        borrowRecordRepository.findByBookIdAndUserIdAndStatus(book.getId(), user.getId(), "BORROWED")
                .ifPresent(r -> {
                    throw new ServiceException("Người dùng này đang mượn cuốn sách này rồi, không thể mượn thêm!");
                });

        // Decrement book available copies
        book.setAvailableCopies(book.getAvailableCopies() - 1);
        Book updatedBook = bookRepository.save(book);

        // Trigger ESL Tag update
        syncEslTag(updatedBook);

        // Create borrow record
        BorrowRecord record = BorrowRecord.builder()
                .book(updatedBook)
                .user(user)
                .borrowDate(Instant.now())
                .dueDate(Instant.now().plus(14, ChronoUnit.DAYS)) // Hạn trả mặc định 14 ngày
                .status("BORROWED")
                .build();

        BorrowRecord savedRecord = borrowRecordRepository.save(record);
        log.info("Book successfully borrowed: recordId={}", savedRecord.getId());
        return ResponseEntity.ok(savedRecord);
    }

    @PostMapping("/return")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public ResponseEntity<BorrowRecord> returnBook(@RequestBody BorrowRequest request) {
        log.info("Return book request received: bookId={}, userCode={}", request.getBookId(), request.getUserCode());

        Book book = bookRepository.findById(request.getBookId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sách với ID: " + request.getBookId()));

        User user = userRepository.findByUserCode(request.getUserCode())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng với mã số: " + request.getUserCode()));

        // Find active borrow record
        BorrowRecord record = borrowRecordRepository.findByBookIdAndUserIdAndStatus(book.getId(), user.getId(), "BORROWED")
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bản ghi mượn sách đang hoạt động cho người dùng và sách này!"));

        // Increment book available copies
        book.setAvailableCopies(book.getAvailableCopies() + 1);
        Book updatedBook = bookRepository.save(book);

        // Trigger ESL Tag update
        syncEslTag(updatedBook);

        // Update record
        record.setReturnDate(Instant.now());
        record.setStatus("RETURNED");

        BorrowRecord savedRecord = borrowRecordRepository.save(record);
        log.info("Book successfully returned: recordId={}", savedRecord.getId());
        return ResponseEntity.ok(savedRecord);
    }

    @GetMapping("/user/{userCode}")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN', 'USER')")
    public ResponseEntity<List<BorrowRecord>> getHistoryByUser(@PathVariable String userCode) {
        User user = userRepository.findByUserCode(userCode)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng với mã số: " + userCode));
        return ResponseEntity.ok(borrowRecordRepository.findByUserId(user.getId()));
    }

    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    public ResponseEntity<List<BorrowRecord>> getAllRecords() {
        return ResponseEntity.ok(borrowRecordRepository.findAll());
    }

    private void syncEslTag(Book updatedBook) {
        eslTagRepository.findByBookId(updatedBook.getId()).ifPresent(tag -> {
            try {
                // Tự động loại bỏ dấu Tiếng Việt
                String temp = Normalizer.normalize(updatedBook.getTitle(), Normalizer.Form.NFD);
                String normalizedName = temp.replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                                            .replaceAll("Đ", "D").replaceAll("đ", "d");

                String tempAuthor = Normalizer.normalize(updatedBook.getAuthor(), Normalizer.Form.NFD);
                String normalizedAuthor = tempAuthor.replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                                            .replaceAll("Đ", "D").replaceAll("đ", "d");

                Map<String, String> payloadMap = new HashMap<>();
                payloadMap.put("action", "update");
                payloadMap.put("name", normalizedName);
                payloadMap.put("author", normalizedAuthor);
                String payload = objectMapper.writeValueAsString(payloadMap);
                
                log.info("Publishing update to ESL Tag {} due to borrow/return: {}", tag.getMacAddress(), payload);
                mqttGateway.sendToMqtt("esl/update/" + tag.getMacAddress(), payload);
            } catch (Exception e) {
                log.error("Error creating MQTT payload for ESL Tag {}", tag.getMacAddress(), e);
            }
        });
    }
}
