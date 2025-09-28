package com.team.cafe.config_js;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.team.cafe.google.api.CafeHarvestService;
import com.team.cafe.google.infra.google.GooglePlacesClient;
import com.team.cafe.list.hj.Cafe;
import com.team.cafe.list.hj.CafeListRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.List;

@Component
@RequiredArgsConstructor
public class CafeDataInitializer implements CommandLineRunner {

    private final CafeListRepository cafeListRepository;
    private final CafeHarvestService harvest;
    private final ObjectMapper mapper;

    @Override
    public void run(String... args) throws Exception {
        File file = new File("src/main/resources/data/daejeon-cafes.json");
        List<GooglePlacesClient.Place> places;

        if (file.exists()) {
            // ✅ 파일이 있으면 그대로 읽기
            places = mapper.readValue(file, new TypeReference<>() {});
            System.out.println("ℹ️ 기존 JSON 파일 로드: " + places.size() + "건");
        } else {
            // ✅ 없으면 API 호출 후 파일 생성
            places = harvest.harvestDaejeonCafes();
            file.getParentFile().mkdirs();
            mapper.writerWithDefaultPrettyPrinter().writeValue(file, places);
            System.out.println("✅ JSON 파일 새로 생성: " + file.getAbsolutePath());
        }

        // ✅ DB 저장 (중복 방지: googlePlaceId 기준)
        for (GooglePlacesClient.Place p : places) {
            if (p.getId() != null && !cafeListRepository.existsByGooglePlaceId(p.getId())) {
                cafeListRepository.save(toEntity(p));
            }
        }
    }

    private Cafe toEntity(GooglePlacesClient.Place p) {
        return Cafe.builder()
                .googlePlaceId(p.getId())
                .name(p.getDisplayName() != null ? p.getDisplayName().getText() : null)
                .streetAdr(p.getFormattedAddress())
                .build();
    }
}