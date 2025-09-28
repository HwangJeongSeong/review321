package com.team.cafe.google.login;


import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class GoogleLoginService {

    @Value("${google.client-id}")
    private String CLIENT_ID;

    @Value("${google.client-secret}")
    private String CLIENT_SECRET;

    @Value("${google.redirect-uri}")
    private String REDIRECT_URI;
    private final RestTemplate restTemplate = new RestTemplate();

    public String getLoginUrl() {
        return "https://accounts.google.com/o/oauth2/v2/auth" +
                "?client_id=" + CLIENT_ID +
                "&redirect_uri=" + REDIRECT_URI +
                "&response_type=code" +
                "&scope=openid%20email%20profile";
    }


    public Map<String, Object> getToken(String code) {
        String url = "https://oauth2.googleapis.com/token" +
                "?code=" + code +
                "&client_id=" + CLIENT_ID +
                "&client_secret=" + CLIENT_SECRET +
                "&redirect_uri=" + REDIRECT_URI +
                "&grant_type=authorization_code";

        return restTemplate.postForObject(url, null, Map.class);
    }

    public Map<String, Object> getUserInfo(String idToken) {
        String url = "https://oauth2.googleapis.com/tokeninfo?id_token=" + idToken;
        return restTemplate.getForObject(url, Map.class);
    }
}