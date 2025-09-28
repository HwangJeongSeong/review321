package com.team.cafe.businessuser.sj;

import groovy.util.logging.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;


@Controller
@RequestMapping("/businessuser")
@RequiredArgsConstructor
public class BusinessUserController {

    private final BusinessUserService businessUserService;

    /* ---------- 회원가입 화면 ---------- */
    @GetMapping("/register")
    public String showRegisterForm(@RequestParam(value = "error", required = false) String error,
                                   @RequestParam(value = "success", required = false) String success,
                                   Model model) {
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", new BusinessUserDto());
        }
        model.addAttribute("error", error);
        model.addAttribute("success", success);
        return "signup/business-signup";
    }

    /* ---------- 회원가입 처리 ---------- */
    @PostMapping("/register")
    public String register(@ModelAttribute("form") BusinessUserDto form,
                           @RequestParam(value = "phone",  required = false) String phone,   // hidden phone
                           @RequestParam(value = "phone1", required = false) String phone1,  // 분할 입력
                           @RequestParam(value = "phone2", required = false) String phone2,
                           @RequestParam(value = "phone3", required = false) String phone3,
                           RedirectAttributes ra) {
        // 1) 대표자 전화 보정: representativePhone이 비어 있으면 phone/phone1~3에서 합성
        if (isBlank(form.getRepresentativePhone())) {
            String normalized = normalizePhone(phone, phone1, phone2, phone3);
            form.setRepresentativePhone(normalized);
        }

        // 2) 대표자 이메일 보정: 입력 필드가 없으므로 회원 이메일로 대체
        if (isBlank(form.getRepresentativeEmail())) {
            form.setRepresentativeEmail(form.getEmail());
        }


        try {
            businessUserService.register(form);
            ra.addAttribute("success", 1);
            return "redirect:/user/login";
        } catch (DuplicateBusinessNumberException e) {
            ra.addAttribute("error", "businessNumberExists");
        } catch (DuplicateUsernameException e) {
            ra.addAttribute("error", "usernameExists");
        } catch (DuplicateEmailException e) {
            ra.addAttribute("error", "emailExists");
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            ra.addAttribute("error", "db");
        } catch (Exception e) {
            ra.addAttribute("error", "unknown");
        }
        ra.addFlashAttribute("form", form);
        return "redirect:/businessuser/register";
    }

    private static boolean isBlank(String s){
        return s == null || s.trim().isEmpty();
    }

    private static String normalizePhone(String phone, String p1, String p2, String p3){
        if (!isBlank(phone)) return phone.trim();
        String a = p1 == null ? "" : p1.trim();
        String b = p2 == null ? "" : p2.trim();
        String c = p3 == null ? "" : p3.trim();
        if (!a.isEmpty() && !b.isEmpty() && !c.isEmpty()) return a + "-" + b + "-" + c;
        return null; // 없으면 null 저장(검증 원하면 여기서 예외 던져도 됨)
    }



    /* ---------- 실시간 중복 체크 API ---------- */
    @GetMapping(value = "/check-username", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Boolean> checkUsername(@RequestParam String username) {
        boolean available = !businessUserService.existsByUsername(username);
        return ResponseEntity.ok(available);
    }

    @GetMapping(value = "/check-email", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Boolean> checkEmail(@RequestParam String email) {
        boolean available = !businessUserService.existsByEmail(email);
        return ResponseEntity.ok(available);
    }

    @GetMapping(value = "/check-nickname", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Boolean> checkNickname(@RequestParam String nickname) {
        boolean available = !businessUserService.existsByNickname(nickname);
        return ResponseEntity.ok(available);
    }
}
