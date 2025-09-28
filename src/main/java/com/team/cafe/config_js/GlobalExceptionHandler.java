package com.team.cafe.config_js;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * 전역 MVC 예외 처리:
 * - 사용자 입력 오류/상태 오류/권한 오류를 플래시 메시지로 표준화
 * - 가능하면 Referer(직전 페이지)로 리다이렉트, 없으면 홈("/")
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    // 400 계열: 잘못된 요청/상태
    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public String handleBadRequest(Exception ex, RedirectAttributes ra, HttpServletRequest req) {
        ra.addFlashAttribute("error", nonEmpty(ex.getMessage(), "요청을 처리할 수 없습니다."));
        return "redirect:" + getSafeReferer(req);
    }

    // 403 계열: 권한 문제
    @ExceptionHandler(SecurityException.class)
    public String handleSecurity(SecurityException ex, RedirectAttributes ra, HttpServletRequest req) {
        ra.addFlashAttribute("error", nonEmpty(ex.getMessage(), "권한이 없습니다."));
        return "redirect:" + getSafeReferer(req);
    }

    // 그 외 예기치 못한 오류
    @ExceptionHandler(Exception.class)
    public String handleUnknown(Exception ex, RedirectAttributes ra, HttpServletRequest req) {
        ra.addFlashAttribute("error", "알 수 없는 오류가 발생했습니다.");
        return "redirect:" + getSafeReferer(req);
    }

    private String nonEmpty(String s, String fallback) {
        return (s == null || s.isBlank()) ? fallback : s;
    }

    /**
     * Referer가 없으면 홈("/")로 보낸다.
     * (추후 필요 시 도메인 화이트리스트 검증 추가 가능)
     */
    private String getSafeReferer(HttpServletRequest req) {
        String ref = req.getHeader("Referer");
        return (ref == null || ref.isBlank()) ? "/" : ref;
        // 보안 강화가 필요하면 ref 검증 로직을 추가
    }
}
