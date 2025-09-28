package com.team.cafe.review.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * ReviewCreateRequest
 * - 리뷰 작성 시 클라이언트(폼/요청) → 서버(Controller)로 전달되는 데이터 객체
 * - @Valid 와 함께 사용하여 입력값 검증(Validation) 수행
 * - record: 불변(immutable) 데이터 전송용 객체. Getter/Setter 불필요, 생성자/equals 자동 생성됨.
 */
public record ReviewCreateRequest (

        @NotNull // 반드시 있어야 함 (어떤 카페에 대한 리뷰인지)
        Long cafeId,

        @NotNull // 내용은 필수
        @Size(min = 5, max = 4000) // 최소 5자, 최대 4000자 → 너무 짧거나 긴 리뷰 방지
        String content,

        @NotNull // 별점은 필수
        @DecimalMin("0.0") // 최소 0.0
        @DecimalMax("5.0") // 최대 5.0
        Double rating

) {
    // ⚠️ 별점은 0.5 단위 제약(0.0, 0.5, 1.0, …, 5.0)은 표준 애노테이션만으로 검증 불가
    // 서비스 로직이나 커스텀 Validator를 만들어 체크하는 게 안전
}
