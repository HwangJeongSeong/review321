package com.team.cafe.user.sjhy;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
public class CustomAuthenticationProvider implements AuthenticationProvider {

    private final UserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;

    public CustomAuthenticationProvider(UserDetailsService userDetailsService,
                                        PasswordEncoder passwordEncoder,
                                        UserRepository userRepository) {
        this.userDetailsService = userDetailsService;
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String username = authentication.getName();
        String rawPassword = authentication.getCredentials().toString();

        String loginType = null;
        String requestPath = null;

        Object details = authentication.getDetails();
        if (details instanceof MultiEndpointWebAuthenticationDetails multiDetails) {
            loginType = multiDetails.getLoginType();
            requestPath = multiDetails.getRequestPath();
        } else {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            HttpServletRequest request = attributes != null ? attributes.getRequest() : null;
            loginType = request != null ? request.getParameter("loginType") : null;
            requestPath = request != null ? request.getServletPath() : null;
        }

        // 사용자 조회
        UserDetails user = userDetailsService.loadUserByUsername(username);
        SiteUser siteUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new BadCredentialsException("아이디 또는 비밀번호가 일치하지 않습니다."));

        // 비밀번호 체크
        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            throw new BadCredentialsException("아이디 또는 비밀번호가 일치하지 않습니다.");
        }

        // role vs loginType 체크
        String roleValue = siteUser.getRole();
        if (roleValue == null || roleValue.isBlank()) {
            roleValue = UserRole.USER.name();
        }

        String normalizedRole = roleValue.toUpperCase();
        if (!normalizedRole.startsWith("ROLE_")) {
            normalizedRole = "ROLE_" + normalizedRole;
        }

        boolean isBusiness = normalizedRole.equalsIgnoreCase(UserRole.BUSINESS.getValue());
        boolean isUser = normalizedRole.equalsIgnoreCase(UserRole.USER.getValue());

        String normalizedLoginType;
        if (loginType == null || loginType.isBlank()) {
            if (requestPath != null && requestPath.startsWith("/business")) {
                normalizedLoginType = UserRole.BUSINESS.name();
            } else {
                normalizedLoginType = UserRole.USER.name();
            }
        } else {
            normalizedLoginType = loginType.trim().toUpperCase();
        }

        if (UserRole.USER.name().equals(normalizedLoginType) && isBusiness) {
            throw new BadCredentialsException("사업자 계정은 일반 로그인 불가");
        }
        if (UserRole.BUSINESS.name().equals(normalizedLoginType) && isUser) {
            throw new BadCredentialsException("일반 계정은 사업자 로그인 불가");
        }

        // 성공 시 Authentication 리턴 (principal은 SiteUser로 유지하고 비밀번호는 컨텍스트에 저장하지 않음)
        UsernamePasswordAuthenticationToken authenticated =
                new UsernamePasswordAuthenticationToken(siteUser, null, user.getAuthorities());
        authenticated.setDetails(authentication.getDetails());
        return authenticated;
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
