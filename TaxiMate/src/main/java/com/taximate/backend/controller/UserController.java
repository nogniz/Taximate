package com.taximate.backend.controller;

import com.taximate.backend.model.User;
import com.taximate.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class UserController {

    private final UserService userService;

    // 1. 회원가입 (POST /api/users/register)
    // 사전 조건: /api/auth/email → /api/auth/verify 완료 후 호출
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> request) {
        String studentId = request.get("studentId");
        String name      = request.get("name");
        String email     = request.get("email");
        String password  = request.get("password");

        try {
            User user = userService.register(studentId, name, email, password);
            return ResponseEntity.ok(Map.of(
                    "status", "SUCCESS",
                    "message", "회원가입이 완료되었습니다.",
                    "studentId", user.getStudentId(),
                    "name", user.getName()
            ));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(403).body(Map.of("status", "ERROR", "message", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("status", "ERROR", "message", e.getMessage()));
        }
    }

    // 2. 로그인 (POST /api/users/login)
    // 응답: { "token": "eyJ..." }
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> request) {
        String email    = request.get("email");
        String password = request.get("password");

        try {
            String token = userService.login(email, password);
            return ResponseEntity.ok(Map.of(
                    "status", "SUCCESS",
                    "token", token
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(401).body(Map.of("status", "ERROR", "message", e.getMessage()));
        }
    }

    // 3. 내 프로필 조회 (GET /api/users/me)
    // Header: Authorization: Bearer <token>
    @GetMapping("/me")
    public ResponseEntity<?> getProfile(Authentication auth) {
        String studentId = (String) auth.getPrincipal();
        try {
            Map<String, Object> profile = userService.getProfile(studentId);
            return ResponseEntity.ok(profile);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("status", "ERROR", "message", e.getMessage()));
        }
    }

    // 4. 매너 온도 갱신 (PATCH /api/users/manner) - 내부용
    @PatchMapping("/manner")
    public ResponseEntity<?> updateManner(@RequestBody Map<String, Float> request, Authentication auth) {
        String studentId = (String) auth.getPrincipal();
        float score = request.getOrDefault("score", 0f);

        try {
            float newTemp = userService.updateManner(studentId, score);
            return ResponseEntity.ok(Map.of(
                    "status", "SUCCESS",
                    "mannerTemp", newTemp
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("status", "ERROR", "message", e.getMessage()));
        }
    }

    // 5. 상대방 평가 (POST /api/users/{targetId}/rate)
    // 동승 완료 후 다른 멤버를 평가 - 평가자(rater)가 대상(target)의 온도를 변경
    // Header: Authorization: Bearer <token>
    // Body: { "score": 0.5 }  (양수=좋아요 +0.5, 음수=싫어요 -0.3)
    @PostMapping("/{targetId}/rate")
    public ResponseEntity<?> rateUser(@PathVariable String targetId,
                                      @RequestBody Map<String, Float> request,
                                      Authentication auth) {
        String raterId = (String) auth.getPrincipal();
        if (raterId.equals(targetId)) {
            return ResponseEntity.badRequest().body(Map.of("status", "ERROR", "message", "자기 자신은 평가할 수 없습니다."));
        }
        float score = request.getOrDefault("score", 0f);
        try {
            float newTemp = userService.updateManner(targetId, score);
            return ResponseEntity.ok(Map.of(
                    "status", "SUCCESS",
                    "targetId", targetId,
                    "mannerTemp", newTemp
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("status", "ERROR", "message", e.getMessage()));
        }
    }
}
