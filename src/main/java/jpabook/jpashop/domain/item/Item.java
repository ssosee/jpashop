package jpabook.jpashop.domain.item;

import jpabook.jpashop.domain.Category;
import jpabook.jpashop.exception.NotEnoughStockException;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "dtype")
@Getter @Setter
public abstract class Item {
    @Id
    @GeneratedValue
    @Column(name = "item_id")
    private Long id;

    private String name;
    private int price;
    private int stockQuantity;

    /***
     * 카테고리와 상품은 다대다 양방향관계이다.
     * 다대다 양방향 관계는 관계형데이터 베이스에서는 풀지 못하여
     * 가운데 테이블을 하나 만들어서 푼다.
     * 카테고리 <-1--N-> 카테고리 상품 <-N--1-> 상품
     * 실무에서는 권장 X
     */
    @ManyToMany(mappedBy = "items")
    private List<Category> categories = new ArrayList<>();


    //=====비지니스 로직=====//
    /***
     * stock 증가
     */
    public void addStock(int stockQuantity) {
        this.stockQuantity += stockQuantity;
    }
    /***
     * stock 감소
     */
    public void removeStock(int stockQuantity) {
        int restStock = this.stockQuantity - stockQuantity;
        if(restStock < 0) {
            throw new NotEnoughStockException("need more stock");
        }
        this.stockQuantity = restStock;
    }
}
