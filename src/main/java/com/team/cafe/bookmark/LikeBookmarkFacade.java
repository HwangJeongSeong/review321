package com.team.cafe.bookmark;

import com.team.cafe.like.LikeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LikeBookmarkFacade {
    private final LikeService likeService;
    private final BookmarkService bookmarkService;

    @Transactional
    public boolean toggleAndSync(Long cafeId, Long userId, boolean removeBookmarkOnUnlike) {
        boolean likedNow = likeService.toggle(cafeId, userId);

        if (likedNow) {
            bookmarkService.ensure(cafeId, userId);   // 좋아요 ON → 북마크 보장
        } else if (removeBookmarkOnUnlike) {
            bookmarkService.remove(cafeId, userId);   // 좋아요 OFF → 북마크 삭제 (옵션)
        }

        return likedNow;
    }
}