package com.taximate.backend.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "chat_messages")
@Data
@NoArgsConstructor
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 방 채팅: roomId / DM: "uid1-uid2" (두 ID 정렬 후 하이픈 연결) */
    private String chatId;

    private String senderId;   // 학번
    private String senderName; // 이름

    @Column(length = 1000)
    private String content;

    private String type;       // "ROOM" | "DM"
    private LocalDateTime sentAt;

    public ChatMessage(String chatId, String senderId, String senderName, String content, String type) {
        this.chatId    = chatId;
        this.senderId  = senderId;
        this.senderName = senderName;
        this.content   = content;
        this.type      = type;
        this.sentAt    = LocalDateTime.now();
    }

    /** DM chatId: 두 userId를 알파벳 정렬 후 "-" 로 연결 (일관성 보장) */
    public static String dmChatId(String uid1, String uid2) {
        return java.util.Arrays.asList(uid1, uid2).stream()
                .sorted()
                .collect(java.util.stream.Collectors.joining("-"));
    }
}
