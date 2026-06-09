package com.taximate.backend.model;

import lombok.Data;

@Data
public class ChatMessage {
    private String roomId;    // 동승 방 ID
    private String senderId;  // 보내는 사람 (학번 또는 유저 ID)
    private String message;   // 채팅 메시지 내용
    private String timestamp; // 보낸 시간
}