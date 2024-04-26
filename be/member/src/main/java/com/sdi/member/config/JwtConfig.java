package com.sdi.member.config;

import com.sdi.member.jwt.AuthTokenProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JwtConfig {
    @Value("${JWT_SECRET_ACCESS}")
    private String accessKey;
    @Value("${JWT_SECRET_REFRESH}")
    private String refreshKey;

    @Bean
    public AuthTokenProvider jwtProvider() {
        return new AuthTokenProvider(accessKey, refreshKey);
    }
}
