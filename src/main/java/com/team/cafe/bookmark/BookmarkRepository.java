package com.team.cafe.bookmark;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {

    //북마크가 표시가 되어있는지
    boolean existsByUser_IdAndCafe_Id(Long userId, Long cafeId);

    //해당 유저의 북마크 데이터가 있는지 조회
    Optional<Bookmark> findByUser_IdAndCafe_Id(Long userId, Long cafeId);

    // 특정 유저 북마크한 전체 카페목록 조회
    List<Bookmark> findAllByUser_Id(Long userId);

    @Query("select b from Bookmark b join fetch b.cafe where b.user.id = :userId")
    List<Bookmark> findAllByUserIdWithCafe(@Param("userId") Long userId);

    //북마크 목록확인 지금은 사용안할거임
//    @Query("""
//    select b.cafe.id
//        from Bookmark b
//        where b.user.id = :userId and b.cafe.id in :cafeIds
//    """)
//    List<Long> findBookmarkedCafeIds(@Param("userId") Long userId,
//                                     @Param("cafeIds") List<Long> cafeIds);

    long countByCafe_Id(Long cafeId);

}
