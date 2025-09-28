package com.team.cafe.Root;

import com.team.cafe.list.hj.Cafe;
import com.team.cafe.list.hj.CafeListRepository;
import com.team.cafe.list.hj.CafeListService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Random;

@RequiredArgsConstructor
@Service
public class MainService {
    private final MainRepository mainRepository;

    public List<Cafe> getTop16ByViews() {
        return mainRepository.findTop4ByOrderByHitCountDesc();
    }


    @Transactional(readOnly = true)
    public List<CafeBannerRow> getTodayRecommended(int poolSize, int take) {
        var pool = mainRepository.findTopByHitCount(poolSize); // ex) 50
        long seed = LocalDate.now().toEpochDay();              // 날짜 기반 seed(하루 고정)
        Collections.shuffle(pool, new Random(seed));
        return pool.stream().limit(take).toList();             // ex) 10
    }


    @Transactional(readOnly = true)
    public List<CafeReviewSummary> getTop4ByReviews(int limit) {
        return mainRepository.findTopByReview(limit);
    }
}
