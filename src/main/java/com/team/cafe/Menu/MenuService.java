package com.team.cafe.Menu;

import com.team.cafe.list.hj.CafeListRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MenuService {
    private final MenuRepository menuRepository;
    private final CafeListRepository cafeListRepository;


    public List<Menu> findForDetail(Long cafeId) {
        return menuRepository.findForDetail(cafeId);
    }
}
