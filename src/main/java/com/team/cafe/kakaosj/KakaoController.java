package com.team.cafe.kakaosj;

import com.team.cafe.user.sjhy.SiteUser;
import com.team.cafe.user.sjhy.UserRole;
import com.team.cafe.user.sjhy.UserSecurityService;
import com.team.cafe.user.sjhy.UserService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
@RequiredArgsConstructor
@RequestMapping("/user/kakao")
public class KakaoController {

    private static final Logger logger = LoggerFactory.getLogger(KakaoController.class);

    private final KakaoService kakaoService;
    private final UserService userService;
    private final UserSecurityService userSecurityService;

    @GetMapping("/callback")
    public String kakaoCallback(@RequestParam String code, HttpSession session) {

        // 1. AccessToken 발급
        String accessToken = kakaoService.getAccessToken(code);

        // 2. 사용자 정보 가져오기
        Map<String, Object> kakaoUserInfo = kakaoService.getUserInfo(accessToken);
        Map<String, Object> kakaoAccount = (Map<String, Object>) kakaoUserInfo.get("kakao_account");

        String kakaoId = String.valueOf(kakaoUserInfo.get("id"));
        String email = (String) kakaoAccount.get("email");
        String nickname = (String)((Map<String, Object>)kakaoAccount.get("profile")).get("nickname");

        // 이메일이 없을 경우 임시 이메일 생성
        if (email == null || email.isBlank()) {
            email = "kakao_" + kakaoId + "@noemail.com";
        }

        // 3. DB 회원 조회/등록
        SiteUser siteUser = userService.registerOrGetKakaoUser(kakaoId, email, nickname);

        // SiteUser(Entity)가 아닌 userDetails(security)기반으로 인증 여부를 판단하게 하기 위함.
         UserDetails userDetails = userSecurityService.loadUserByUsername(siteUser.getUsername());

         //카카오 로그인 상태 콘솔 확인용
         //logger.info("kakaoCallback 1 - {} : {}", user.getUsername(), user.getAuthorities().toString());

        // 4. Spring Security 로그인 처리
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
//        SecurityContextHolder.getContext().setAuthentication(auth);
        SecurityContext securityContext =  SecurityContextHolder.getContext();
        securityContext.setAuthentication(auth);

        session.setAttribute("SPRING_SECURITY_CONTEXT",securityContext);

        // 카카오 로그인 상태 콘솔 확인용
        //logger.info("kakaoCallback 2 - {} : {}", auth.getPrincipal().toString(), auth.getAuthorities().toString());

        // 5. 메인 페이지로 리다이렉트
        return "redirect:/";
//        return "redirect:/login/login_form";
    }

}
