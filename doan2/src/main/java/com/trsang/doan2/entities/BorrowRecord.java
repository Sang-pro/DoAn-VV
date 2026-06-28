package com.trsang.doan2.entities;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Data
@Builder
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "borrow_records")
public class BorrowRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "borrow_date", nullable = false, columnDefinition = "TIMESTAMP")
    private Instant borrowDate;

    @Column(name = "due_date", nullable = false, columnDefinition = "TIMESTAMP")
    private Instant dueDate;

    @Column(name = "return_date", columnDefinition = "TIMESTAMP")
    private Instant returnDate;

    @Column(name = "status", nullable = false, length = 20)
    private String status; // BORROWED, RETURNED, OVERDUE
}
