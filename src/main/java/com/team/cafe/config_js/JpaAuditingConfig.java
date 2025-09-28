package com.team.cafe.config_js;

import java.util.Optional;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * JPA Auditing 활성화 및 감사 주체(Auditor) 결정.
 * BaseEntity의 @CreatedBy, @LastModifiedBy에 사용할 사용자 식별자를 제공한다.
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {

    /**
     * 현재 인증된 사용자의 username을 감사 주체로 반환.
     * 인증되지 않은 경우 "system"으로 기록.
     */
    @Bean
    public AuditorAware<String> auditorProvider() {
        return () -> {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
                return Optional.of("system");
            }
            return Optional.ofNullable(auth.getName());
        };
    }
}
