package com.team.cafe.config_js;
// config 패키지 → 외부 API 호출 관련 설정을 보관하는 곳


import io.netty.channel.ChannelOption;
// Netty의 소켓 옵션을 설정하기 위한 클래스
import io.netty.handler.timeout.ReadTimeoutHandler;
// 읽기(Read) 작업에 대한 타임아웃을 설정하는 핸들러
import io.netty.handler.timeout.WriteTimeoutHandler;
// 쓰기(Write) 작업에 대한 타임아웃을 설정하는 핸들러
import org.springframework.beans.factory.annotation.Value;
// @Value → application.properties 또는 application.yml에 정의된 설정값을 주입받을 때 사용
import org.springframework.context.annotation.Bean;
// @Bean → 스프링 컨테이너에 객체를 등록해서 다른 클래스에서 주입 가능
import org.springframework.context.annotation.Configuration;
// @Configuration → 설정 클래스임을 알리는 어노테이션
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
// WebClient가 사용할 HTTP 커넥터 (Reactor Netty 기반)
import org.springframework.web.reactive.function.client.WebClient;
// Spring WebFlux의 비동기 HTTP 클라이언트 (외부 API 호출에 사용)
import reactor.netty.http.client.HttpClient;
// Reactor Netty의 HttpClient → 세부 옵션(타임아웃 등) 설정 가능
import java.util.concurrent.TimeUnit;
// 시간 단위를 밀리초, 초 등으로 지정하기 위해 사용


/**
 * WebClientConfig 클래스
 * - 외부 API 서버와 통신할 때 사용할 WebClient를 커스터마이징해서 Bean으로 등록
 * - HttpClient에 타임아웃 설정을 적용하여 안정적인 API 통신을 보장
 */
@Configuration
public class WebClientConfig {

    // application.properties(yml)에서 가져오는 값들
    @Value("${app.external.connect-timeout-ms}") private int connectMs; // 연결 타임아웃 (밀리초)
    @Value("${app.external.read-timeout-ms}") private int readMs;       // 응답(읽기) 타임아웃
    @Value("${app.external.write-timeout-ms}") private int writeMs;     // 요청(쓰기) 타임아웃

    /**
     * WebClient Bean 등록
     * - HttpClient를 커스터마이징해서 타임아웃 설정을 적용
     * - 외부 API 호출 시 주입받아서 사용 가능
     */
    @Bean
    public WebClient webClient() {
        // Netty 기반 HttpClient 생성 및 타임아웃 설정
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectMs) // 서버 연결 시도 제한 시간
                .doOnConnected(conn -> conn
                        // 서버 응답 대기 시간(read) 제한
                        .addHandlerLast(new ReadTimeoutHandler(readMs, TimeUnit.MILLISECONDS))
                        // 요청 데이터 전송(write) 제한
                        .addHandlerLast(new WriteTimeoutHandler(writeMs, TimeUnit.MILLISECONDS))
                );

        // WebClient를 빌드하여 Bean으로 등록
        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient)) // 위에서 만든 HttpClient 연결
                .build();
    }
}