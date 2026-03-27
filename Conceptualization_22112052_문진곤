# Taximate# 1. Conceptualization

**Project Title**: Taxi Mate
<img width="769" height="645" alt="TaxiMate - 복사본" src="https://github.com/user-attachments/assets/2dd66852-2626-4b0f-95cf-a42006f5fb55" />

**Student Info**: 22112052, 문진곤, moonjg0305@yu.ac.kr  
**Repository**: [https://github.com/nogniz/Taximate](https://github.com/nogniz/Taximate)

## [ Revision history ]

| Revision date | Version # | Description | Author |
| :--- | :--- | :--- | :--- |
| 03/27/2026 | 0.1 | 초기 컨셉 잡기 | 문진곤 |

---

## 1. Business purpose

*  대학가에서 택시는 중요한 이동 수단이지만, 혼자 이용하기에는 비용 부담이 큽니다.  
*  실시간으로 목적지가 맞는 사람을 찾기 어렵고 신원 확인이 불확실하다는 단점이 있습니다.  
*  특히 대학교 주변처럼 특정 시간대에 수요가 몰리는 지역에서는 동승자를 찾는 과정 자체가 스트레스가 되기도 합니다.  
*  본 프로젝트는 사용자가 일일이 동승자를 찾아 헤매는 수고를 덜어주는 것을 목표로 합니다.  
*  사용자가 목적지와 시간대만 입력하면, 시스템이 위치 정보를 기반으로 최적의 동승 후보를 필터링하여 매칭해 줍니다.  
*  이를 통해 복잡한 검색 과정 없이도 단 몇 번의 클릭만으로 안전하고 저렴하게 택시를 이용할 수 있는 편의성을 제공하고자 합니다.  
*  앱 사용 시 위치 정보 활용을 통해 출발지 설정을 자동화하고, 사용자의 이동 경로를 분석하여 경로 상에 있는 다른 사용자들을 추천합니다.  
*  또한, 학교 메일 인증 시스템을 도입하여 '누가 탈지 모른다'는 불안감을 해소하고 신뢰할 수 있는 대학생 전용 동승 환경을 구축합니다.  

---

## 2. System context diagram

<img width="1414" height="402" alt="diagram - 복사본" src="https://github.com/user-attachments/assets/87dbcfb9-6e4f-4cb5-ad6f-094adeb70578" />


* **Login**: 로그인
* **Register**: 회원가입
* **View My Profile**: 내 정보 조회
* **Logout**: 로그아웃
* **Request Ride**: 동승 요청 (목적지, 시간 설정)
* **Join Room**: 생성된 동승 방 참여
* **Send/Receive Message**: 동승자 간 실시간 채팅 송신 및 수신
* **Cancel Ride**: 동승 요청 방 참여 취소
* **Update Room Status**: 동승 방 상태 정보 업데이트
* **Receive Match**: 실시간 동승자 매칭 결과 수신
* **Request/Confirm Auth**: 메일 인증 요청 및 결과 확인
* **Verify/Return Location**: 위치 및 경로 검증 요청 및 정보 반환
* **Process Payment / Status**: 결제 성공/실패 상태 반환

---

## 3. Use case list

| No | Use Case | Actor | Description |
| :--- | :--- | :--- | :--- |
| 1 | Login/Logout | User | 등록된 계정으로 시스템에 접속하거나 접속을 종료함 |
| 2 | Register | User | 메일 인증을 통해 새로운 계정을 생성함 |
| 3 | View My Profile | User | 본인의 사용자 정보, 이용기록, 신뢰도 점수 등 조회 |
| 4 | Request Ride | User | 목적지와 출발 시간을 설정하여 새로운 동승자 방을 생성함 |
| 5 | Join Room | User | 이미 생성된 동승 방 목록을 확인하고 참여를 요청함 |
| 6 | Cancel Ride | User | 생성한 방을 삭제하거나 참여 중인 동승 방에서 나감 |
| 7 | Send/Receive Message | User | 매칭된 동승자들과 실시간으로 채팅을 주고받음 |
| 8 | Receive Match | User | 시스템 알고리즘에 의해 최적의 동승자가 매칭되었음을 알림받음 |
| 9 | Update Room Status | User | 동승 인원 충족, 운행 시작/종료 등 상태 실시간 업데이트 |
| 10 | Verify / Return Location | 지도 API | 현재 위치 및 경로 유사도를 검증하여 거리 정보를 반환함 |
| 11 | Request / Confirm Auth | 인증 서버 | 메일을 통해 인증하고 성공/실패 여부를 확인함 |
| 12 | Process Payment (PG) | 결제 시스템 | 확정된 거리에 따른 금액 정산 및 송금/결제 절차 진행 |

---

## 4. Concept of operation

### 1) Login / Logout
*  **Purpose**: 앱을 사용하기 위해 등록된 사용자인지 확인하고 접속 종료를 관리함.  
*  **Approach**: ID/PW 입력 후 서버에서 회원 정보를 조회하여 성공/실패 여부 확인.  
*  **Dynamics**: 앱 실행 시 로그인하거나 로그아웃을 원하는 경우 발생.  
*  **Goals**: 로그인 및 로그아웃 기능을 구현함.  

### 2) Register
*  **Purpose**: 서비스 이용을 위한 새로운 사용자 계정 생성.  
*  **Approach**: 메일 인증을 필수 단계로 포함하여 신분 검증.  
*  **Dynamics**: 중복 가입 방지 및 약관 동의 후 데이터베이스에 저장.  
*  **Goals**: 회원가입 기능을 구현함.  

### 3) View My Profile
*  **Purpose**: 개인 정보와 서비스 이용 내역 확인 및 관리.  
*  **Approach**: 마이페이지를 통해 프로필, 학과, 매너 온도(신뢰도) 등 표시.  
*  **Dynamics**: 과거 동승 횟수나 절약한 택시비 합계 통계 제공.  
*  **Goals**: 내 정보를 확인할 수 있도록 함.  

### 4) Request Ride / Join Room
*  **Purpose**: 새로운 동승 그룹 생성 혹은 경로가 일치하는 방 합류.  
*  **Approach**: 앱 내 지도에서 목적지 설정 및 방 생성 알림 송출.  
*  **Dynamics**: 실시간 위치 정보(Map API)를 활용하여 주변 사용자에게 알림.  
*  **Goals**: 유사 목적지 사용자를 5분 이내에 매칭하고 성공률 제고.  

### 5) Send / Receive Message
*  **Purpose**: 매칭된 인원 간 구체적인 탑승 위치와 시간 조율.  
*  **Approach**: 소켓 통신을 이용한 실시간 채팅 기능 활용.  
*  **Dynamics**: 세부 약속 지점 선정 시 사용 (예: 영남대 정문 앞).  
*  **Goals**: 지연 시간 0.5초 미만의 원활한 소통 지원.  

### 6) Cancel Ride / Update Room Status
*  **Purpose**: 방 삭제/퇴장 관리 및 진행 상태의 실시간 공유.  
*  **Approach**: 상태 값(인원 충족, 운행 시작/종료 등) 자동 업데이트.  
*  **Dynamics**: 취소 시 페널티 연동 및 상태 변경에 따른 푸시 알림 발송.  
*  **Goals**: 예약 현황 정확성 유지 및 동승 프로세스 가시성 확보.  

### 7) Receive Match
*  **Purpose**: 최적의 동승 파트너 매칭을 사용자에게 통보.  
*  **Approach**: 경로 유사도가 높은 방 발견 시 자동 팝업 안내.  
*  **Dynamics**: 앱 비활성화 상태에서도 푸시 알림으로 매칭 여부 확인 가능.  
*  **Goals**: 수동 검색의 번거로움과 대기 시간 최소화.  

### 8) Verify / Return Location (Map API)
*  **Purpose**: 위치 기반 서비스 제공 및 목적지 간 거리 측정.  
*  **Approach**: Google Maps API 연동을 통한 실시간 좌표 수집 및 시각화.  
*  **Dynamics**: 경로 유사도 80% 이상인 경우에만 동승 목록으로 분류.  
*  **Goals**: 위치 오차 범위 5m 이내의 정확한 픽업 지점 설정.  

### 9) Request / Confirm Auth
*  **Purpose**: 외부 시스템을 통한 실제 대학생 여부 최종 확정.  
*  **Approach**: 학교 인증 메일로 발송된 일회용 코드로 외부 서버와 통신.  
*  **Dynamics**: 인증 성공 시에만 모든 서비스 기능 권한 부여.  
*  **Goals**: 허위 계정 방지 및 서비스 안전성 극대화.  

---

## 5. Problem statement

*  **실시간 데이터 처리의 어려움**: 여러 사용자의 위치 정보를 실시간 수집하고 경로 유사도를 계산하기 위한 서버 부하 관리 및 알고리즘 최적화 필요.  
*  **신뢰성 및 보안 문제**: 대학생 전용 서비스로서 철저한 학교 메일 인증 및 개인정보 노출 최소화 요구.  
*  **결제 및 정산의 투명성**: 외부 PG사 연동 시 결제 오류 방지 및 정확한 정산 금액 분배 설계 필요.  
* **비기능적 요구사항 (NFRs)**:
    *  **응답성**: 매칭 및 채팅 전송 지연 시간 1초 미만 유지.  
    *  **가용성**: 학기 중 피크 시간대 접속 급증 시에도 안정적인 운영 보장.  
    *  **사용성**: 직관적 UI/UX 제공.  

---

## 6. Glossary

*  **동승 (Carpooling)**: 같은 방향 승객들이 택시를 나누어 타고 비용을 분담하는 행위.  
*  **매너 온도 (Manner Temperature)**: 상호 평가를 통해 시각화한 사용자의 신뢰도 지표.  
*  **경로 유사도 (Route Similarity)**: 사용자의 출발/목적지 일치도를 나타내는 매칭 핵심 기준.  
*  **PG (Payment Gateway)**: 온라인 안전 결제 및 송금을 대행해 주는 서비스.  

---

## 7. References

*  **Google Maps Platform Documentation**: 실시간 위치 정보 및 경로 탐색 API 활용 방안 참조.  
