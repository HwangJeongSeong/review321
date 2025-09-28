package com.team.cafe.Menu;

import com.team.cafe.list.hj.CafeListRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/mypage/cafe/menu")
public class CafeMenuApiController {

    private final MenuService menuService;           // findForDetail(cafeId) 제공
    private final MenuRepository menuRepository;     // save/delete
    private final CafeListRepository cafeListRepository; // 카페 존재 확인용

    // 목록
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public List<MenuDto> list(@RequestParam Long cafeId) {
        return menuService.findForDetail(cafeId).stream()
                .map(MenuDto::of)
                .toList();
    }

    // 등록
    @PostMapping(consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    @Transactional
    public ResponseEntity<?> create(@RequestParam Long cafeId,
                                    @RequestParam String name,
                                    @RequestParam Integer price,
                                    @RequestParam(required = false) String description) {
        var cafe = cafeListRepository.findById(cafeId)
                .orElseThrow(() -> new IllegalArgumentException("카페가 없습니다: " + cafeId));
        Menu m = new Menu();
        m.setCafe(cafe);
        m.setName(name.trim());
        m.setPrice(price);
        m.setDescription(description != null ? description.trim() : null);
        // sortOrder는 비워두면 쿼리에서 coalesce(999999)로 맨 뒤 정렬
        var saved = menuRepository.save(m);
        return ResponseEntity.created(URI.create("/api/mypage/cafe/menu/" + saved.getId()))
                .body(MenuDto.of(saved));
    }

    // 삭제:
    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<?> delete(@PathVariable Long id) {
        if (!menuRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        menuRepository.deleteById(id);
        return ResponseEntity.ok("deleted");
    }

    // 응답용
    public record MenuDto(Long id, String name, Integer price, String description) {
        public static MenuDto of(Menu m) {
            return new MenuDto(m.getId(), m.getName(), m.getPrice(), m.getDescription());
        }
    }
}
