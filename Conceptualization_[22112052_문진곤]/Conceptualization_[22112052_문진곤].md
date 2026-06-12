# 1. Conceptualization

**Project Title**: Taxi Mate
![Logo](./TaxiMate.png)  
**Student Info**: 22112052, 문진곤, moonjg0305@yu.ac.kr  
**Repository**: [https://github.com/nogniz/TaxiMate](https://github.com/nogniz/TaxiMate)

## [ Revision history ]

| Revision date | Version # | Description | Author |
| :--- | :--- | :--- | :--- |
| 03/27/2026 | 0.1 | 초기 컨셉 잡기 | 문진곤 |
| 06/11/2026 | 1.0 | 실제 구현 기반으로 전면 업데이트 (이메일 인증, JWT, 자동 매칭, WebSocket, 정산) | 문진곤 |
| 06/11/2026 | 2.0 | 탑승완료/출발/강퇴 기능, 순차하차 정산, 장소 선택 피커, 1:1 DM, SETTLED 상태 추가 반영 | 문진곤 |
| 06/13/2026 | 2.1 | 이메일 발송 방식 Google SMTP → Brevo Transactional Email API(HTTPS REST)로 교체, 로컬 폴백 코드(123456) 추가 | 문진곤 |

---

## 1. Business purpose

* 대학가에서 택시는 중요한 이동 수단이지만, 혼자 이용하기에는 비용 부담이 큽니다.
* 실시간으로 목적지가 맞는 사람을 찾기 어렵고 신원 확인이 불확실하다는 단점이 있습니다.
* 특히 대학교 주변처럼 특정 시간대에 수요가 몰리는 지역에서는 동승자를 찾는 과정 자체가 스트레스가 되기도 합니다.
* 본 프로젝트는 사용자가 일일이 동승자를 찾아 헤매는 수고를 덜어주는 것을 목표로 합니다.
* 사용자가 목적지와 출발 일시를 선택하면, 시스템이 Haversine 공식으로 목적지 간 직선거리를 계산하고 ±30분 이내 출발 시각인 방에 자동으로 합류시킵니다.
* 이를 통해 복잡한 검색 과정 없이도 단 몇 번의 클릭만으로 안전하고 저렴하게 택시를 이용할 수 있는 편의성을 제공하고자 합니다.
* 학교 이메일 인증 시스템을 도입하여 '누가 탈지 모른다'는 불안감을 해소하고 신뢰할 수 있는 대학생 전용 동승 환경을 구축합니다.
* 탑승 완료 후에는 총 택시비를 **순차하차(Sequential Drop-off) 방식**으로 계산하여 가까운 목적지까지만 가는 사람은 덜, 멀리 가는 사람은 더 부담하도록 합니다.

---

## 2. System context diagram

![System Context Diagram](./diagram.png)

* **Login / Register**: JWT 기반 로그인 및 학교 이메일 인증을 통한 회원가입
* **Request Ride**: 동승 요청 (방 개설: 제목, 출발지 선택, 목적지 선택, 출발 일시, 최대 인원)
* **Auto Match**: Haversine 공식 기반 목적지 유사도(≤2km) + 출발시각 ±30분 이내 자동 매칭 및 방 합류
* **Board / Depart / Kick**: 탑승완료(전원), 출발(방장, 전원 탑승 완료 시), 강퇴(방장 전용) 기능
* **Receive Notification**: WebSocket(STOMP)으로 MATCHED / FULL / BOARDED / KICKED / COMPLETED / CANCELLED / PAYMENT / PAYMENT_DONE 실시간 알림 수신
* **Update Room Status**: 방 상태 전환 (WAITING → FULL → COMPLETED → SETTLED / CANCELLED)
* **Send/Receive Message**: 동승자 간 실시간 WebSocket 채팅 (방 채팅 + 1:1 DM)
* **Process Payment**: 순차하차 정산 (총 금액 입력 → 거리 비율 기반 개인 금액 계산 → 납부 처리 → SETTLED)
* **Request/Confirm Auth**: Brevo Transactional Email API(HTTPS REST)를 통한 학교 이메일 6자리 인증코드 발송 및 검증
* **Rate Passenger**: 이용 완료 후 동승자 평가 (👍 +0.5°C / 👎 -0.3°C 매너 온도 갱신)

---

## 3. Use case list

| No | Use Case | Actor | Description |
| :--- | :--- | :--- | :--- |
| 1 | Login / Logout | User | 등록된 이메일+비밀번호로 JWT 토큰을 발급받아 시스템에 접속함 |
| 2 | Register | User | 학교 이메일 인증(6자리 코드, 3분 만료)을 완료한 후 학번/이름/비밀번호로 계정을 생성함 |
| 3 | View My Profile | User | JWT 인증 후 본인의 학번, 이름, 이메일, 매너 온도를 조회함 |
| 4 | Create Room | User | 제목, 출발지(선택 피커), 목적지(선택 피커), 출발 일시(datetime), 최대 인원을 설정하여 WAITING 상태의 동승 방을 생성함 |
| 5 | Auto Match | User | 목적지·출발 일시를 입력하면 시스템이 Haversine 거리(≤2km) + ±30분 이내 조건으로 기존 방에 자동 합류함 |
| 6 | Join Room | User | 방 목록에서 원하는 방에 개인 목적지를 지정하여 수동 참여함 |
| 7 | Board Completed | User | 탑승 완료 시 각 참여자가 '탑승완료' 버튼을 누르고, boardedUsers에 등록됨 |
| 8 | Depart | User (Host) | 방장이 모든 참여자의 탑승완료 확인 후 '출발' 처리 → COMPLETED 전환 |
| 9 | Kick User | User (Host) | 방장이 특정 참여자를 강퇴하면 방에서 제거되고 KICKED 알림이 발송됨 |
| 10 | Cancel Room | User (Host) | 방장이 방 취소 시 WAITING/FULL → CANCELLED로 전환됨 |
| 11 | Send/Receive Message | User | 동승자들과 WebSocket 기반 실시간 방 채팅 또는 1:1 DM을 주고받음 |
| 12 | Receive Notification | User | WebSocket(STOMP) 구독을 통해 MATCHED / FULL / BOARDED / KICKED / COMPLETED / PAYMENT 등의 이벤트를 실시간으로 수신함 |
| 13 | Process Payment | User | COMPLETED 상태 방에서 총 택시비를 입력하면 순차하차 방식으로 1인당 금액이 계산되고 개인별 납부 처리를 진행함. 전원 납부 시 SETTLED 전환 |
| 14 | Rate Passenger | User | 이용내역에서 동승자를 👍/👎 평가하여 매너 온도를 갱신함 |
| 15 | DM from History | User | 이용내역 카드에서 동승자에게 1:1 채팅(DM)을 바로 시작할 수 있음 |
| 16 | Request / Confirm Auth | Email Server | Brevo Transactional Email API(HTTPS REST)로 인증코드를 발송하고 3분 내 일치 여부를 검증함. 로컬 환경(`BREVO_API_KEY` 미설정) 시 고정 코드 `123456` 사용 |

---

## 4. Concept of operation

### 1) Login / Register
* **Purpose**: 학교 이메일 인증을 통해 대학생 신분을 검증하고 JWT 기반 인증 토큰을 발급함.
* **Approach**: `POST /api/auth/email`로 인증코드 발송 → `POST /api/auth/verify`로 검증 → `POST /api/users/register`로 등록 → `POST /api/users/login`으로 JWT 발급.
* **Dynamics**: 인증코드는 3분 만료이며, 로그인 성공 시 24시간 유효한 JWT가 발급됨.
* **Goals**: 허위 계정 방지 및 서비스 안전성 극대화.

### 2) View My Profile & Manner Temperature
* **Purpose**: 본인의 프로필과 신뢰도 지표(매너 온도)를 확인하고 관리함.
* **Approach**: `GET /api/users/me`로 JWT 인증 후 프로필 조회. `POST /api/users/{uid}/rate`로 동승자 평가.
* **Dynamics**: 좋아요(+0.5°C) / 싫어요(-0.3°C). 0~100°C 범위로 클램핑됨. 기본값 36.5°C.
* **Goals**: 신뢰할 수 있는 동승 환경 유지.

### 3) Create Room / Auto Match
* **Purpose**: 새로운 동승 그룹 생성 혹은 경로가 유사한 기존 방에 자동 합류.
* **Approach**: `POST /api/rooms`로 방 생성. `POST /api/rooms/match`로 자동 매칭.
* **Dynamics**: 출발지·목적지는 26개 사전 정의 장소(DAEGU_COORDS)에서 선택하여 좌표 오류 없음. Haversine 거리(≤2km) + 출발 시각 ±30분 이내 조건을 모두 만족하는 방에 자동 합류. MATCHED 알림 발송.
* **Goals**: 수동 검색 없이 자동으로 최적의 동승 파트너 매칭.

### 4) Boarding & Departure Flow
* **Purpose**: 실제 탑승 여부를 확인하여 안전한 출발을 보장함.
* **Approach**: 각 참여자가 `POST /api/rooms/{id}/board`로 탑승완료 처리 → 방장은 `isAllBoarded() = true` 확인 후 `PATCH /api/rooms/{id}/depart`로 출발 처리.
* **Dynamics**: BOARDED 알림으로 모든 참여자가 탑승 현황(n/total)을 실시간 확인. 방장만 강퇴(`DELETE /api/rooms/{id}/kick/{uid}`)와 출발 처리가 가능.
* **Goals**: 미탑승자가 있는 상태에서의 출발 처리 방지.

### 5) Room Status Transitions
* **Purpose**: 동승 방의 수명 주기를 명확하게 관리함.
* **Approach**: WAITING → FULL (자동), FULL/WAITING → COMPLETED (방장 출발), COMPLETED → SETTLED (전원 납부), WAITING/FULL → CANCELLED (방장 취소).
* **Dynamics**: COMPLETED/CANCELLED/SETTLED는 Terminal 상태. 잘못된 전이 시 400 오류 반환.
* **Goals**: 예약 현황 정확성 유지 및 정산 진입 조건 보장.

### 6) Send / Receive Message & Notifications
* **Purpose**: 매칭된 인원 간 실시간 소통 및 시스템 이벤트의 즉각적 전달.
* **Approach**: STOMP over SockJS. 방 채팅(`/sub/chat/room/{roomId}`), DM(`/sub/chat/dm/{dmId}`), 알림(`/sub/notification/{roomId}`) 채널 분리 운용.
* **Dynamics**: 알림 타입: MATCHED, FULL, BOARDED, KICKED, COMPLETED, CANCELLED, PAYMENT, PAYMENT_DONE.
* **Goals**: 지연 없는 실시간 소통 및 이벤트 전달.

### 7) Process Payment (순차하차 N빵)
* **Purpose**: 탑승 완료 후 총 택시비를 탑승 구간 비율로 공정하게 분배함.
* **Approach**: `POST /api/payments/{roomId}`로 총 금액 입력 → PaymentService가 각 참여자의 목적지까지 거리를 계산하고 순차하차 알고리즘으로 구간별 요금을 분배 → `PATCH /api/payments/{roomId}/pay`로 납부 처리 → 전원 납부 시 PAYMENT_DONE 알림 + SETTLED 전환.
* **Dynamics**: 순차하차 방식: 가까운 목적지까지의 구간 요금은 해당 구간 탑승자 전원이 균등 분담. 멀리 가는 사람일수록 더 많은 금액 부담.
* **Goals**: 정확하고 공정한 정산.

### 8) Request / Confirm Auth (Email Server)
* **Purpose**: 외부 이메일 서비스를 통한 실제 대학생 여부 최종 확정.
* **Approach**: Brevo Transactional Email API(HTTPS REST, `https://api.brevo.com/v3/smtp/email`)로 6자리 코드 발송. Railway SMTP 포트 차단을 우회하며 임의의 `@yu.ac.kr` 수신자에게 발송 가능. 3분 내 입력 시 인증 완료.
* **Dynamics**: `BREVO_API_KEY` 환경변수 미설정 시(로컬 개발환경) 이메일 발송 없이 고정 코드 `123456`으로 동작.
* **Goals**: 허위 계정 방지 및 서비스 안전성 극대화.

---

## 5. Problem statement

* **실시간 데이터 처리**: 여러 사용자의 동승 요청을 실시간으로 처리하기 위해 WebSocket(STOMP) 기반 비동기 알림 시스템을 도입하였다.
* **신뢰성 및 보안 문제**: 학교 이메일 인증과 JWT Stateless 인증으로 허위 계정과 인증 우회를 방지하였다. BCrypt로 비밀번호를 단방향 해시 저장하여 개인정보 노출을 최소화하였다.
* **이메일 발송 인프라**: Railway 배포 환경에서 SMTP 포트(25/465/587)가 차단되어 JavaMailSender(Google SMTP) 방식이 불가하다. 이를 Brevo Transactional Email API(HTTPS REST)로 교체하여 포트 차단 없이 임의의 `@yu.ac.kr` 주소로 인증코드 발송이 가능해졌다.
* **매칭 정확도**: Google Maps API 대신 Haversine 공식을 직접 구현하여 외부 API 의존도 없이 좌표 기반 거리 계산을 수행하였다. 또한 26개 사전 정의 장소 선택 피커를 도입하여 장소명 모호성(예: 경대병원역 vs 칠곡경대병원역)을 근본적으로 제거하였다.
* **결제 및 정산의 투명성**: 순차하차(Sequential Drop-off) 방식으로 목적지가 가까운 참여자와 먼 참여자의 부담을 거리 비율에 따라 공정하게 분배하였다. 외부 PG사 연동 없이 시스템 내부에서 납부 상태를 추적한다.
* **탑승 안전성**: boardedUsers 집합으로 각 참여자의 탑승 여부를 추적하고, 전원 탑승완료 시에만 방장의 출발 처리가 가능하도록 isAllBoarded() 게이트를 구현하였다.
* **비기능적 요구사항 (NFRs)**:
    * **응답성**: WebSocket 알림 및 REST API 응답 시간 100ms 이내 (테스트 완료).
    * **가용성**: Spring Boot Embedded Tomcat으로 안정적 운영.
    * **보안**: Spring Security 6.1.5 + JWT + BCrypt 적용.

---

## 6. Glossary

* **동승 (Carpooling)**: 같은 방향 승객들이 택시를 나누어 타고 비용을 분담하는 행위.
* **매너 온도 (Manner Temperature)**: 상호 평가를 통해 시각화한 사용자의 신뢰도 지표. 기본값 36.5도. 👍 +0.5°C / 👎 -0.3°C.
* **Haversine 공식**: 지구 구면 위 두 위도/경도 좌표 간의 최단 직선거리를 계산하는 공식. 임계값 2km 이내 + ±30분 이내 출발 시각 시 자동 매칭 승인.
* **순차하차 정산 (Sequential Drop-off)**: 총 택시비를 탑승 구간별로 나누고, 해당 구간에 탑승한 인원 수로 균등 분담하는 방식. 멀리 가는 사람이 더 많이 부담.
* **JWT (JSON Web Token)**: 서버 세션 없이 인증 상태를 유지하는 자가 검증 토큰. 24시간 유효.
* **STOMP**: WebSocket 위에서 pub/sub 메시지 라우팅을 담당하는 서브프로토콜.
* **boardedUsers**: 각 참여자의 탑승완료 여부를 추적하는 Set. isAllBoarded() = true일 때만 출발 처리 가능.
* **WAITING / FULL / COMPLETED / SETTLED / CANCELLED**: TaxiRoom의 상태 값. COMPLETED → SETTLED → Terminal. CANCELLED → Terminal.
* **Brevo Transactional Email API**: HTTPS REST 방식의 이메일 발송 서비스. Railway 환경의 SMTP 포트 차단을 우회하며 임의의 수신자에게 발송 가능.
* **로컬 폴백 코드**: `BREVO_API_KEY` 환경변수 미설정 시 이메일 발송 없이 인증코드 `123456`을 고정으로 사용하는 개발 편의 기능.

---

## 7. References

* **Spring Boot 3.1.5 Documentation**: https://docs.spring.io/spring-boot/docs/3.1.5/reference/html/
* **JJWT 0.11.5**: https://github.com/jwtk/jjwt
* **Spring WebSocket + STOMP Guide**: https://spring.io/guides/gs/messaging-stomp-websocket/
* **Haversine Formula**: 지구 구면 거리 계산 공식 적용 참조.
* **Brevo Transactional Email API**: https://developers.brevo.com/reference/sendtransacemail
