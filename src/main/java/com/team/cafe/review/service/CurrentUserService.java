package com.team.cafe.review.service;


import com.team.cafe.user.sjhy.SiteUser;
import com.team.cafe.user.sjhy.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@Transactional
public class CurrentUserService {

    private final UserRepository siteUserRepository;

    public CurrentUserService(UserRepository siteUserRepository) {
        this.siteUserRepository = siteUserRepository;
    }

    /** 현재 인증 객체 반환 (없으면 null) */
    private Authentication getAuthentication() {
        return SecurityContextHolder.getContext() != null
                ? SecurityContextHolder.getContext().getAuthentication()
                : null;
    }

    /** 현재 로그인한 사용자의 username (없으면 Optional.empty) */
    public Optional<String> getCurrentUsername() {
        Authentication auth = getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return Optional.empty();
        String name = auth.getName();
        if (name == null || "anonymousUser".equals(name)) return Optional.empty();
        return Optional.of(name);
    }

    /** 현재 로그인한 사용자 SiteUser (없으면 Optional.empty) */
    public Optional<SiteUser> getCurrentUser() {
        return getCurrentUsername().flatMap(siteUserRepository::findByUsername);
    }

    /** 현재 로그인한 사용자 SiteUser (없으면 IllegalStateException) */
    public SiteUser getCurrentUserOrThrow() {
        return getCurrentUser().orElseThrow(() ->
                new IllegalStateException("로그인이 필요합니다."));
    }
}

// 스피링 시큐리티를 통해 인증된 사용자 정보에 principal을 통해 접근 할 수 있지만 도메인 엔티티와 UserDetails 객체 분리로
// 스프릴 시큐리티 기본 princinpal은 UserDateils 이거나 OAuth2User 로 실제 DB 엔티티는 더 많은 필드 email, role, 상태 등을
// 갖고 있어서 컨트롤러에서 매번 principal에서 repository 에서 SiteUser을 변환하는 건 중복이 많다.
// 해당 클래스가 그 연결을 담당하면, 컨트롤러는 SiteUser만 바로 사용 가능
// username에서 조회 하다 id, email, authId등으로 바뀌어도 해당 클래스만 고치면 됨
// 즉 현재 유저에 대한 정보를 시큐리티를 통해 매번 받아 오는 것이 아닌 해당 클래스를 통해 저장하고 필요할 때 마다 꺼내옴