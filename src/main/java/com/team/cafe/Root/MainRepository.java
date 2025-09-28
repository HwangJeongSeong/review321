package com.team.cafe.Root;

import com.team.cafe.list.hj.Cafe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MainRepository extends JpaRepository<Cafe, Long> {
    List<Cafe> findTop4ByOrderByHitCountDesc();

    @Query(value = """
    SELECT
      c.cafe_id   AS id,
      c.cafe_name AS cafeName,
      (
        SELECT ci.img_url
        FROM cafe_image ci
        WHERE ci.cafe_id = c.cafe_id
        ORDER BY ci.id DESC
        LIMIT 1
      ) AS primaryImageUrl
    FROM cafe c
    ORDER BY c.hit_count DESC
    LIMIT :limit
    """, nativeQuery = true)
    List<CafeBannerRow> findTopByHitCount(@Param("limit") int limit);

    @Query(value = """
        WITH
        r_stats AS (
          SELECT r.cafe_id,
                 ROUND(AVG(r.rating), 1) AS avg_rating,
                 COUNT(r.id)            AS reviews_count
          FROM reviews r
          GROUP BY r.cafe_id
        ),
        latest_reviews AS (
          SELECT cafe_id, created_by, content
          FROM (
            SELECT r.*,
                   ROW_NUMBER() OVER (PARTITION BY r.cafe_id ORDER BY r.created_at DESC) AS rn
            FROM reviews r
            WHERE r.is_active = 1
          ) x
          WHERE x.rn = 1
        ),
        primary_img AS (
          SELECT cafe_id, img_url
          FROM (
            SELECT ci.*,
                   ROW_NUMBER() OVER (PARTITION BY ci.cafe_id ORDER BY ci.id ASC) AS rn
            FROM cafe_image ci
          ) x
          WHERE x.rn = 1
        )
        SELECT
          c.cafe_id                    AS id,
          c.cafe_name                  AS cafeName,
          COALESCE(rs.avg_rating, 0.0) AS avgRating,
          COALESCE(rs.reviews_count, 0) AS reviewsCount,
          pi.img_url                   AS primaryImageUrl,
          lr.created_by                AS latestReviewsAuthor,
          lr.content                   AS latestReviewsContent
        FROM cafe c
        LEFT JOIN r_stats       rs ON rs.cafe_id = c.cafe_id
        LEFT JOIN latest_reviews lr ON lr.cafe_id = c.cafe_id
        LEFT JOIN primary_img   pi ON pi.cafe_id = c.cafe_id
        ORDER BY COALESCE(rs.reviews_count, 0) DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<CafeReviewSummary> findTopByReview(@Param("limit") int limit);


}
