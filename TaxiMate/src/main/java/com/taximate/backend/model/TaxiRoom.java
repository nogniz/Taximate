package com.taximate.backend.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.*;

@Entity
@Data
@NoArgsConstructor
public class TaxiRoom {

    @Id
    private String roomId;
    private String title;
    private String departure;
    private String destination;
    private String departureTime;
    private int maxParticipants;
    private String status;

    @ElementCollection(fetch = FetchType.EAGER)
    private List<String> userIds = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_destinations", joinColumns = @JoinColumn(name = "room_id"))
    @MapKeyColumn(name = "user_id")
    @Column(name = "destination")
    private Map<String, String> userDestinations = new HashMap<>();

    /** 탑승완료 누른 유저 목록 */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "boarded_users", joinColumns = @JoinColumn(name = "room_id"))
    @Column(name = "user_id")
    private Set<String> boardedUsers = new HashSet<>();

    public TaxiRoom(String roomId, String title, String departure, String destination,
                    String departureTime, int maxParticipants) {
        this.roomId = roomId;
        this.title = title;
        this.departure = departure;
        this.destination = destination;
        this.departureTime = departureTime;
        this.maxParticipants = maxParticipants;
        this.status = "WAITING";
    }

    public synchronized boolean addUser(String userId) {
        return addUser(userId, null);
    }

    public synchronized boolean addUser(String userId, String personalDestination) {
        if (userIds.size() < maxParticipants && !userIds.contains(userId)) {
            userIds.add(userId);
            userDestinations.put(userId,
                    (personalDestination != null && !personalDestination.isBlank())
                            ? personalDestination : this.destination);
            if (userIds.size() == maxParticipants) this.status = "FULL";
            return true;
        }
        return false;
    }

    /** 탑승완료 처리 */
    public boolean boardUser(String userId) {
        if (!userIds.contains(userId)) return false;
        return boardedUsers.add(userId);
    }

    /** 전원 탑승 여부 */
    public boolean isAllBoarded() {
        return !userIds.isEmpty() && boardedUsers.containsAll(userIds);
    }

    /** 강퇴 (방장만 호출) */
    public boolean kickUser(String targetId) {
        if (!userIds.contains(targetId)) return false;
        userIds.remove(targetId);
        userDestinations.remove(targetId);
        boardedUsers.remove(targetId);
        // FULL이었으면 다시 WAITING
        if ("FULL".equals(status)) status = "WAITING";
        return true;
    }
}
