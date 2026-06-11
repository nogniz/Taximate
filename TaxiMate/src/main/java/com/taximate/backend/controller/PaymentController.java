package com.taximate.backend.controller;

import com.taximate.backend.model.Payment;
import com.taximate.backend.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PaymentController {

    private final PaymentService paymentService;

    // 1. 정산 시작 (POST /api/payments/{roomId})
    // Body: { "totalFare": 12000 }
    @PostMapping("/{roomId}")
    public ResponseEntity<?> createPayment(@PathVariable String roomId,
                                           @RequestBody Map<String, Integer> request) {
        int totalFare = request.getOrDefault("totalFare", 0);
        if (totalFare <= 0) {
            return ResponseEntity.badRequest().body(Map.of("status", "ERROR", "message", "택시비를 입력하세요."));
        }
        try {
            Payment payment = paymentService.createPayment(roomId, totalFare);
            return ResponseEntity.ok(Map.of(
                    "status", "SUCCESS",
                    "roomId", payment.getRoomId(),
                    "totalFare", payment.getTotalFare(),
                    "farePerPerson", payment.getFarePerPerson(),
                    "headCount", payment.getHeadCount(),
                    "paidStatus", payment.getPaidStatus(),
                    "fareSplit", payment.getFareSplit()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("status", "ERROR", "message", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("status", "ERROR", "message", e.getMessage()));
        }
    }

    // 2. 내 납부 처리 (PATCH /api/payments/{roomId}/pay)
    // Header: Authorization: Bearer <token>
    @PatchMapping("/{roomId}/pay")
    public ResponseEntity<?> markPaid(@PathVariable String roomId, Authentication auth) {
        String studentId = (String) auth.getPrincipal();
        try {
            Payment payment = paymentService.markPaid(roomId, studentId);
            boolean allPaid = payment.getPaidStatus().values().stream().allMatch(v -> v);
            java.util.List<String> members = new java.util.ArrayList<>(payment.getPaidStatus().keySet());
            return ResponseEntity.ok(Map.of(
                    "status", "SUCCESS",
                    "farePerPerson", payment.getFarePerPerson(),
                    "paidStatus", payment.getPaidStatus(),
                    "allPaid", allPaid,
                    "members", members
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("status", "ERROR", "message", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("status", "ERROR", "message", e.getMessage()));
        }
    }

    // 3. 정산 현황 조회 (GET /api/payments/{roomId})
    @GetMapping("/{roomId}")
    public ResponseEntity<?> getPayment(@PathVariable String roomId) {
        try {
            Payment payment = paymentService.getPayment(roomId);
            boolean allPaid = payment.getPaidStatus().values().stream().allMatch(v -> v);
            return ResponseEntity.ok(Map.of(
                    "roomId", payment.getRoomId(),
                    "totalFare", payment.getTotalFare(),
                    "farePerPerson", payment.getFarePerPerson(),
                    "headCount", payment.getHeadCount(),
                    "paidStatus", payment.getPaidStatus(),
                    "fareSplit", payment.getFareSplit(),
                    "allPaid", allPaid
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("status", "ERROR", "message", e.getMessage()));
        }
    }
}
