package com.team.cafe.user.sjhy;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import com.team.cafe.user.sjhy.UserService;

@Controller
@RequiredArgsConstructor
//@RequestMapping("/user")// hy
public class UserController {

    private final UserService userService;

    @GetMapping("/login/select")
    public String loginSelect() {
        return "/login/login_select";
    }

    @GetMapping("/user/login")
    public String kakaoLogin(Model model) {
        model.addAttribute("kakaoApiKey", kakaoApiKey);
        model.addAttribute("redirectUri", redirectUri);
        model.addAttribute("googleClientId", googleClientId);
        model.addAttribute("googleRedirectUri", googleRedirectUri);
        model.addAttribute("loginType", "USER");
        return "/login/login_form_user"; // login.html
    }

//    @GetMapping("/user/login/google")
//    public String googleLogin(Model model) {
//        model.addAttribute("googleClientId", googleClientId);
//        model.addAttribute("googleRedirectUri", googleRedirectUri);
//        model.addAttribute("loginType", "USER");
//        return "/login/login_form_user";
//    }

    @GetMapping("/business/login")
    public String businessLoginForm(Model model) {
        model.addAttribute("loginType", "BUSINESS");
        return "/login/login_form_business";
    }


    @Value("${kakao.api.key}")
    private String kakaoApiKey;

    @Value("${kakao.redirect-uri}")
    private String redirectUri;


    @Value("${google.client-id}")
    private String googleClientId;

    @Value("${google.client-secret}")
    private String googleClientSecret;

    @Value("${google.redirect-uri}")
    private String googleRedirectUri;
}