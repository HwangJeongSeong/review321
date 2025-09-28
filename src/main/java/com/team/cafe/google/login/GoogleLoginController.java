package com.team.cafe.google.login;

import com.team.cafe.user.sjhy.SiteUser;
import com.team.cafe.user.sjhy.UserSecurityService;
import com.team.cafe.user.sjhy.UserService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

@Controller
@RequiredArgsConstructor
@RequestMapping("/user/google")
public class GoogleLoginController {
    private final GoogleLoginService googleLoginService;
    private final UserService userService;
    private final UserSecurityService userSecurityService;

    @GetMapping("/login")
    public String googleLogin() {
        // 로그인 URL은 Service에서 생성
        return "redirect:" + googleLoginService.getLoginUrl();
    }

    @GetMapping("/callback")
    public String googleCallback(@RequestParam String code, HttpSession session) {
        // 1. 토큰 발급
        Map<String, Object> tokenResponse = googleLoginService.getToken(code);
        String idToken = (String) tokenResponse.get("id_token");

        // 2. 사용자 정보
        Map<String, Object> googleUser = googleLoginService.getUserInfo(idToken);
        String googleId = (String) googleUser.get("sub");
        String email = (String) googleUser.get("email");
        String name = (String) googleUser.get("name");

        // 3. DB 저장 or 조회
        SiteUser siteUser = userService.registerOrGetGoogleUser(googleId, email, name);

        // 4. SecurityContext 저장
        UserDetails userDetails = userSecurityService.loadUserByUsername(siteUser.getUsername());
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContext securityContext = SecurityContextHolder.getContext();
        securityContext.setAuthentication(auth);

        session.setAttribute("SPRING_SECURITY_CONTEXT", securityContext);

        return "redirect:/";
    }
}