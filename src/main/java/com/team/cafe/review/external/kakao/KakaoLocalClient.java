package com.team.cafe.review.external.kakao;


import com.team.cafe.review.external.kakao.dto.KakaoAddressResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * KakaoLocalClient
 * - 카카오 로컬 API(주소 검색 서비스)를 호출하는 클라이언트 컴포넌트
 * - WebClient를 이용해서 외부 API에 HTTP 요청을 보냄
 * - 응답(JSON)을 KakaoAddressResponse DTO로 매핑
 */
@Component
@RequiredArgsConstructor
public class KakaoLocalClient {

    // WebClientConfig에서 Bean으로 등록한 WebClient 주입
    private final WebClient webClient;

    // application.properties(app.external.kakao.rest-api-key) 값 주입
    // 기본값은 "a2ed26f1ff8c196702de6b2f52470d05" (테스트 키)로 설정
    @Value("${app.external.kakao.rest-api-key:a2ed26f1ff8c196702de6b2f52470d05}")
    private String kakaoApiKey;

    /**
     * 주소 검색 요청
     * @param query 검색할 주소 문자열
     * @return Mono<KakaoAddressResponse> (비동기 반환)
     *
     * - WebClient는 reactive 타입(Mono/Flux)을 반환함
     * - 컨트롤러에서 block() 호출하거나 reactive 체인으로 이어서 사용 가능
     */
    public Mono<KakaoAddressResponse> searchAddress(String query) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .scheme("https") // HTTPS 프로토콜
                        .host("dapi.kakao.com") // 카카오 API 서버 도메인
                        .path("/v2/local/search/address.json") // 주소 검색 API 경로
                        .queryParam("query", query) // 요청 파라미터: 검색할 주소
                        .build())
                .header(HttpHeaders.AUTHORIZATION, "KakaoAK " + kakaoApiKey) // 인증 헤더 추가
                .retrieve() // 요청 보내고 응답 받기

                // ✅ 에러 상태 처리 (Spring 6/Boot 3.x 방식)
                // 응답 상태 코드가 4xx/5xx면 예외를 Mono error로 변환
                .onStatus(HttpStatusCode::isError,
                        resp -> resp.createException().flatMap(Mono::error))

                // 응답 JSON → KakaoAddressResponse DTO 변환
                .bodyToMono(KakaoAddressResponse.class);
    }
}
