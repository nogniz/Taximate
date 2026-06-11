package com.taximate.backend.service;

import com.taximate.backend.config.jwt.JwtUtil;
import com.taximate.backend.model.User;
import com.taximate.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final EmailService emailService;

    // 1. 회원가입 (이메일 인증 완료 후 호출)
    public User register(String studentId, String name, String email, String password) {
        // 이메일 인증 완료 여부 체크 (데모용 비활성화)
        // if (!emailService.isVerified(email)) {
        //     throw new IllegalStateException("이메일 인증이 완료되지 않았습니다. 먼저 인증을 진행해주세요.");
        // }
        // 중복 체크
        if (userRepository.existsByStudentId(studentId)) {
            throw new IllegalArgumentException("이미 등록된 학번입니다.");
        }
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("이미 등록된 이메일입니다.");
        }

        String encodedPassword = passwordEncoder.encode(password);
        User user = new User(studentId, name, email, encodedPassword);

        // 인증 완료 플래그 소모 (데모용 비활성화)
        // emailService.consumeVerified(email);

        return userRepository.save(user);
    }

    // 2. 로그인 → JWT 토큰 반환
    @Transactional(readOnly = true)
    public String login(String email, String password) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 이메일입니다."));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        return jwtUtil.generateToken(user.getStudentId());
    }

    // 3. 프로필 조회
    @Transactional(readOnly = true)
    public Map<String, Object> getProfile(String studentId) {
        User user = userRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        return Map.of(
                "studentId", user.getStudentId(),
                "name", user.getName(),
                "email", user.getEmail(),
                "isAuth", user.isAuth(),
                "mannerTemp", user.getMannerTemp()
        );
    }

    // 4. 매너 온도 갱신 (동승 완료 후 호출)
    public float updateManner(String studentId, float score) {
        User user = userRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        user.updateManner(score);
        userRepository.save(user);
        return user.getMannerTemp();
    }
}
