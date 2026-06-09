package com.taximate.backend.service;

import com.taximate.backend.model.TaxiRoom;
import com.taximate.backend.repository.TaxiRoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class MatchEngine {

    private final TaxiRoomRepository roomRepository; // 💡 DB 직접 주입
    private static final double EARTH_RADIUS = 6371.0; // 지구 반지름 (km)

    // 1. 하버사인 공식 거리 계산
    public double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS * c;
    }

    // 2. 가짜 좌표 매핑 테이블
    private double[] getCoordinates(String location) {
        switch (location) {
            case "영남대 정문":   return new double[]{35.8361, 128.7529};
            case "영남대역":     return new double[]{35.8365, 128.7532};
            case "대구은행역":   return new double[]{35.8598, 128.6147};
            case "영남대 뒷문":   return new double[]{35.8422, 128.7570};
            case "반월당역":     return new double[]{35.8655, 128.5934};
            default:            return new double[]{35.0000, 128.0000};
        }
    }

    // 3. 거리 기반 경로 유사도 계산
    public double calculateRouteSimilarity(String userRoute, String targetRoute) {
        double[] userCoord = getCoordinates(userRoute);
        double[] targetCoord = getCoordinates(targetRoute);

        double distanceError = calculateDistance(userCoord[0], userCoord[1], targetCoord[0], targetCoord[1]);
        double similarity = 1.0 - (distanceError / 7.5);

        if (similarity < 0) similarity = 0.0;
        return similarity;
    }

    // 4. DB 기반 실시간 자동 매칭 및 자동 합류 비즈니스 로직
    public TaxiRoom findAndJoinMatch(String userId, String title, String departure, String destination, String departureTime) {
        // 💡 메모리가 아니라 실제 DB에서 현재 대기 중(WAITING)인 모든 방을 끌고 옴
        List<TaxiRoom> activeRooms = roomRepository.findAll();

        for (TaxiRoom room : activeRooms) {
            if ("WAITING".equals(room.getStatus())) {
                double depSimilarity = calculateRouteSimilarity(departure, room.getDeparture());
                double destSimilarity = calculateRouteSimilarity(destination, room.getDestination());

                if ((depSimilarity + destSimilarity) / 2 >= 0.80) {
                    room.addUser(userId);
                    return roomRepository.save(room); // 💡 합류된 상태 변경 후 DB에 즉시 업데이트
                }
            }
        }
        return null; // 맞는 방 없으면 null 리턴 (컨트롤러에서 새 방 생성 트리거 작동)
    }
}