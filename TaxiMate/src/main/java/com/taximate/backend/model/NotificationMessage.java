package com.taximate.backend.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class NotificationMessage {
    private String type;    // MATCHED, FULL, COMPLETED, CANCELLED
    private String roomId;
    private String message; // 사람이 읽을 수 있는 알림 메시지
}
