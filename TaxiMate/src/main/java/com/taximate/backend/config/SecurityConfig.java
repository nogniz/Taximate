package com.taximate.backend.config;

import com.taximate.backend.config.jwt.JwtAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // 인증 없이 접근 가능한 경로 (AntPathRequestMatcher 명시 - 멀티 서블릿 환경)
                .requestMatchers(
                    new AntPathRequestMatcher("/"),
                    new AntPathRequestMatcher("/index.html"),
                    new AntPathRequestMatcher("/**.html"),
                    new AntPathRequestMatcher("/**.js"),
                    new AntPathRequestMatcher("/**.css"),
                    new AntPathRequestMatcher("/api/auth/**"),
                    new AntPathRequestMatcher("/api/users/register"),
                    new AntPathRequestMatcher("/api/users/login"),
                    new AntPathRequestMatcher("/h2-console/**"),
                    new AntPathRequestMatcher("/ws/**")
                ).permitAll()
                // 나머지는 JWT 토큰 필요
                .anyRequest().authenticated()
            )
            // H2 콘솔을 iframe으로 열기 위해 필요
            .headers(h -> h.frameOptions(f -> f.disable()))
            // JWT 필터를 Spring Security 기본 필터 앞에 삽입
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
