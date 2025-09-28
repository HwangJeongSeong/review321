package com.team.cafe.cafeListImg.hj;

import com.team.cafe.list.hj.Cafe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CafeImageRepository extends JpaRepository<CafeImage, Long> {

    // 단일 카페 대표 이미지 1장
    Optional<CafeImage> findByCafe_Id(Long cafeId);

    // 이번 페이지의 카페들 대표 이미지 일괄 조회
    List<CafeImage> findByCafe_IdIn(Collection<Long> cafeIds);

    @Query("select count(u) from Cafe c join c.likedUsers u where c.id = :cafeId")
    long countLikes(@Param("cafeId") Long cafeId);
    List<CafeImage> findAllByCafe_Id(Long cafeId);


    @Query("select distinct c from Cafe c left join fetch c.images where c.id = :id")
    Optional<Cafe> findByIdWithImages(@Param("id") Long id);

}