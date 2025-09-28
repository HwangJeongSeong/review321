package com.team.cafe.bookmark;

import com.team.cafe.list.hj.CafeListRepository;
import com.team.cafe.user.sjhy.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BookmarkService {
    private final BookmarkRepository bookmarkRepository;
    private final UserRepository userRepository;
    private final CafeListRepository cafeListRepository;

    @Transactional
    public void ensure(Long cafeId, Long userId) {
        if (bookmarkRepository.existsByUser_IdAndCafe_Id(userId, cafeId)) return;

        Bookmark bm = new Bookmark();
        bm.setUser(userRepository.getReferenceById(userId));
        bm.setCafe(cafeListRepository.getReferenceById(cafeId));

        try {
            bookmarkRepository.save(bm);
        } catch (DataIntegrityViolationException e) {
            // 유니크 제약 충돌 → 이미 누군가가 먼저 넣음 → 무시
        }

    }

    @Transactional
    public void remove(Long cafeId, Long userId) {
        // 1) (user_id, cafe_id)에 해당하는 북마크를 조회
        bookmarkRepository.findByUser_IdAndCafe_Id(userId, cafeId)
                // 2) 있으면 delete
                .ifPresent(bookmarkRepository::delete);
        // flush/commit 시점에 DELETE 실행
    }

    public boolean existsByUser_IdAndCafe_Id(Long id, Long cafeId) {
        return bookmarkRepository.existsByUser_IdAndCafe_Id(id, cafeId);
    }

    public List<Bookmark> getBookmarksByUser(Long userId) {
        return bookmarkRepository.findAllByUserIdWithCafe(userId);
    }
}
