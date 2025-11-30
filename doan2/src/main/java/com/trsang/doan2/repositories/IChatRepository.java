package com.trsang.doan2.repositories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.trsang.doan2.entities.Chat;
import com.trsang.doan2.entities.User;

@Repository
public interface IChatRepository extends JpaRepository<Chat, UUID> {
    List<Chat> findByUserOrderByUpdatedAtDesc(User user);

    Page<Chat> findByUser(User user, Pageable pageable);

    Optional<Chat> findByIdAndUser(UUID id, User user);

    Page<Chat> findByUserId(UUID id, Pageable pageable);

    @Query("SELECT c FROM Chat c WHERE c.user.id = :userId")
    Integer countByUserId(@Param("userId") UUID userId);
    
}
