package com.team.cafe.google.api;// com.team.cafe.domain.CafeHarvestService



import com.team.cafe.google.infra.google.GooglePlacesClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CafeHarvestService {

    private final GooglePlacesClient google;

    /**
     * 대전 주요 5개 권역(서구/유성구/중구/동구/대덕구) 중심을 타일처럼 순회 수집
     */
    public List<GooglePlacesClient.Place> harvestDaejeonCafes() {
        // 대전 권역 중심 좌표들 (대략적인 중심점)
        record C(double lat, double lng, int radius) {}
        List<C> centers = List.of(
                new C(36.3504, 127.3845, 4000), // 서구(둔산)
                new C(36.3600, 127.3470, 4000), // 유성구
                new C(36.3250, 127.4210, 4000), // 중구
                new C(36.3500, 127.4500, 4000), // 동구
                new C(36.4300, 127.4400, 4000)  // 대덕구
        );

        Map<String, GooglePlacesClient.Place> dedup = new LinkedHashMap<>();

        for (C c : centers) {
            GooglePlacesClient.NearbyResponse resp =
                    google.searchNearbyCafes(c.lat(), c.lng(), c.radius(), 20);

            if (resp != null && resp.getPlaces() != null) {
                for (GooglePlacesClient.Place p : resp.getPlaces()) {
                    // id가 "places/xxxxx" 형태라면 그대로 키로 씀
                    if (p.getId() != null && !dedup.containsKey(p.getId())) {
                        dedup.put(p.getId(), p);
                    }
                }
            }

            // 너무 빠른 연속 호출 방지 (요금/제한 대비 살짝 쉬어가기)
            try { Thread.sleep(150); } catch (InterruptedException ignored) {}
        }

        return new ArrayList<>(dedup.values());
    }
}