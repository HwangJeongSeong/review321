package com.team.cafe.google.api;


import com.team.cafe.google.infra.google.GooglePlacesClient;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/google")
@RequiredArgsConstructor
public class GooglePlacesController {

    private final GooglePlacesClient google;
    private final CafeHarvestService harvestService;

    @GetMapping("/daejeon-cafes")
    public GooglePlacesClient.NearbyResponse getDaejeonCafes() {
        return google.searchTextCafes("대전 카페");
    }

    @GetMapping("/daejeon-cafes/all")
    public List<GooglePlacesClient.Place> getDaejeonCafesAll() {
        return harvestService.harvestDaejeonCafes();
    }
}