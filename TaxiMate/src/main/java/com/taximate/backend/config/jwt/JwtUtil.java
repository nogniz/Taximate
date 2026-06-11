package com.taximate.backend.config.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
public class JwtUtil {

    // 256비트 이상의 시크릿 키 (실제 배포 시 환경변수로 분리 권장)
    private static final String SECRET = "taximate-secret-key-must-be-at-least-256bits-long!";
    private static final long EXPIRATION_MS = 1000L * 60 * 60 * 24; // 24시간

    private final Key key = Keys.hmacShaKeyFor(SECRET.getBytes());

    // 토큰 생성 (subject: studentId)
    public String generateToken(String studentId) {
        return Jwts.builder()
                .setSubject(studentId)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_MS))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    // 토큰에서 studentId 추출
    public String getStudentId(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    // 토큰 유효성 검사
    public boolean isValid(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}
