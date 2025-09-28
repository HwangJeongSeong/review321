package com.team.cafe.Menu;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MenuRepository extends JpaRepository<Menu, Long> {

    // 정렬(정의 없으면 맨 뒤) → 이름순
    @Query("""
           select m from Menu m
           where m.cafe.id = :cafeId
           order by coalesce(m.sortOrder, 999999), m.name
           """)
    List<Menu> findForDetail(@Param("cafeId") Long cafeId);

}