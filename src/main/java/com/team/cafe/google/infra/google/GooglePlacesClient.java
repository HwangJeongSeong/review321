package com.team.cafe.google.infra.google;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Component
public class GooglePlacesClient {

    private final RestClient client;

    private static final String FIELD_MASK = String.join(",",
            "places.id",
            "places.displayName",
            "places.formattedAddress",
            "places.userRatingCount",
            "places.businessStatus",
            "places.photos"
    );

    public GooglePlacesClient(@Value("${google.apiKey}") String apiKey) {
        this.client = RestClient.builder()
                .baseUrl("https://places.googleapis.com/v1")
                .defaultHeader("X-Goog-Api-Key", apiKey)
                .defaultHeader("X-Goog-FieldMask", FIELD_MASK)
                .build();
    }

    // Text Search로 "대전 카페" 검색
    public NearbyResponse searchTextCafes(String query) {
        Map<String, Object> body = Map.of(
                "textQuery", query,
                "languageCode", "ko",
                "regionCode", "KR"
        );

        return client.post()
                .uri("/places:searchText")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(NearbyResponse.class);
    }
    //searchNearbyCafes 메서드
    public NearbyResponse searchNearbyCafes(double lat, double lng, int radiusMeters, int maxCount) {
        if (maxCount < 1 || maxCount > 20) maxCount = 20; // API 제약
        Map<String, Object> body = Map.of(
                "includedTypes", List.of("cafe"),
                "maxResultCount", maxCount,
                "locationRestriction", Map.of(
                        "circle", Map.of(
                                "center", Map.of("latitude", lat, "longitude", lng),
                                "radius", (double) radiusMeters
                        )
                ),
                "languageCode", "ko",
                "regionCode", "KR"
        );

        return client.post()
                .uri("/places:searchNearby")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(NearbyResponse.class);
    }
    
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class NearbyResponse {
        private List<Place> places;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Place {
        private String id;
        private TextVal displayName;
        private String formattedAddress;
        private String businessStatus;
        private List<Photo> photos;
    }

    @Data @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TextVal { private String text; }

    @Data @JsonIgnoreProperties(ignoreUnknown = true)
    public static class LatLng { private Double latitude; private Double longitude; }

    @Data @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Photo { private String name; private Integer widthPx; private Integer heightPx; }
}