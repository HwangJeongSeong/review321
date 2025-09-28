package com.team.cafe.config_js;
// config 패키지 → 웹 MVC 관련 설정 파일 모아두는 곳


import org.springframework.beans.factory.annotation.Value;
// @Value → application.properties(yml)에 정의된 값을 읽어서 변수에 주입
import org.springframework.context.annotation.Configuration;
// @Configuration → 스프링 설정 클래스임을 알림
import org.springframework.web.servlet.config.annotation.*;
// WebMvcConfigurer, ResourceHandlerRegistry 등을 사용하기 위해 import


/**
 * WebMvcConfig 클래스
 * - 스프링 MVC(Web) 관련 설정을 커스터마이징하는 클래스
 * - 여기서는 업로드된 파일을 정적 리소스로 제공하기 위한 설정을 추가
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    // application.properties에 설정된 업로드 경로를 가져옴
    // 예: app.upload.dir=C:/uploads/
    @Value("${app.upload.dir}")
    private String uploadDir;

    // URL 접두사 (ex. /uploads, /images/review 등)
    @Value("${app.upload.url-prefix:/uploads}")
    private String urlPrefix;

    /**
     * 정적 리소스 핸들러 추가 메서드
     * - 업로드된 파일이 저장된 로컬 디렉토리를 URL로 접근 가능하게 매핑
     * - application.properties 의 app.upload.url-prefix 값을 기반으로 동적으로 매핑
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 경로 끝에 "/"가 없으면 자동으로 붙여줌
        String base = uploadDir.endsWith("/") ? uploadDir : uploadDir + "/";

        // 접두사도 "/"로 시작하도록 보정
        String prefix = urlPrefix.startsWith("/") ? urlPrefix : "/" + urlPrefix;

        registry.addResourceHandler(prefix + "/**")
                // 브라우저에서 요청할 URL 패턴 (예: http://localhost:8080/images/review/파일.jpg)
                .addResourceLocations("file:" + base);
        // 실제 파일이 저장된 위치 (로컬 디렉토리)
        // "file:" 접두어 → 로컬 파일 시스템 경로임을 명시
        registry.addResourceHandler("/cafes/**")
                .addResourceLocations("file:" + base + "cafes/");

    }
}