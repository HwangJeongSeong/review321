package com.team.cafe.review.external.kakao.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record KakaoAddressResponse(
        Meta meta,
        List<Document> documents
) {
    public record Meta(
            @JsonProperty("total_count") Integer totalCount,
            @JsonProperty("pageable_count") Integer pageableCount,
            @JsonProperty("is_end") Boolean isEnd
    ) {}

    public record Document(
            @JsonProperty("address_name") String addressName,
            @JsonProperty("x") String x, // 경도
            @JsonProperty("y") String y, // 위도
            Address address,
            @JsonProperty("road_address") RoadAddress roadAddress
    ) {
        public record Address(
                @JsonProperty("region_1depth_name") String region1,
                @JsonProperty("region_2depth_name") String region2,
                @JsonProperty("region_3depth_name") String region3
        ) {}
        public record RoadAddress(
                @JsonProperty("address_name") String addressName,
                @JsonProperty("building_name") String buildingName
        ) {}
    }
}