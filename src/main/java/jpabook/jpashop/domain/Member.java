package jpabook.jpashop.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import javax.validation.constraints.NotEmpty;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter @Setter
public class Member {

    /***
     * 회원과 주문은 일대다 양방향 관계
     * 회원 한명은 여러 주문을 할 수 있다.
     * 1:N
     * 이때, key point는 연관관계의 주인은 주문(외래키가 있는 곳을 연관관계의 주인으로 하는 것이 좋다.)
     */
    @Id @GeneratedValue
    @Column(name = "member_id")
    private Long id;

    @NotEmpty
    private String name;

    @Embedded
    private Address address;

    @JsonIgnore
    @OneToMany(mappedBy = "member") //order table에 member에 mapping
    private List<Order> orders = new ArrayList<>();
}
