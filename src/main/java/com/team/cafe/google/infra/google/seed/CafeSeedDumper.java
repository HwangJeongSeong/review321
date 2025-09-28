package com.team.cafe.google.infra.google.seed;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team.cafe.google.api.CafeHarvestService;
import com.team.cafe.google.infra.google.GooglePlacesClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.List;

@Component
@RequiredArgsConstructor
public class CafeSeedDumper {

    private final CafeHarvestService harvest;
    private final ObjectMapper mapper;

    // 개발자가 "한 번 실행"할 용도
    public void dumpDaejeonCafesNearby() throws Exception {
        List<GooglePlacesClient.Place> cafes = harvest.harvestDaejeonCafes();
        mapper.writerWithDefaultPrettyPrinter()
                .writeValue(new File("src/main/resources/data/daejeon-cafes.json"), cafes);
    }
}
