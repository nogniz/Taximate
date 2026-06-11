package com.taximate.backend.controller;

import com.taximate.backend.model.ChatMessage;
import com.taximate.backend.model.User;
import com.taximate.backend.repository.ChatRepository;
import com.taximate.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ChatController {

    private final ChatRepository chatRepository;
    private final UserRepository userRepository;
    private final SimpMessageSendingOperations messagingTemplate;

    // ══════════════════════════════════════════
    //  STOMP — 실시간 메시지 수신 + 저장 + 브로드캐스트
    // ══════════════════════════════════════════

    /** 방 채팅: /pub/chat/room/{roomId} */
    @MessageMapping("/chat/room/{roomId}")
    public void sendRoomMessage(@DestinationVariable String roomId,
                                @Payload Map<String, String> payload) {
        String senderId = payload.getOrDefault("senderId", "unknown");
        String content  = payload.getOrDefault("content", "");
        String name     = resolveName(senderId);

        ChatMessage msg = chatRepository.save(
                new ChatMessage(roomId, senderId, name, content, "ROOM"));
        messagingTemplate.convertAndSend("/sub/chat/room/" + roomId, msg);
    }

    /** DM: /pub/chat/dm/{chatId} */
    @MessageMapping("/chat/dm/{chatId}")
    public void sendDmMessage(@DestinationVariable String chatId,
                              @Payload Map<String, String> payload) {
        String senderId = payload.getOrDefault("senderId", "unknown");
        String content  = payload.getOrDefault("content", "");
        String name     = resolveName(senderId);

        ChatMessage msg = chatRepository.save(
                new ChatMessage(chatId, senderId, name, content, "DM"));
        messagingTemplate.convertAndSend("/sub/chat/dm/" + chatId, msg);
    }

    // ══════════════════════════════════════════
    //  REST — 채팅 기록 조회
    // ══════════════════════════════════════════

    /** 방 채팅 기록 조회 */
    @GetMapping("/api/chat/room/{roomId}")
    @ResponseBody
    public ResponseEntity<List<ChatMessage>> getRoomHistory(@PathVariable String roomId) {
        return ResponseEntity.ok(chatRepository.findByChatIdOrderBySentAtAsc(roomId));
    }

    /** DM 기록 조회 */
    @GetMapping("/api/chat/dm/{chatId}")
    @ResponseBody
    public ResponseEntity<List<ChatMessage>> getDmHistory(@PathVariable String chatId) {
        return ResponseEntity.ok(chatRepository.findByChatIdOrderBySentAtAsc(chatId));
    }

    /** DM chatId 생성/조회 (나 ↔ 상대방) */
    @GetMapping("/api/chat/dm-id/{targetId}")
    @ResponseBody
    public ResponseEntity<Map<String, String>> getDmChatId(@PathVariable String targetId,
                                                            Authentication auth) {
        String myId   = (String) auth.getPrincipal();
        String chatId = ChatMessage.dmChatId(myId, targetId);
        return ResponseEntity.ok(Map.of("chatId", chatId, "targetId", targetId));
    }

    /** 내 DM 대화 목록 (최신 메시지 포함) */
    @GetMapping("/api/chat/dms")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getMyDms(Authentication auth) {
        String myId = (String) auth.getPrincipal();

        List<String> allIds = chatRepository.findAllDmChatIds();
        List<String> myIds  = allIds.stream()
                .filter(id -> Arrays.asList(id.split("-")).contains(myId))
                .collect(Collectors.toList());

        List<Map<String, Object>> result = myIds.stream().map(chatId -> {
            String[] parts   = chatId.split("-");
            String otherId   = parts[0].equals(myId) ? parts[1] : parts[0];
            String otherName = resolveName(otherId);

            Map<String, Object> item = new HashMap<>();
            item.put("chatId",   chatId);
            item.put("otherId",  otherId);
            item.put("otherName", otherName);
            chatRepository.findFirstByChatIdOrderBySentAtDesc(chatId).ifPresent(m -> {
                item.put("lastMessage", m.getContent());
                item.put("lastTime",    m.getSentAt().toString());
            });
            return item;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    // ── 학번으로 이름 조회 헬퍼 ──────────────────────
    private String resolveName(String studentId) {
        return userRepository.findById(studentId)
                .map(User::getName)
                .orElse(studentId);
    }
}
