package com.team.cafe.user.sjhy;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.web.authentication.WebAuthenticationDetails;

/**
 * {@link WebAuthenticationDetails} 확장 클래스.
 * <p>
 * 다중 로그인 엔드포인트(/user/login, /business/login)를 구분하기 위해
 * 요청에 포함된 loginType 파라미터와 실제 요청 경로를 함께 저장한다.
 */
public class MultiEndpointWebAuthenticationDetails extends WebAuthenticationDetails {

    private final String loginType;
    private final String requestPath;

    public MultiEndpointWebAuthenticationDetails(HttpServletRequest request) {
        super(request);
        this.loginType = normalize(request.getParameter("loginType"));
        this.requestPath = request.getServletPath();
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public String getLoginType() {
        return loginType;
    }

    public String getRequestPath() {
        return requestPath;
    }
}