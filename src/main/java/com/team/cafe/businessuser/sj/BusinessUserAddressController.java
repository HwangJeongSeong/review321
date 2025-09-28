package com.team.cafe.businessuser.sj;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@RequestMapping("/businessuser/address")
public class BusinessUserAddressController {

    // 발급받은 실제 서비스용 주소검색 API 키
    private final String API_KEY = "U01TX0FVVEgyMDI1MDkwMzE3MTQ0MDExNjE0NjI=";

    /**
     * 주소 검색 API 호출
     * @param keyword 검색할 주소 키워드
     * @param currentPage 페이지 번호 (기본 1)
     * @param countPerPage 페이지당 검색 결과 수 (기본 10)
     * @return 외부 API 결과 JSON 그대로 반환
     */
    @GetMapping("/search")
    public ResponseEntity<String> searchAddress(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "1") int currentPage,
            @RequestParam(defaultValue = "10") int countPerPage) {

        // 외부 API URL 생성
        String url = UriComponentsBuilder
                .fromUriString("https://api.odcloud.kr/api/nts-address/v1/search")
                .queryParam("serviceKey", API_KEY)
                .queryParam("keyword", keyword)
                .queryParam("currentPage", currentPage)
                .queryParam("countPerPage", countPerPage)
                .queryParam("type", "json")
                .encode()
                .toUriString();

        try {
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

            // API 호출 결과 그대로 반환
            return ResponseEntity.ok(response.getBody());

        } catch (Exception e) {
            // 호출 실패 시 에러 메시지 반환
            return ResponseEntity.status(500)
                    .body("{\"error\":\"주소 검색 API 호출 실패\", \"message\":\"" + e.getMessage() + "\"}");
        }
    }
}
