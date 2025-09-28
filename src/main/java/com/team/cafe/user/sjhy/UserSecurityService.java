package com.team.cafe.user.sjhy;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component("securityService")
@RequiredArgsConstructor
@Service
public class UserSecurityService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        Optional<SiteUser> _siteUser = this.userRepository.findByUsername(username);
        if (_siteUser.isEmpty()) {
            throw new UsernameNotFoundException("사용자를 찾을수 없습니다.");
        }
        SiteUser siteUser = _siteUser.get();

        List<GrantedAuthority> authorities = new ArrayList<>();

        String role = siteUser.getRole();

        if (role == null || role.isBlank()) {
            role = UserRole.USER.name();
        }

        String normalizedRole = role.toUpperCase();
        if (!normalizedRole.startsWith("ROLE_")) {
            normalizedRole = "ROLE_" + normalizedRole;
        }

        // UserRole enum에 정의된 값만 허용, 나머지는 기본 USER 권한 부여
        if (!normalizedRole.equals(UserRole.BUSINESS.getValue()) &&
                !normalizedRole.equals(UserRole.USER.getValue())) {
            normalizedRole = UserRole.USER.getValue();
        }

        authorities.add(new SimpleGrantedAuthority(normalizedRole));
        // principal을 SiteUser로 사용

        // 카카오 로그인 위한 임시 데이터
//        return User.builder()
//                .username(siteUser.getUsername())
//                .password(siteUser.getPassword() != null ? siteUser.getPassword() : "{noop}kakao_user")
//                .authorities(authorities)
//                .accountExpired(false)       // 계정 만료 여부
//                .accountLocked(false)        // 계정 잠금 여부
//                .credentialsExpired(false)   // 비밀번호 만료 여부
//                .disabled(false)             // 활성화 여부
//                .build();

        // 카카오 api는 비밀번호 null상태로 데이터를 받아오기 때문에
        // null 값일때 임시 데이터를 넣어주기로,
        return new User(siteUser.getUsername(),
                siteUser.getPassword() != null ? siteUser.getPassword() : "{noop}kakao_user",
                authorities);
    }

    // ===================================================
    // 화면에서 nickname 가져올 때 사용
    // ===================================================
    public String getNickname() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        // 로그인하지 않은 상태면 "게스트" 반환
        if (auth == null || auth instanceof AnonymousAuthenticationToken) {
            return "게스트";
        }

        String username = auth.getName(); // 로그인 시 사용한 아이디(username)

        // DB 조회 후 nickname 반환
        return userRepository.findByUsername(username)
                .map(SiteUser::getNickname)
                .orElse("알 수 없음");
    }
}