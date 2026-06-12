package com.taximate.backend.controller;

import com.taximate.backend.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // 브라우저 테스트 허용 CORS 설정
public class AuthController {

    private final EmailService emailService;

    // 1. 인증 메일 발송 요청 API (POST /api/auth/email)
    @PostMapping("/email")
    public ResponseEntity<?> requestEmailAuth(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        try {
            emailService.sendVerificationEmail(email);
            return ResponseEntity.ok(Map.of("status", "SUCCESS", "message", "인증 메일이 발송되었습니다."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("status", "ERROR", "message", e.getMessage()));
        } catch (Exception e) {
            // 디버그: 실제 에러 메시지 반환 (배포 후 확인 후 제거 예정)
            return ResponseEntity.internalServerError().body(Map.of("status", "ERROR", "message", e.getClass().getSimpleName() + ": " + e.getMessage()));
        }
    }

    // 2. 인증 번호 검증 API (POST /api/auth/verify)
    @PostMapping("/verify")
    public ResponseEntity<?> verifyEmailCode(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String code = request.get("code");

        boolean isVerified = emailService.verifyCode(email, code);
        if (isVerified) {
            return ResponseEntity.ok(Map.of("status", "SUCCESS", "message", "학생 인증에 성공했습니다."));
        } else {
            return ResponseEntity.badRequest().body(Map.of("status", "FAIL", "message", "인증번호가 일치하지 않거나 만료되었습니다."));
        }
    }
}