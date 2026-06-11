package com.taximate.backend.service;

import com.taximate.backend.model.TaxiRoom;
import com.taximate.backend.repository.TaxiRoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Transactional
public class MatchEngine {

    private final TaxiRoomRepository roomRepository;
    private final NotificationService notificationService;
    private static final double EARTH_RADIUS = 6371.0;
    private static final String KAKAO_API_KEY = "KakaoAK 3dd2f26b211e73ee92b4683eceffdbea";
    private static final String KAKAO_LOCAL_URL = "https://dapi.kakao.com/v2/local/search/keyword.json?query=";
    // 같은 장소명은 캐싱해서 API 중복 호출 방지
    private final Map<String, double[]> coordCache = new ConcurrentHashMap<>();

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

    // 2. 카카오 Local API로 실제 좌표 조회
    @SuppressWarnings("unchecked")
    private double[] getCoordinates(String location) {
        if (location == null || location.isBlank()) return new double[]{35.8500, 128.6000};

        // 캐시에 있으면 바로 반환
        if (coordCache.containsKey(location)) return coordCache.get(location);

        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", KAKAO_API_KEY);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            String url = KAKAO_LOCAL_URL + java.net.URLEncoder.encode(location, "UTF-8");
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                List<Map<String, Object>> documents = (List<Map<String, Object>>) response.getBody().get("documents");
                if (documents != null && !documents.isEmpty()) {
                    Map<String, Object> place = documents.get(0);
                    double lat = Double.parseDouble(place.get("y").toString());
                    double lon = Double.parseDouble(place.get("x").toString());
                    double[] coords = new double[]{lat, lon};
                    coordCache.put(location, coords); // 캐싱
                    return coords;
                }
            }
        } catch (Exception e) {
            System.out.println("[MatchEngine] 카카오 API 조회 실패: " + location + " → " + e.getMessage());
        }

        // API 실패 시 대구 중심 좌표
        return new double[]{35.8500, 128.6000};
    }

    // 2-1. 두 장소명 사이의 실제 거리(km) 반환 - 정산 비율 계산용
    public double getDistanceBetweenPlaces(String place1, String place2) {
        double[] c1 = getCoordinates(place1);
        double[] c2 = getCoordinates(place2);
        return calculateDistance(c1[0], c1[1], c2[0], c2[1]);
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

    // 4-0. 출발시간 문자열 → 분(int) 변환 (HH:mm 형식, 파싱 실패 시 -1)
    private int parseMinutes(String timeStr) {
        if (timeStr == null || timeStr.isBlank()) return -1;
        try {
            String[] parts = timeStr.trim().split(":");
            if (parts.length < 2) return -1;
            return Integer.parseInt(parts[0]) * 60 + Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    // 4. DB 기반 실시간 자동 매칭 및 자동 합류 비즈니스 로직
    public TaxiRoom findAndJoinMatch(String userId, String title, String departure, String destination, String departureTime) {
        // 💡 메모리가 아니라 실제 DB에서 현재 대기 중(WAITING)인 모든 방을 끌고 옴
        List<TaxiRoom> activeRooms = roomRepository.findAll();
        int myMinutes = parseMinutes(departureTime);

        for (TaxiRoom room : activeRooms) {
            if ("WAITING".equals(room.getStatus())) {
                // ▶ 출발시간 30분 이내 조건
                if (myMinutes >= 0) {
                    int roomMinutes = parseMinutes(room.getDepartureTime());
                    if (roomMinutes >= 0 && Math.abs(myMinutes - roomMinutes) > 30) {
                        continue; // 30분 초과 → 스킵
                    }
                }

                double depSimilarity = calculateRouteSimilarity(departure, room.getDeparture());
                double destSimilarity = calculateRouteSimilarity(destination, room.getDestination());

                if ((depSimilarity + destSimilarity) / 2 >= 0.80) {
                    room.addUser(userId, destination); // 매칭 시 개인 목적지 등록
                    TaxiRoom saved = roomRepository.save(room);

                    // 매칭 알림
                    notificationService.notify(saved.getRoomId(), "MATCHED",
                            userId + "님이 방에 합류했습니다. (" + saved.getUserIds().size() + "/" + saved.getMaxParticipants() + ")");

                    // 인원이 꽉 찼으면 FULL 알림 추가
                    if ("FULL".equals(saved.getStatus())) {
                        notificationService.notify(saved.getRoomId(), "FULL",
                                "인원이 꽉 찼습니다! 탑승 준비를 해주세요.");
                    }

                    return saved;
                }
            }
        }
        return null; // 맞는 방 없으면 null 리턴 (컨트롤러에서 새 방 생성 트리거 작동)
    }
}