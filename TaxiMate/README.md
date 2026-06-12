# TaxiMate 🚕

> 영남대학교 학생 전용 택시 동승 매칭 서비스

**배포 URL**: https://taximate-production.up.railway.app

---

## 🔑 교수님 시연용 계정

| 항목 | 값 |
|------|-----|
| 이메일 | moonjg0305@yu.ac.kr |
| 비밀번호 | test |
| 학번 | 22112052 |
| 이름 | 문진곤 |

> ⚠️ 이메일 인증 시 인증코드는 위 이메일 수신함에 직접 도착합니다.  
> 시연을 위해 교수님 본인의 `@yu.ac.kr` 이메일로 직접 가입하셔도 됩니다.

---

## 주요 기능

- **영남대 이메일 인증 회원가입** (`@yu.ac.kr` 전용)
- **택시 방 생성 / 참여** (출발지·목적지·시간·인원 설정)
- **실시간 채팅** (STOMP over WebSocket)
- **자동 요금 분배** (거리 기반 1/N 정산)
- **DM 기능** (1:1 다이렉트 메시지)

---

## 기술 스택

| 구분 | 기술 |
|------|------|
| Backend | Spring Boot 3.1.5, Java 17 |
| 인증 | JWT (Access Token) |
| 실시간 | STOMP over WebSocket + SockJS |
| DB | H2 In-Memory |
| 이메일 | Brevo Transactional Email API |
| 배포 | Railway (Nixpacks) |

---

## 향후 개선 사항

### 🔒 개인정보 보호 — 학번 비공개 전환 검토

현재 채팅 및 매칭 화면에 **학번과 실명이 함께 노출**됩니다.

**이름 공개는 유지**하는 것이 맞습니다. 모르는 사람과 택시를 동승하는 서비스 특성상 상대방의 이름을 알 수 있어야 신뢰도가 높아지고 실제로 이용할 수 있기 때문입니다.

반면 **학번은 공개할 필요가 없습니다**:

- 학번은 이미 이메일 인증(@yu.ac.kr)으로 재학생 여부가 검증됨
- 학번이 노출되면 학교 시스템과 연계해 추가 신원 특정 가능
- 채팅/매칭 상대에게 학번까지 알릴 이유 없음

**개선안**: 채팅·매칭 화면에는 이름만 표시, 학번은 서버 내부 식별자 및 관리자 용도로만 사용

---

## 실행 방법 (로컬)

```bash
cd TaxiMate
./gradlew bootRun
# http://localhost:8080
```

> 로컬 실행 시 `BREVO_API_KEY` 환경변수가 없으면 이메일 발송 없이 **인증코드 `123456` 고정**으로 동작합니다.  
> 실제 이메일 발송이 필요하면 IntelliJ → Run/Debug Configurations → Environment variables에 `BREVO_API_KEY` 추가
