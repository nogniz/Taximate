package com.taximate.backend.service;

import com.taximate.backend.model.NotificationMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final SimpMessageSendingOperations messagingTemplate;

    // 특정 방의 구독자 전원에게 알림 전송
    // 클라이언트는 /sub/notification/{roomId} 를 구독하면 수신됨
    public void notify(String roomId, String type, String message) {
        NotificationMessage notification = new NotificationMessage(type, roomId, message);
        messagingTemplate.convertAndSend("/sub/notification/" + roomId, notification);
    }
}
