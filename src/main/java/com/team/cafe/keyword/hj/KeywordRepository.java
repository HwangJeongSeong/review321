package com.team.cafe.keyword.hj;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface KeywordRepository extends JpaRepository<Keyword, Long> {

    // 타입 ASC → 이름 ASC
    @EntityGraph(attributePaths = {"type"})
    List<Keyword> findAllByOrderByTypeAscNameAsc();
//    SELECT k
//    FROM Keyword k
//    ORDER BY k.type ASC, k.name ASC

    @Query("""
          select k.id as id, k.name as name, k.type.typeName as typeName
                  from CafeKeyword ck
                    join ck.keyword k
                  where ck.cafe.id = :cafeId
                  order by k.name
    """)
    List<KeywordRow> findKeywordRowsByCafeId(@Param("cafeId") Long cafeId);
}
