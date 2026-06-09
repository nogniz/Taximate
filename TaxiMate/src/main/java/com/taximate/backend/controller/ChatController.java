package com.taximate.backend.controller;

import com.taximate.backend.model.ChatMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class ChatController {

    private final SimpMessageSendingOperations messagingTemplate;

    // 클라이언트가 /pub/chat/message/{roomId}로 메시지를 보내면 이 메서드가 실행됨
    @MessageMapping("/chat/message/{roomId}")
    public void sendMessage(@DestinationVariable String roomId, ChatMessage message) {
        // /sub/chat/room/{roomId}를 구독하고 있는 다른 동승자들에게 실시간으로 메시지 전송
        messagingTemplate.convertAndSend("/sub/chat/room/" + roomId, message);
    }
}