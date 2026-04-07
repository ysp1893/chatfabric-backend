package com.chatfabric.chat.repository;

import com.chatfabric.chat.entity.ChatParticipant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatParticipantRepository extends JpaRepository<ChatParticipant, Long> {

    List<ChatParticipant> findByChatId(Long chatId);

    boolean existsByChatIdAndUserId(Long chatId, Long userId);
}
