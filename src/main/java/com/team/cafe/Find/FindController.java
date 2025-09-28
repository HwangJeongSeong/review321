package com.team.cafe.Find;

import com.team.cafe.user.sjhy.SiteUser;
import com.team.cafe.user.sjhy.UserRepository;
import com.team.cafe.user.sjhy.UserService;
import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequiredArgsConstructor
public class FindController {

    private final FindService findService;

    // 아이디 찾기 페이지
    @GetMapping("/user/findId")
    public String findId() {
        return "login/find_id";
    }

    // 비밀번호 찾기 페이지
    @GetMapping("/user/findPassword")
    public String findPassword() {
        return "login/find_password";
    }

    // 아이디 찾기 인증메일 발송
    @PostMapping("/user/findId")
    public String searchId(Model model, @RequestParam String email) {

        try {
            findService.sendVerificationCode(email);

            // 이메일을 세션에 저장 (재전송 기능 만들때)
//            session.setAttribute("verificationEmail", email);

            model.addAttribute("email", email);
            // 인증번호 입력창 보여줄지 여부
            model.addAttribute("showVerification", true);
            // 인증 메일 발송 알림
            model.addAttribute("showAlert", true);
        } catch (RuntimeException | MessagingException e) {
            model.addAttribute("emailError",true);

        }
        return "login/find_id";
    }

    // 아이디 찾기 인증메일 재전송
//    @PostMapping("/user/resendEmail")
//    public String resendEmail(Model model, HttpSession session) {
//        String email = (String) session.getAttribute("verificationEmail");
//
//        if (email != null) {
//            try {
//                findService.sendVerificationCode(email);
//
//                model.addAttribute("email", email);
//                // 인증번호 입력창 보여줄지 여부
//                model.addAttribute("showVerification", true);
//                // 인증 메일 발송 알림
//                model.addAttribute("showAlert", true);
//            } catch (RuntimeException e) {
//                model.addAttribute("emailError",
//                        e.getMessage());
//            }
//        } else {
//            model.addAttribute("emailError", "이메일 정보가 없습니다. 다시 입력해주세요.");
//        }
//        return "login/find_id";
//    }

    // 인증번호 확인 및 아이디 출력
    @PostMapping("/user/verifyIdCode")
    public String verifyCode(@RequestParam String email,
                             @RequestParam String code,
                             Model model) {
        try {
            String username = findService.verifyCodeAndFindId(email, code);
            model.addAttribute("username", username);
            model.addAttribute("idFound", true);
            model.addAttribute("showVerification", true);
            model.addAttribute("email", email);
            return "login/find_id";
        } catch (RuntimeException e) {
            model.addAttribute("codeError", e.getMessage());
            model.addAttribute("email", email);
            model.addAttribute("showVerification", true); // 다시 인증번호 입력 폼 표시
            return "login/find_id";
        }
    }


    // 비밀번호 찾기 인증메일 발송
    @PostMapping("/user/findPassword")
    public String searchUser(Model model, @RequestParam String username, @RequestParam String email) {

        try {
            findService.sendVerificationCodePW(username, email);
            model.addAttribute("username", username);
            model.addAttribute("email", email);
            // 임시 비밀번호 입력창 보여줄지 여부
            model.addAttribute("showVerification", true);
            // 인증 메일 발송 알림
            model.addAttribute("showAlert", true);
        } catch (RuntimeException | MessagingException e) {
            model.addAttribute("emailError", true);
        }
        return "login/find_password";
    }

    // 인증번호 확인 및 비밀번호 재설정 페이지 이동
    @PostMapping("/user/verifyPWCode")
    public String verifyPasswordCode(@RequestParam String username,
                                     @RequestParam String email,
                                     @RequestParam String code,
                                     Model model,
                                     HttpSession session) {
        try {
            findService.verifyCodeAndFindPW(email, code);
//            model.addAttribute("username", username);
            session.setAttribute("verifiedUsername", username);
            return "redirect:/user/modifyPassword";
        } catch (RuntimeException e) {
            model.addAttribute("codeError", true);
            model.addAttribute("username", username);
            model.addAttribute("email", email);
            model.addAttribute("showVerification", true); // 다시 인증번호 입력 폼 표시
            return "login/find_password";
        }
    }

    // 비밀번호 재설정 페이지 (GET 요청)
    @GetMapping("/user/modifyPassword")
    public String modifyPasswordForm(Model model, HttpSession session) {
        String username = (String) session.getAttribute("verifiedUsername");
        if (username == null) {
            // 세션에 username이 없으면, 비밀번호 찾기 페이지로 돌려보냄
            return "redirect:/user/findPassword";
        }
        model.addAttribute("username", username);
        return "login/modify_password";
    }


    @PostMapping("/user/modifyPassword")
    public String modifyPassword(@RequestParam String password,
                                 @RequestParam String passwordConfirm,
                                 Model model,
                                 HttpSession session) {
        String username = (String) session.getAttribute("verifiedUsername");
        ////css 위해 잠시 주석 !!!!!!!!!!!!!!1 꼭 풀기!!!!!!!!!!!!!!
//        if (username == null) {
//            // 세션에 username이 없으면, 비정상적인 접근이므로 리다이렉트
//            // 이 기능이 없으면 url접속만으로 마음대로 다른사람의 비밀번호를 바꿀 수 있음.
//            return "redirect:/user/findPassword";
//        }

        try {
            if (!password.equals(passwordConfirm)) {
                model.addAttribute("errorMessage", "새 비밀번호와 비밀번호 확인이 일치하지 않습니다.");
                model.addAttribute("username", username); // 오류 발생 시 username을 다시 추가
                return "login/modify_password";
            }
            findService.updatePassword(username, password);
            session.removeAttribute("verifiedUsername");
            model.addAttribute("successMessage", "비밀번호가 성공적으로 변경되었습니다. 다시 로그인 해주세요.");
            return "redirect:/user/login"; // PRG 패턴 적용
        } catch (RuntimeException e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("username", username); // 오류 발생 시 username을 다시 추가
            return "login/modify_password";
        }
    }
}

