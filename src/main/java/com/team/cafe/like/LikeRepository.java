package com.team.cafe.like;

import com.team.cafe.list.hj.Cafe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface LikeRepository  extends JpaRepository<Cafe, Long> {
    // 이 카페를 좋아요한 유저 수
    @Query("select count(u) from Cafe c join c.likedUsers u where c.id = :cafeId")
    long countLikes(@Param("cafeId") Long cafeId); // ⬅️ Integer → Long


    @Query("""
        select c.id as cafeId, count(distinct u.id) as cnt
        from Cafe c
        left join c.likedUsers u
        where c.id in :ids
        group by c.id
    """)
    List<CafeLikeCount> findLikeCountsByCafeIds(List<Long> ids);

//    boolean existsByCafeIdAndUserId(Long cafeId, Long userId);
}
