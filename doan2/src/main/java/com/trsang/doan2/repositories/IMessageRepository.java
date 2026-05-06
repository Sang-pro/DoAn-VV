package com.trsang.doan2.repositories;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.trsang.doan2.entities.Chat;
import com.trsang.doan2.entities.Message;
import com.trsang.doan2.entities.Message.MessageRole;

@Repository
public interface IMessageRepository extends JpaRepository<Message, UUID> {
    List<Message> findByChatOrderByCreatedAtAsc(Chat chat);

    List<Message> findByChatOrderByCreatedAtDesc(Chat chat, Pageable pageable);

    Page<Message> findByChat(Chat chat, Pageable pageable);

    Long countByChat(Chat chat);

    List<Message> findByChatAndRole(Chat chat, MessageRole role);

    List<Message> findByChatIdOrderByCreatedAtAsc(UUID chatId);

    @Query("SELECT COUNT(m) FROM Message m JOIN m.chat c WHERE c.user.id = :userId")
    Integer countByUser (@Param("userId") UUID userId);
}
