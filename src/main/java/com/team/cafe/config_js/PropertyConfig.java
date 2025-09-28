package com.team.cafe.config_js;

import com.team.cafe.kakaosj.KakaoController;
import com.team.cafe.kakaosj.KakaoService;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;


// properties, secret.properties 모두 로딩
@Configuration
@PropertySource("classpath:application-secret.properties")
public class PropertyConfig {


    @Value("${google.client-id}")
    private String clientId;

    @Value("${google.redirect-uri}")
    private String redirectUri;

    // 확인용
    @PostConstruct
    public void checkConfig() {
        System.out.println(">>> Google ClientId = " + clientId);
        System.out.println(">>> Google RedirectUri = " + redirectUri);
    }
}