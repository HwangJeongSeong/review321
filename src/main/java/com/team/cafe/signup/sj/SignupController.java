package com.team.cafe.signup.sj;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
@RequestMapping("/signup")
public class SignupController {

    private final SignupService signupService;

    // 회원가입 폼
    @GetMapping
    public String signupForm() {
        return "/signup/signup_form"; // templates/signup_form.html
    }

    // 회원가입 처리
    @PostMapping
    public String signup(@RequestParam String username,
                         @RequestParam String email,
                         @RequestParam String password,
                         @RequestParam String nickname,
                         @RequestParam String phone,
                         Model model) {

        String errorMessage = signupService.register(username, email, password, nickname, phone);

        if (errorMessage != null) {
            model.addAttribute("errorMessage", errorMessage);
            return "/signup/signup_form"; // 오류 발생 시 다시 가입 페이지
        }

        return "redirect:/user/login"; // 정상 가입 시 로그인 페이지로
    }


    // 아이디 중복 체크
    @GetMapping(value = "/check-username", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Boolean> checkUsername(@RequestParam String username) {
        boolean available = !signupService.existsByUsername(username);
        return ResponseEntity.ok(available);
    }

    // 이메일 중복 체크
    @GetMapping(value = "/check-email", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Boolean> checkEmail(@RequestParam String email) {
        boolean available = !signupService.existsByEmail(email);
        return ResponseEntity.ok(available);
    }

    // 닉네임 중복 체크
    @GetMapping(value = "/check-nickname", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Boolean> checkNickname(@RequestParam String nickname) {
        boolean available = !signupService.existsByNickname(nickname);
        return ResponseEntity.ok(available);
    }
}
