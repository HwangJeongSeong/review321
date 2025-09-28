package com.team.cafe.keyword.hj;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "keyword", // 인덱스용 !
        indexes = {
                @Index(name="idx_keyword_type", columnList = "type_id"),
                @Index(name="idx_keyword_name", columnList = "name")
        })
@Getter
@Setter
public class Keyword {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length=60, nullable=false)
    private String name;       // 예: "연인 데이트", "주차 편리"

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="type_id", nullable=false)
    private KeywordType type;

    @Column(length=200)
    private String description;
}