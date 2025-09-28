package com.team.cafe.user.sjhy;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
public class CustomAuthenticationFailureHandler implements AuthenticationFailureHandler {

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException, ServletException {
        String loginType = request.getParameter("loginType");
        String requestPath = request.getServletPath();
        String errorMessage = "아이디 또는 비밀번호가 일치하지 않습니다.";
        String redirectUrl = "";
        // URL 인코딩을 적용한 메시지 변수 사용
//        String encodedMessage = URLEncoder.encode(errorMessage, StandardCharsets.UTF_8.toString());

        // 요청에 loginType이 BUSINESS로 명시된 경우 사업자 로그인 페이지로 리다이렉션
        boolean isBusinessRequest = "BUSINESS".equals(loginType)
                || (loginType == null && requestPath != null && requestPath.startsWith("/business"));
//
//        if (isBusinessRequest) {
//            response.sendRedirect("/business/login?error&message=" + encodedMessage);
//        } else {
//            // 그 외의 경우 일반 사용자 로그인 페이지로 리다이렉션
//            response.sendRedirect("/user/login?error&message=" + encodedMessage);
//        }
        // 예외 메시지를 통해 특정 오류를 식별
        if (exception != null && exception.getMessage() != null) {
            if (exception.getMessage().contains("사업자 계정은 일반 로그인 불가")) {
                // 일반 로그인 폼에서 사업자 계정으로 로그인 시도
                redirectUrl = "/user/login?mismatch=business";
            } else if (exception.getMessage().contains("일반 계정은 사업자 로그인 불가")) {
                // 사업자 로그인 폼에서 일반 계정으로 로그인 시도
                redirectUrl = "/business/login?mismatch=user";
            } else {
                // 일반적인 아이디/비밀번호 불일치 오류
                String encodedMessage = URLEncoder.encode(errorMessage, StandardCharsets.UTF_8);
                if (isBusinessRequest) {
                    redirectUrl = "/business/login?error&message=" + encodedMessage;
                } else {
                    redirectUrl = "/user/login?error&message=" + encodedMessage;
                }
            }
        } else {
            // 알 수 없는 오류
            String encodedMessage = URLEncoder.encode(errorMessage, StandardCharsets.UTF_8);
            if (isBusinessRequest) {
                redirectUrl = "/business/login?error&message=" + encodedMessage;
            } else {
                redirectUrl = "/user/login?error&message=" + encodedMessage;
            }
        }

        response.sendRedirect(redirectUrl);
    }
}
