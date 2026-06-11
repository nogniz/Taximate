package com.taximate.backend.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.HashMap;

@Entity
@Table(name = "payments")
@Data
@NoArgsConstructor
public class Payment {

    @Id
    private String roomId; // 방 ID = 정산 ID (1방 1정산)

    private int totalFare;       // 총 택시비
    private int farePerPerson;   // 1인당 금액
    private int headCount;       // 정산 인원 수

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "payment_status", joinColumns = @JoinColumn(name = "room_id"))
    @MapKeyColumn(name = "user_id")
    @Column(name = "is_paid")
    private Map<String, Boolean> paidStatus = new HashMap<>();

    // 거리 비율 기반 개인별 금액 (userId → 금액)
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "payment_split", joinColumns = @JoinColumn(name = "room_id"))
    @MapKeyColumn(name = "user_id")
    @Column(name = "fare_amount")
    private Map<String, Integer> fareSplit = new HashMap<>();

    public Payment(String roomId, int totalFare, java.util.List<String> userIds) {
        this.roomId = roomId;
        this.totalFare = totalFare;
        this.headCount = userIds.size();
        this.farePerPerson = (int) Math.ceil((double) totalFare / headCount);
        for (String userId : userIds) {
            this.paidStatus.put(userId, false);
            this.fareSplit.put(userId, this.farePerPerson); // 기본값: 균등 분배
        }
    }

    // 거리 비율 기반 정산용 생성자
    public Payment(String roomId, int totalFare, java.util.List<String> userIds, Map<String, Integer> fareSplit) {
        this.roomId = roomId;
        this.totalFare = totalFare;
        this.headCount = userIds.size();
        this.farePerPerson = userIds.isEmpty() ? 0 : (int) Math.ceil((double) totalFare / userIds.size());
        this.fareSplit = fareSplit;
        for (String userId : userIds) {
            this.paidStatus.put(userId, false);
        }
    }
}
