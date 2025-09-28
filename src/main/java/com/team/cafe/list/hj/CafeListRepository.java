package com.team.cafe.list.hj;

import com.team.cafe.review.dto.CafeWithRating;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface CafeListRepository extends JpaRepository<Cafe, Long> { // ⬅️ Integer → Long
    // 기본 전체 조회 페이징
    Page<Cafe> findAll(Pageable pageable);

    // 해당 유저가 이 카페를 좋아요 했는지 여부
    boolean existsByIdAndLikedUsers_Id(Long cafeId, Long userId); // ⬅️ Integer → Long


    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("update Cafe c set c.hitCount = c.hitCount + 1 where c.id = :id")
    int incrementHitCount(@Param("id") Long id);



    @Query(value = """
    SELECT c.*
    FROM cafe c
    LEFT JOIN cafe_keyword ck ON ck.cafe_id = c.cafe_id
    WHERE (:kw IS NULL OR :kw = '' OR
       LOWER(c.cafe_name) LIKE LOWER(CONCAT('%', :kw, '%'))
       /* 필요시 확장:
       OR LOWER(c.city)     LIKE LOWER(CONCAT('%', :kw, '%'))
       OR LOWER(c.district) LIKE LOWER(CONCAT('%', :kw, '%'))
       OR LOWER(c.address1) LIKE LOWER(CONCAT('%', :kw, '%')) */
    )
    AND (
        :parking IS NULL
        OR c.parking_yn = CASE WHEN :parking THEN 1 ELSE 0 END
      )
    AND (
        :now IS NULL
        OR (
             (c.open_time <= c.close_time AND c.open_time <= :now AND :now <= c.close_time)
             OR
             (c.open_time >  c.close_time AND (:now >= c.open_time OR :now <= c.close_time))
           )
      )
    AND ( :size = 0 OR ck.keyword_id IN (:keyList) )
    GROUP BY c.cafe_id
    HAVING ( :size = 0 OR COUNT(DISTINCT ck.keyword_id) = :size )
    ORDER BY
      CASE WHEN :sort = 'name'      AND :dir = 'ASC'  THEN c.cafe_name  END ASC,
      CASE WHEN :sort = 'name'      AND :dir = 'DESC' THEN c.cafe_name  END DESC,
      CASE WHEN :sort = 'hit'       AND :dir = 'ASC'  THEN c.hit_count  END ASC,
      CASE WHEN :sort = 'hit'       AND :dir = 'DESC' THEN c.hit_count  END DESC,
      CASE WHEN :sort = 'createdAt' AND :dir = 'ASC'  THEN c.created_at END ASC,
      CASE WHEN :sort = 'createdAt' AND :dir = 'DESC' THEN c.created_at END DESC
   
    """,
            countQuery = """
    SELECT COUNT(*)
    FROM (
        SELECT c.cafe_id
    FROM cafe c
    LEFT JOIN cafe_keyword ck ON ck.cafe_id = c.cafe_id
    WHERE (:kw IS NULL OR :kw = '' OR
         LOWER(c.cafe_name) LIKE LOWER(CONCAT('%', :kw, '%'))
         /* 필요시 확장 동일 적용 */
    )
    AND (
          :parking IS NULL
          OR c.parking_yn = CASE WHEN :parking THEN 1 ELSE 0 END
        )
    AND (
          :now IS NULL
          OR (
               (c.open_time <= c.close_time AND c.open_time <= :now AND :now <= c.close_time)
               OR
               (c.open_time >  c.close_time AND (:now >= c.open_time OR :now <= c.close_time))
             )
        )
    AND ( :size = 0 OR ck.keyword_id IN (:keyList) )
    
    GROUP BY c.cafe_id
    HAVING ( :size = 0 OR COUNT(DISTINCT ck.keyword_id) = :size )
    ) t
""",
            nativeQuery = true)
    Page<Cafe> findAllMatchAllWithFiltersCaseOrder(
            @Param("kw") String kw,
            @Param("parking") Boolean parking,
            @Param("now") java.time.LocalTime now,
            @Param("keyList") List<Long> keyList,
            @Param("size") Long size,
            @Param("sort") String sort,
            @Param("dir")  String dir,
            Pageable pageable
    );


    @Query(value = """
    SELECT c.cafe_id AS id,
           c.cafe_name AS cafeName,
           AVG(r.rating) AS avgRating
    FROM cafe c
    LEFT JOIN reviews r ON r.cafe_id = c.cafe_id
    WHERE c.cafe_id IN (:ids)
    GROUP BY c.cafe_id, c.cafe_name
    ORDER BY c.cafe_id
    """, nativeQuery = true)
    List<CafeWithRating> getCafesWithAvgRating(List<Long> ids);


    // 내(로그인 사용자)의 비즈니스 계정에 연결된 카페를 찾는다
    Optional<Cafe> findByBusinessUser_User_Id(Long userId);
    Cafe findTopByOrderByIdDesc();


    boolean existsByGooglePlaceId(String googlePlaceId);


}