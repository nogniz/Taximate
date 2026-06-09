package com.taximate.backend.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.ArrayList;
import java.util.List;

@Entity // 💡 DB 테이블로 관리하겠다고 선언
@Data
@NoArgsConstructor // JPA 필수 기본 생성자
public class TaxiRoom {

    @Id // 💡 PK (기본키) 설정
    private String roomId;
    private String title;
    private String departure;
    private String destination;
    private String departureTime;
    private int maxParticipants;
    private String status;

    @ElementCollection(fetch = FetchType.EAGER) // 💡 값 타입 컬렉션 매핑 (userIds 리스트 저장용)
    private List<String> userIds = new ArrayList<>();

    public TaxiRoom(String roomId, String title, String departure, String destination, String departureTime, int maxParticipants) {
        this.roomId = roomId;
        this.title = title;
        this.departure = departure;
        this.destination = destination;
        this.departureTime = departureTime;
        this.maxParticipants = maxParticipants;
        this.status = "WAITING";
    }

    public synchronized boolean addUser(String userId) {
        if (userIds.size() < maxParticipants && !userIds.contains(userId)) {
            userIds.add(userId);
            if (userIds.size() == maxParticipants) {
                this.status = "FULL";
            }
            return true;
        }
        return false;
    }
}