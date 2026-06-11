package com.taximate.backend.service;

import com.taximate.backend.model.TaxiRoom;
import com.taximate.backend.repository.TaxiRoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional // 💡 데이터 정합성을 위한 트랜잭션 보장
public class TaxiRoomManager {

    private final TaxiRoomRepository roomRepository; // 💡 DB 주입
    private final NotificationService notificationService;

    // 1. 새로운 동승 방 생성 및 DB 저장
    public TaxiRoom createRoom(String title, String departure, String destination, String departureTime, int maxParticipants, String creatorId) {
        String roomId = UUID.randomUUID().toString().substring(0, 8);
        TaxiRoom newRoom = new TaxiRoom(roomId, title, departure, destination, departureTime, maxParticipants);

        newRoom.addUser(creatorId, destination); // 방장의 목적지 = 방의 공통 목적지

        // 💡 메모리가 아닌 실제 DB 테이블에 Insert 쿼리 날림
        return roomRepository.save(newRoom);
    }

    // 2. 전체 대기방 목록 조회 (DB에서 Select All)
    @Transactional(readOnly = true)
    public List<TaxiRoom> getAllRooms() {
        return roomRepository.findAll();
    }

    // 3. 특정 방 조회
    @Transactional(readOnly = true)
    public TaxiRoom getRoom(String roomId) {
        return roomRepository.findById(roomId).orElse(null);
    }

    // 4. 기존 방에 멤버 합류 및 DB 업데이트
    public boolean joinRoom(String roomId, String userId) {
        return joinRoom(roomId, userId, null);
    }

    public boolean joinRoom(String roomId, String userId, String personalDestination) {
        TaxiRoom room = roomRepository.findById(roomId).orElse(null);
        if (room != null) {
            boolean isAdded = room.addUser(userId, personalDestination);
            if (isAdded) {
                roomRepository.save(room);
                return true;
            }
        }
        return false;
    }

    // 5. 방 삭제
    public boolean deleteRoom(String roomId) {
        if (roomRepository.existsById(roomId)) {
            roomRepository.deleteById(roomId);
            return true;
        }
        return false;
    }

    // 6. 탑승완료 표시 (모든 멤버 가능)
    public TaxiRoom boardUser(String roomId, String userId) {
        TaxiRoom room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 방입니다."));
        if (!room.getUserIds().contains(userId))
            throw new IllegalStateException("방 참여자가 아닙니다.");
        room.boardUser(userId);
        TaxiRoom saved = roomRepository.save(room);
        int cnt = saved.getBoardedUsers().size();
        int total = saved.getUserIds().size();
        notificationService.notify(roomId, "BOARDED",
                userId + "님 탑승완료 (" + cnt + "/" + total + ")");
        return saved;
    }

    // 7. 출발 (방장만, 전원 탑승완료 시에만 가능) → COMPLETED
    public TaxiRoom departRoom(String roomId, String requesterId) {
        TaxiRoom room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 방입니다."));
        if (!room.getUserIds().get(0).equals(requesterId))
            throw new IllegalStateException("방장만 출발할 수 있습니다.");
        if (!room.isAllBoarded())
            throw new IllegalStateException("아직 탑승하지 않은 멤버가 있습니다.");
        if ("COMPLETED".equals(room.getStatus()) || "CANCELLED".equals(room.getStatus()))
            throw new IllegalStateException("이미 종료된 방입니다.");
        room.setStatus("COMPLETED");
        TaxiRoom saved = roomRepository.save(room);
        notificationService.notify(roomId, "DEPARTED", "출발했습니다! 안전하게 도착하세요 🚕");
        return saved;
    }

    // 8. 강퇴 (방장만 가능)
    public TaxiRoom kickUser(String roomId, String hostId, String targetId) {
        TaxiRoom room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 방입니다."));
        if (!room.getUserIds().get(0).equals(hostId))
            throw new IllegalStateException("방장만 강퇴할 수 있습니다.");
        if (hostId.equals(targetId))
            throw new IllegalStateException("자기 자신은 강퇴할 수 없습니다.");
        if (!room.kickUser(targetId))
            throw new IllegalArgumentException("해당 멤버가 방에 없습니다.");
        TaxiRoom saved = roomRepository.save(room);
        notificationService.notify(roomId, "KICKED", targetId + "님이 강퇴되었습니다.");
        return saved;
    }

    // 9. 탑승 완료 (WAITING/FULL → COMPLETED) - 기존 호환용
    public TaxiRoom completeRoom(String roomId, String requesterId) {
        return departRoom(roomId, requesterId);
    }

    // 7. 방 취소 (WAITING/FULL → CANCELLED, 방장만 가능)
    public TaxiRoom cancelRoom(String roomId, String requesterId) {
        TaxiRoom room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 방입니다."));

        // 방장 = userIds의 첫 번째 유저
        if (!room.getUserIds().get(0).equals(requesterId)) {
            throw new IllegalStateException("방장만 방을 취소할 수 있습니다.");
        }
        if ("COMPLETED".equals(room.getStatus()) || "CANCELLED".equals(room.getStatus())) {
            throw new IllegalStateException("이미 종료된 방입니다.");
        }

        room.setStatus("CANCELLED");
        TaxiRoom saved = roomRepository.save(room);
        notificationService.notify(roomId, "CANCELLED", "방장이 방을 취소했습니다.");
        return saved;
    }
}