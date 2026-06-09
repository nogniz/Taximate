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

    // 1. 새로운 동승 방 생성 및 DB 저장
    public TaxiRoom createRoom(String title, String departure, String destination, String departureTime, int maxParticipants, String creatorId) {
        String roomId = UUID.randomUUID().toString().substring(0, 8);
        TaxiRoom newRoom = new TaxiRoom(roomId, title, departure, destination, departureTime, maxParticipants);

        newRoom.addUser(creatorId);

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
        TaxiRoom room = roomRepository.findById(roomId).orElse(null);
        if (room != null) {
            boolean isAdded = room.addUser(userId);
            if (isAdded) {
                roomRepository.save(room); // 변경 사항 DB 갱신 (Update)
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
}