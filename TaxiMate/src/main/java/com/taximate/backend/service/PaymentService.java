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

    // 순차 하차 방식 요금 분배
    // 택시는 모두 함께 타서 가까운 목적지 순서로 내림
    // 각 구간 요금은 그 구간에 탑승한 인원수로 나눔
    private Map<String, Integer> calcDistanceFareSplit(TaxiRoom room, int totalFare) {
        Map<String, String> userDests = room.getUserDestinations();
        String departure = room.getDeparture();
        java.util.List<String> users = new java.util.ArrayList<>(room.getUserIds());
        Map<String, Integer> split = new HashMap<>();
        users.forEach(u -> split.put(u, 0));

        // 각 유저의 출발지→목적지 거리 계산
        Map<String, Double> distFromDep = new HashMap<>();
        for (String uid : users) {
            String dest = userDests.getOrDefault(uid, room.getDestination());
            double dist = matchEngine.getDistanceBetweenPlaces(departure, dest);
            distFromDep.put(uid, Math.max(dist, 0.01));
        }

        // 거리 기준 오름차순 정렬 (가까운 순 = 먼저 내리는 순)
        users.sort(java.util.Comparator.comparingDouble(distFromDep::get));

        // 전체 노선 거리 = 가장 멀리 가는 사람의 거리
        double totalRouteDist = distFromDep.get(users.get(users.size() - 1));
        if (totalRouteDist <= 0) {
            int eq = (int) Math.ceil((double) totalFare / users.size());
            split.replaceAll((k, v) -> eq);
            return split;
        }

        // 구간별 요금 계산
        // 구간 i: i-1번째 하차 지점 ~ i번째 하차 지점
        // 해당 구간에 탑승한 인원 = (users.size() - i)명
        double prevDist = 0.0;
        int remaining = totalFare;
        for (int i = 0; i < users.size(); i++) {
            String uid = users.get(i);
            double currDist = distFromDep.get(uid);
            double legDist = currDist - prevDist;          // 이번 구간 거리
            int passengersOnLeg = users.size() - i;         // 이 구간 탑승 인원

            // 이번 구간 요금 = totalFare × (legDist / totalRouteDist)
            int legFare;
            if (i < users.size() - 1) {
                legFare = (int) Math.round(totalFare * legDist / totalRouteDist);
                remaining -= legFare;
            } else {
                legFare = remaining; // 마지막 구간 = 남은 금액 (반올림 오차 흡수)
            }

            // 이 구간을 탄 모든 사람(i번째 이후 포함)에게 1/passengersOnLeg씩 부과
            int perPerson = (int) Math.ceil((double) legFare / passengersOnLeg);
            for (int j = i; j < users.size(); j++) {
                split.merge(users.get(j), perPerson, Integer::sum);
            }

            prevDist = currDist;
        }

        System.out.println("[PaymentService] 순차하차 정산: " + split
                + " | 거리순: " + users.stream()
                    .map(u -> u + "→" + String.format("%.1f", distFromDep.get(u)) + "km")
                    .collect(java.util.stream.Collectors.joining(", ")));
        return split;
    }

    // 3. 정산 현황 조회
    @Transactional(readOnly = true)
    public Payment getPayment(String roomId) {
        return paymentRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("정산 정보가 없습니다."));
    }
}
