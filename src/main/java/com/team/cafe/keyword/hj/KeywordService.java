package com.team.cafe.keyword.hj;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class KeywordService {
    private final KeywordRepository keywordRepository;

    @Transactional(readOnly = true)
    public List<Keyword> findAllOrderByTypeAndName() {
        return keywordRepository.findAllByOrderByTypeAscNameAsc();
    }


    public List<KeywordRow> findKeywordRowsByCafeId(Long cafeId) {
        return keywordRepository.findKeywordRowsByCafeId(cafeId);
    }
}
