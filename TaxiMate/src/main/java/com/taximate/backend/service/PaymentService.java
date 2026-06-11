package com.taximate.backend.service;

import com.taximate.backend.model.Payment;
import com.taximate.backend.model.TaxiRoom;
import com.taximate.backend.repository.PaymentRepository;
import com.taximate.backend.repository.TaxiRoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final TaxiRoomRepository roomRepository;
    private final NotificationService notificationService;
    private final MatchEngine matchEngine;

    // 1. 정산 시작 (총 택시비 입력 → n빵 계산 후 저장)
    public Payment createPayment(String roomId, int totalFare) {
        TaxiRoom room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 방입니다."));

        if (!"COMPLETED".equals(room.getStatus())) {
            throw new IllegalStateException("탑승 완료된 방에서만 정산할 수 있습니다.");
        }
        if (paymentRepository.existsById(roomId)) {
            throw new IllegalStateException("이미 정산이 시작된 방입니다.");
        }

        // 거리 비율 기반 개인 금액 계산
        Map<String, Integer> fareSplit = calcDistanceFareSplit(room, totalFare);

        Payment payment = new Payment(roomId, totalFare, room.getUserIds(), fareSplit);
        Payment saved = paymentRepository.save(payment);

        // 정산 시작 알림
        notificationService.notify(roomId, "PAYMENT",
                "정산이 시작되었습니다! 1인당 " + saved.getFarePerPerson() + "원입니다.");

        return saved;
    }

    // 2. 납부 처리 (특정 유저가 자기 몫 냈다고 표시)
    public Payment markPaid(String roomId, String userId) {
        Payment payment = paymentRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("정산 정보가 없습니다."));

        if (!payment.getPaidStatus().containsKey(userId)) {
            throw new IllegalArgumentException("해당 방의 참여자가 아닙니다.");
        }
        if (payment.getPaidStatus().get(userId)) {
            throw new IllegalStateException("이미 납부 처리된 유저입니다.");
        }

        payment.getPaidStatus().put(userId, true);
        Payment saved = paymentRepository.save(payment);

        // 전원 납부 완료 시 방 SETTLED 처리 + 알림
        boolean allPaid = saved.getPaidStatus().values().stream().allMatch(v -> v);
        if (allPaid) {
            TaxiRoom room = roomRepository.findById(roomId).orElse(null);
            if (room != null) {
                room.setStatus("SETTLED");
                roomRepository.save(room);
            }
            notificationService.notify(roomId, "PAYMENT_DONE", "모든 인원이 정산을 완료했습니다! 🎉");
        }

        return saved;
    }

    // 거리 비율 기반 개인 금액 계산 (출발지 ~ 각자 목적지)
    private Map<String, Integer> calcDistanceFareSplit(TaxiRoom room, int totalFare) {
        Map<String, String> userDests = room.getUserDestinations();
        String departure = room.getDeparture();
        java.util.List<String> users = room.getUserIds();

        // 각 유저의 거리 계산
        Map<String, Double> distMap = new HashMap<>();
        for (String uid : users) {
            String dest = userDests.getOrDefault(uid, room.getDestination());
            double dist = matchEngine.getDistanceBetweenPlaces(departure, dest);
            distMap.put(uid, Math.max(dist, 0.1)); // 최소 0.1km (0원 방지)
        }

        double totalDist = distMap.values().stream().mapToDouble(Double::doubleValue).sum();
        Map<String, Integer> split = new HashMap<>();

        if (totalDist <= 0) {
            // 거리 계산 실패 시 균등 분배
            int eq = (int) Math.ceil((double) totalFare / users.size());
            users.forEach(uid -> split.put(uid, eq));
            return split;
        }

        // 비율대로 계산하되 반올림 오차는 첫 번째 유저에게 흡수
        int allocated = 0;
        for (int i = 0; i < users.size(); i++) {
            String uid = users.get(i);
            if (i < users.size() - 1) {
                int fare = (int) Math.round(totalFare * distMap.get(uid) / totalDist);
                split.put(uid, Math.max(fare, 0));
                allocated += fare;
            } else {
                split.put(uid, Math.max(totalFare - allocated, 0));
            }
        }

        System.out.println("[PaymentService] 거리 비율 정산: " + split);
        return split;
    }

    // 3. 정산 현황 조회
    @Transactional(readOnly = true)
    public Payment getPayment(String roomId) {
        return paymentRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("정산 정보가 없습니다."));
    }
}
