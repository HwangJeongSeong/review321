package com.team.cafe.kakaosj;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@RequiredArgsConstructor
@Service
public class KakaoService {

    private final String KAKAO_REST_API_KEY = "a2ed26f1ff8c196702de6b2f52470d05";
    private final String REDIRECT_URI = "http://localhost:8080/user/kakao/callback";

    private final RestTemplate restTemplate = new RestTemplate();

    public String getAccessToken(String code) {
        String url = "https://kauth.kakao.com/oauth/token" +
                "?grant_type=authorization_code" +
                "&client_id=" + KAKAO_REST_API_KEY +
                "&redirect_uri=" + REDIRECT_URI +
                "&code=" + code;

        Map<String, Object> response = restTemplate.postForObject(url, null, Map.class);
        return (String) response.get("access_token");
    }

    public Map<String, Object> getUserInfo(String accessToken) {
        String url = "https://kapi.kakao.com/v2/user/me";
        return restTemplate.getForObject(url + "?access_token=" + accessToken, Map.class);
    }
}
