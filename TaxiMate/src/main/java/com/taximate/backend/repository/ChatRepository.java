package com.taximate.backend.repository;

import com.taximate.backend.model.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

public interface ChatRepository extends JpaRepository<ChatMessage, Long> {

    /** 특정 채팅방 전체 메시지 (시간순) */
    List<ChatMessage> findByChatIdOrderBySentAtAsc(String chatId);

    /** 특정 채팅방 최신 메시지 1건 */
    Optional<ChatMessage> findFirstByChatIdOrderBySentAtDesc(String chatId);

    /** 유저가 참여한 DM chatId 목록 */
    @Query("SELECT DISTINCT m.chatId FROM ChatMessage m WHERE m.type = 'DM'")
    List<String> findAllDmChatIds();
}
