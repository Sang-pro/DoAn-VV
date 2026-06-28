package com.trsang.doan2.repositories;

import com.trsang.doan2.entities.BorrowRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface IBorrowRecordRepository extends JpaRepository<BorrowRecord, Long> {
    List<BorrowRecord> findByUserId(UUID userId);
    List<BorrowRecord> findByBookId(Long bookId);
    List<BorrowRecord> findByStatus(String status);
    
    // Find active borrow record for a user and book
    Optional<BorrowRecord> findByBookIdAndUserIdAndStatus(Long bookId, UUID userId, String status);

    // Check if a book is currently borrowed (any user)
    Optional<BorrowRecord> findByBookIdAndStatus(Long bookId, String status);
}
