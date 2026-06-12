package com.taximate.backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    // 메모리에 인증번호와 만료시간(Epoch Milli)을 함께 저장하는 맵
    // Key: 이메일주소, Value: [인증번호, 만료시간]
    private final Map<String, String[]> authCodeMap = new ConcurrentHashMap<>();
    private static final long AUTH_CODE_EXPIRATION_TIME = 3 * 60 * 1000; // 3분 (밀리초)

    // 인증 완료된 이메일 목록 (회원가입 허가 여부 확인용, 가입 완료 시 소멸)
    private final Map<String, Boolean> verifiedEmails = new ConcurrentHashMap<>();

    // 1. 6자리 인증 번호 생성 및 발송 (시연용: 고정 코드 123456)
    public void sendVerificationEmail(String email) {
        // 시연용 고정 인증코드
        String authCode = "123456";
        long expireAt = System.currentTimeMillis() + AUTH_CODE_EXPIRATION_TIME;

        // 메모리에 적재
        authCodeMap.put(email, new String[]{authCode, String.valueOf(expireAt)});
    }

    // 2. 유저가 입력한 인증 번호 검증
    public boolean verifyCode(String email, String inputCode) {
        if (!authCodeMap.containsKey(email)) return false;

        String[] authData = authCodeMap.get(email);
        String savedCode = authData[0];
        long expireAt = Long.parseLong(authData[1]);

        // 시간 만료 체크
        if (System.currentTimeMillis() > expireAt) {
            authCodeMap.remove(email); // 만료된 데이터 삭제
            return false;
        }

        // 일치 여부 리턴
        if (savedCode.equals(inputCode)) {
            authCodeMap.remove(email); // 인증 성공 시 세션 폐기
            verifiedEmails.put(email, true); // 회원가입 허가 목록에 등록
            return true;
        }

        return false;
    }

    // 3. 이메일 인증 완료 여부 조회 (UserService에서 회원가입 전 체크)
    public boolean isVerified(String email) {
        return verifiedEmails.getOrDefault(email, false);
    }

    // 4. 인증 완료 플래그 소멸 (회원가입 완료 후 호출 → 재가입 방지)
    public void consumeVerified(String email) {
        verifiedEmails.remove(email);
    }
}