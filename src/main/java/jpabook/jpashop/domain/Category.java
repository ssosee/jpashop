package jpabook.jpashop.domain;

import jpabook.jpashop.domain.item.Item;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter @Setter
public class Category {
    @Id
    @GeneratedValue
    @Column(name = "category_id")
    private Long id;

    private String name;

    /***
     * 카테고리와 상품은 다대다 양방향관계이다.
     * 다대다 양방향 관계는 관계형데이터 베이스에서는 풀지 못하여
     * 가운데 테이블을 하나 만들어서 푼다.
     * 카테고리 <-1--N-> 카테고리 상품 <-N--1-> 상품
     * 실무에서는 권장 X
     */
    @ManyToMany
    @JoinTable(
            name = "category_time",
            joinColumns = @JoinColumn(name = "category_id"),
            inverseJoinColumns = @JoinColumn(name = "item_id")
    )
    private List<Item> items = new ArrayList<>();

    /***
     * 셀프로 양방향 연관관계를 걸었다.
     * 부모, 자식이 양방향 관계를 맺는다.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Category parent;

    @OneToMany(mappedBy = "parent")
    private List<Category> child = new ArrayList<>();

    /***
     * 연관관계 편의 메서드
     * @param child
     */
    public void changeChildCategory(Category child) {
        this.child.add(child);
        child.setParent(this);
    }
}
