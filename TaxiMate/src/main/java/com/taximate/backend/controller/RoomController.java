package com.taximate.backend.controller;

import com.taximate.backend.model.TaxiRoom;
import com.taximate.backend.service.TaxiRoomManager;
import com.taximate.backend.service.MatchEngine;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class RoomController {

    private final TaxiRoomManager roomManager;
    private final MatchEngine matchEngine; // 💡 매칭 엔진 주입

    // 1. 방 개설 API
    @PostMapping
    public ResponseEntity<TaxiRoom> createRoom(@RequestBody Map<String, Object> request) {
        TaxiRoom created = roomManager.createRoom(
                (String) request.get("title"),
                (String) request.get("departure"),
                (String) request.get("destination"),
                (String) request.get("departureTime"),
                (Integer) request.get("maxParticipants"),
                (String) request.get("userId")
        );
        // 매칭 엔진 풀에도 동기화
        //matchEngine.addRoomToEngine(created);
        return ResponseEntity.ok(created);
    }

    // 2. 전체 대기방 목록 조회 API
    @GetMapping
    public ResponseEntity<List<TaxiRoom>> getAllRooms() {
        return ResponseEntity.ok(roomManager.getAllRooms());
    }

    // 3. 실시간 최적의 방 자동 매칭 요청 API (POST /api/rooms/match)
    @PostMapping("/match")
    public ResponseEntity<?> autoMatch(@RequestBody Map<String, Object> request) {
        String userId = (String) request.get("userId");
        String title = (String) request.get("title");
        String departure = (String) request.get("departure");
        String destination = (String) request.get("destination");
        String departureTime = (String) request.get("departureTime");

        // 매칭 엔진 기동
        TaxiRoom matchedRoom = matchEngine.findAndJoinMatch(userId, title, departure, destination, departureTime);

        if (matchedRoom != null) {
            // 80% 이상 유사한 방이 있어서 자동 합류 성공한 경우
            return ResponseEntity.ok(Map.of("status", "MATCHED", "room", matchedRoom));
        } else {
            // 맞는 방이 없어서 설계서 요구사항대로 신규 방 생성 트리거 발동
            TaxiRoom newRoom = roomManager.createRoom(title, departure, destination, departureTime, 4, userId);
            //  matchEngine.addRoomToEngine(newRoom);
            return ResponseEntity.ok(Map.of("status", "CREATED", "room", newRoom));
        }
    }
}