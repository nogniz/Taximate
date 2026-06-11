package com.taximate.backend.controller;

import com.taximate.backend.model.TaxiRoom;
import com.taximate.backend.service.TaxiRoomManager;
import com.taximate.backend.service.MatchEngine;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
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

    // 2-1. 내 이용 내역 (SETTLED 상태이고 내가 멤버인 방)
    @GetMapping("/history")
    public ResponseEntity<List<TaxiRoom>> getMyHistory(Authentication auth) {
        String studentId = (String) auth.getPrincipal();
        List<TaxiRoom> history = roomManager.getAllRooms().stream()
                .filter(r -> "SETTLED".equals(r.getStatus()) && r.getUserIds().contains(studentId))
                .collect(java.util.stream.Collectors.toList());
        return ResponseEntity.ok(history);
    }

    // 3. 특정 방 단건 조회 API (GET /api/rooms/{roomId})
    @GetMapping("/{roomId}")
    public ResponseEntity<?> getRoom(@PathVariable String roomId) {
        TaxiRoom room = roomManager.getRoom(roomId);
        if (room == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(room);
    }

    // 3-1. 방 직접 참여 API (POST /api/rooms/{roomId}/join)
    // Body(optional): { "destination": "반월당역" }
    @PostMapping("/{roomId}/join")
    public ResponseEntity<?> joinRoom(@PathVariable String roomId,
                                      @RequestBody(required = false) Map<String, String> body,
                                      Authentication auth) {
        String studentId = (String) auth.getPrincipal();
        String destination = (body != null) ? body.getOrDefault("destination", null) : null;
        boolean joined = roomManager.joinRoom(roomId, studentId, destination);
        if (joined) {
            TaxiRoom room = roomManager.getRoom(roomId);
            return ResponseEntity.ok(Map.of("status", "SUCCESS", "room", room));
        } else {
            return ResponseEntity.badRequest().body(Map.of("status", "ERROR", "message", "참여할 수 없습니다. (만석이거나 이미 참여 중)"));
        }
    }

    // 4. 탑승 완료 처리 API (PATCH /api/rooms/{roomId}/complete)
    // Header: Authorization: Bearer <token>
    @PatchMapping("/{roomId}/complete")
    public ResponseEntity<?> completeRoom(@PathVariable String roomId, Authentication auth) {
        String studentId = (String) auth.getPrincipal();
        try {
            TaxiRoom room = roomManager.completeRoom(roomId, studentId);
            return ResponseEntity.ok(Map.of("status", "SUCCESS", "room", room));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("status", "ERROR", "message", e.getMessage()));
        }
    }

    // 5. 방 취소 API (PATCH /api/rooms/{roomId}/cancel) - 방장만 가능
    // Header: Authorization: Bearer <token>
    @PatchMapping("/{roomId}/cancel")
    public ResponseEntity<?> cancelRoom(@PathVariable String roomId, Authentication auth) {
        String studentId = (String) auth.getPrincipal();
        try {
            TaxiRoom room = roomManager.cancelRoom(roomId, studentId);
            return ResponseEntity.ok(Map.of("status", "SUCCESS", "room", room));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("status", "ERROR", "message", e.getMessage()));
        }
    }

    // 6a. 탑승완료 표시 (모든 멤버 가능)
    @PostMapping("/{roomId}/board")
    public ResponseEntity<?> boardRoom(@PathVariable String roomId, Authentication auth) {
        String studentId = (String) auth.getPrincipal();
        try {
            TaxiRoom room = roomManager.boardUser(roomId, studentId);
            return ResponseEntity.ok(Map.of(
                    "status", "SUCCESS",
                    "boardedCount", room.getBoardedUsers().size(),
                    "totalCount", room.getUserIds().size(),
                    "allBoarded", room.isAllBoarded(),
                    "room", room));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("status", "ERROR", "message", e.getMessage()));
        }
    }

    // 6b. 출발 (방장만, 전원 탑승완료 시에만)
    @PatchMapping("/{roomId}/depart")
    public ResponseEntity<?> departRoom(@PathVariable String roomId, Authentication auth) {
        String studentId = (String) auth.getPrincipal();
        try {
            TaxiRoom room = roomManager.departRoom(roomId, studentId);
            return ResponseEntity.ok(Map.of("status", "SUCCESS", "room", room));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("status", "ERROR", "message", e.getMessage()));
        }
    }

    // 6c. 강퇴 (방장만 가능)
    @DeleteMapping("/{roomId}/kick/{targetId}")
    public ResponseEntity<?> kickUser(@PathVariable String roomId,
                                      @PathVariable String targetId,
                                      Authentication auth) {
        String studentId = (String) auth.getPrincipal();
        try {
            TaxiRoom room = roomManager.kickUser(roomId, studentId, targetId);
            return ResponseEntity.ok(Map.of("status", "SUCCESS", "room", room));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("status", "ERROR", "message", e.getMessage()));
        }
    }

    // 6. 실시간 최적의 방 자동 매칭 요청 API (POST /api/rooms/match)
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