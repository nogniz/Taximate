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

    // 대구 주요 지점 하드코딩 좌표 (Kakao API 실패 시 폴백)
    private static final Map<String, double[]> DAEGU_COORDS = Map.ofEntries(
        Map.entry("영대정문",       new double[]{35.8384, 128.7527}),
        Map.entry("영남대학교",     new double[]{35.8384, 128.7527}),
        Map.entry("영남대",         new double[]{35.8384, 128.7527}),
        Map.entry("영대병원역",     new double[]{35.8428, 128.7528}),
        Map.entry("대구은행역",     new double[]{35.8694, 128.5981}),
        Map.entry("반월당역",       new double[]{35.8657, 128.5938}),  // 대구은행역보다 서쪽 → 영남대에서 더 멀리
        Map.entry("반월당",         new double[]{35.8657, 128.5938}),
        Map.entry("동대구역",       new double[]{35.8798, 128.6283}),
        Map.entry("대구역",         new double[]{35.8795, 128.5889}),
        Map.entry("범어역",         new double[]{35.8618, 128.6284}),
        Map.entry("수성구청역",     new double[]{35.8574, 128.6335}),
        Map.entry("황금역",         new double[]{35.8561, 128.6192}),
        Map.entry("대공원역",       new double[]{35.8409, 128.6361}),
        Map.entry("고산역",         new double[]{35.8366, 128.6611}),
        Map.entry("신매역",         new double[]{35.8379, 128.6481}),
        Map.entry("사월역",         new double[]{35.8350, 128.6757}),
        Map.entry("임당역",         new double[]{35.8355, 128.6878}),
        Map.entry("경산역",         new double[]{35.8274, 128.7395}),
        Map.entry("서문시장",       new double[]{35.8687, 128.5841}),
        Map.entry("칠성시장",       new double[]{35.8841, 128.5973}),
        Map.entry("동성로",         new double[]{35.8703, 128.5968}),
        Map.entry("두류공원",       new double[]{35.8564, 128.5716}),
        Map.entry("북부정류장",     new double[]{35.8895, 128.5875}),
        Map.entry("서부정류장",     new double[]{35.8708, 128.5600}),
        Map.entry("남부정류장",     new double[]{35.8408, 128.5967}),
        Map.entry("대구공항",       new double[]{35.8970, 128.6589})
    );

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

        // ① 하드코딩 사전에서 검색 (부분 일치 포함)
        for (Map.Entry<String, double[]> entry : DAEGU_COORDS.entrySet()) {
            if (location.contains(entry.getKey()) || entry.getKey().contains(location)) {
                double[] coords = entry.getValue();
                coordCache.put(location, coords);
                System.out.println("[MatchEngine] 하드코딩 좌표 사용: " + location + " → " + coords[0] + ", " + coords[1]);
                return coords;
            }
        }

        // ② 최후 폴백: 장소명 해시 기반 대구 내 고유 좌표 생성
        //    → 서로 다른 이름이면 다른 좌표가 나와서 거리 비율 정산이 작동함
        int h = Math.abs(location.hashCode());
        double lat = 35.78 + (h % 20000) / 100000.0;        // 35.78 ~ 35.98
        double lon = 128.48 + ((h * 31) % 27000) / 100000.0; // 128.48 ~ 128.75
        double[] fallback = new double[]{lat, lon};
        coordCache.put(location, fallback);
        System.out.println("[MatchEngine] 해시 폴백 좌표 사용: " + location + " → " + lat + ", " + lon);
        return fallback;
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

    // 4-0. 출발시간 문자열 → 분(int) 변환
    // 지원 형식: "HH:mm" / "2026-06-11T14:30" (datetime-local)
    private int parseMinutes(String timeStr) {
        if (timeStr == null || timeStr.isBlank()) return -1;
        try {
            // datetime-local 형식: "2026-06-11T14:30" → 시간 부분만 추출
            String timePart = timeStr.contains("T")
                    ? timeStr.split("T")[1]
                    : timeStr.trim();
            String[] parts = timePart.split(":");
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