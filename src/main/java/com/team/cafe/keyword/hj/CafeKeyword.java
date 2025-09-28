package com.team.cafe.keyword.hj;

import com.team.cafe.list.hj.Cafe;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name="cafe_keyword", // 인덱스용 !
        uniqueConstraints = @UniqueConstraint(name="uk_cafe_keyword", columnNames = {"cafe_id","keyword_id"}),
        indexes = {
                @Index(name="idx_ck_cafe", columnList = "cafe_id"),
                @Index(name="idx_ck_keyword", columnList = "keyword_id")
        })
@Getter
@Setter
public class CafeKeyword {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 단일 PK로 간단히 (복합키도 가능하지만 운영/디버깅이 편함)

    @ManyToOne(fetch = FetchType.LAZY, optional=false)
    @JoinColumn(name="cafe_id")
    private Cafe cafe;

    @ManyToOne(fetch = FetchType.LAZY, optional=false)
    @JoinColumn(name="keyword_id")
    private Keyword keyword;

    // 확장 여지: isPrimary, weight, note 등
    // @Column private boolean primaryTag;
}