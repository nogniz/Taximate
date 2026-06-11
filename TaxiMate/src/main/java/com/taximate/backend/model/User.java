package com.taximate.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class User {

    @Id
    @Column(nullable = false, unique = true)
    private String studentId;       // 학번 (PK, 예: 22112052)

    @Column(nullable = false)
    private String name;            // 본명

    @Column(nullable = false, unique = true)
    private String email;           // @yu.ac.kr 이메일

    @Column(nullable = false)
    private String password;        // BCrypt 암호화된 비밀번호

    @Column(nullable = false)
    private boolean isAuth = false; // 이메일 인증 완료 여부

    @Column(nullable = false)
    private float mannerTemp = 36.5f; // 매너 온도 (기본값 36.5)

    public User(String studentId, String name, String email, String password) {
        this.studentId = studentId;
        this.name = name;
        this.email = email;
        this.password = password;
        this.isAuth = true;   // 이메일 인증 후 가입하므로 가입 시 인증 완료 상태
        this.mannerTemp = 36.5f;
    }

    // 매너 온도 갱신
    public void updateManner(float score) {
        this.mannerTemp += score;
        if (this.mannerTemp < 0) this.mannerTemp = 0;
        if (this.mannerTemp > 100) this.mannerTemp = 100;
    }
}
