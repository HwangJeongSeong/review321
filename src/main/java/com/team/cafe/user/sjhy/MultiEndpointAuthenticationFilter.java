package com.team.cafe.user.sjhy;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

/**
 * 두 종류의 로그인 엔드포인트(/user/login, /business/login)를 모두 처리하기 위한 커스텀 필터.
 * <p>
 * Spring Security의 기본 UsernamePasswordAuthenticationFilter는 하나의 URL만 처리하도록 설계되어 있기 때문에
 * 사업자 로그인 전용 엔드포인트를 추가로 허용하기 위해 RequestMatcher를 OR 조합으로 설정한다.
 */
public class MultiEndpointAuthenticationFilter extends UsernamePasswordAuthenticationFilter {

    private static final RequestMatcher MULTI_ENDPOINT_MATCHER = new OrRequestMatcher(
            new AntPathRequestMatcher("/user/login", "POST"),
            new AntPathRequestMatcher("/business/login", "POST")
    );

    public MultiEndpointAuthenticationFilter() {
        setRequiresAuthenticationRequestMatcher(MULTI_ENDPOINT_MATCHER);
        setUsernameParameter("username");
        setPasswordParameter("password");
    }

    @Override
    protected boolean requiresAuthentication(HttpServletRequest request, HttpServletResponse response) {
        return MULTI_ENDPOINT_MATCHER.matches(request);
    }

    @Override
    protected void setDetails(HttpServletRequest request,
                              org.springframework.security.authentication.UsernamePasswordAuthenticationToken authRequest) {
        authRequest.setDetails(new MultiEndpointWebAuthenticationDetails(request));
    }
}

