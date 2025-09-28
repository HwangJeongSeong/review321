package com.team.cafe.Menu;

import com.team.cafe.list.hj.Cafe;
import com.team.cafe.review.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Comment;

@Getter
@Setter
@Entity
@Table(name = "cafe_menu",
        indexes = {
                @Index(name="idx_menu_cafe", columnList="cafe_id"),
                @Index(name="idx_menu_name", columnList="name")
        })
public class Menu extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "menu_id")
    @Comment("메뉴 PK")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cafe_id", nullable = false)
    @Comment("소속 카페")
    private Cafe cafe;

    @Column(name = "name", nullable = false, length = 80)
    @Comment("메뉴명")
    private String name;

    @Column(name = "price", nullable = false)
    @Comment("가격(원)")
    private Integer price;

    @Column(name = "description", length = 200)
    @Comment("간단 설명")
    private String description;

    @Column(name = "sort_order")
    @Comment("정렬 순서(작을수록 위)")
    private Integer sortOrder;
}