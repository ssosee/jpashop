package jpabook.jpashop.domain;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Entity
@Table(name = "orders")
@Getter @Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order {

    @Id @GeneratedValue
    @Column(name = "order_id")
    private Long id;

    /***
     * 회원과 주문은 일대다 양방향 관계
     * 회원 한명은 여러 주문을 할 수 있다.
     * 1:N
     * 이때, key point는 연관관계의 주인은 주문(외래키가 있는 곳을 연관관계의 주인으로 하는 것이 좋다.)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    /***
     * 주문과 주문상품은 일대다 양방향 관계이다.
     * 하나의 주문에는 여러 상품이 있다.
     * 1:N
     * 마찬가지로, 외래키가 있는 곳을 연관관계의 주인으로 한다.
     *
     * cascade =>
     * oredr를 persist할 때,
     * orderItems도 persist됨.
     */
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    private List<OrderItem> orderItems = new ArrayList<>();

    /***
     * 주문 상품과 주문은 일대일 양방향 관계이다.
     * 1:1
     * 연관관계의 주인은 아무곳에 해도 상관 없다.
     */
    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "delivery_id")
    private Delivery delivery;

    private LocalDateTime orderDate;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    /***
     * 연관관계 편의 메서드
     */
    public void changeMember(Member member) {

        /**
         * Member member = new Member();
         * Order order = new Order();
         *
         * member.getOrders().add(order);
         * order.setMember(member);
         *
         * order.changeMember(member);
         */

        this.member = member; //order.setMember(member);
        member.getOrders().add(this); //member.getOrders().add(order);
    }

    public void changeOrderItem(OrderItem orderItem) {
        orderItems.add(orderItem);
        orderItem.setOrder(this);
    }

    public void changeDelivery(Delivery delivery) {
        this.delivery = delivery;
        delivery.setOrder(this);
    }

    //==생성 메서드==//
    public static Order createOrder(Member member, Delivery delivery, OrderItem... orderItems) {
        Order order = new Order();
        order.setMember(member);
        order.setDelivery(delivery);

        /***
         *              for(OrderItem orderItem : orderItems) {
         *                 order.changeOrderItem(orderItem);
         *             }
         */
        Arrays.stream(orderItems).forEachOrdered(order::changeOrderItem);

        order.setStatus(OrderStatus.ORDER);
        order.setOrderDate(LocalDateTime.now());

        return order;
    }

    //비지니스 로직==//
    /***
     * 주문 취소
     */
    public void cancel() {
        if(delivery.getDeliveryStatus() == DeliveryStatus.COMP) {
            throw new IllegalStateException("이미 배송완료된 상품은 취소가 불가능 합니다.");
        }

        this.setStatus(OrderStatus.CANCEL);

        /***
         *         for (OrderItem orderItem : orderItems) {
         *             orderItem.cancel();
         *         }
         */
        orderItems.forEach(OrderItem::cancel);
    }

    //==조회 로직==//
    /***
     * 전체 주문 가격 조회
     */
    public int getTotalPrice() {
        int totalPrice = orderItems.stream().mapToInt(OrderItem::getTotalPrice).sum();
        /***
         *         int totalPrice = 0;
         *         for (OrderItem orderItem : orderItems) {
         *             totalPrice += orderItem.getTotalPrice();
         *         }
         */
        return totalPrice;
    }
}
