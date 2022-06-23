package jpabook.jpashop;

import jpabook.jpashop.domain.*;
import jpabook.jpashop.domain.item.Book;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;

/**
 * 총 주문 2개
 * userA
 * JPA1 BOOK
 * JPA2 BOOK
 *
 * userB
 * STRING1 BOOK
 * STRING2 BOOK
 */
@Component
@RequiredArgsConstructor
public class InitDb {

    private final InitSerivce initSerivce;

    @PostConstruct
    public void init() {
        initSerivce.dbInit1();
        initSerivce.dbInit2();
    }


    @Component
    @Transactional
    @RequiredArgsConstructor
    static class InitSerivce {
        private final EntityManager em;

        public void dbInit1() {
            Member member = createMember("userA", "1", "11");
            em.persist(member);

            Book book1 = createBook("JPA1", 10000);

            Book book2 = createBook("JPA2", 20000);

            em.persist(book1);
            em.persist(book2);

            OrderItem orderItem1 = OrderItem.createOrderItem(book1, 10000, 1);
            OrderItem orderItem2 = OrderItem.createOrderItem(book2, 20000, 2);

            Order order = createOrder(member, orderItem1, orderItem2);
            em.persist(order);
        }

        private Order createOrder(Member member, OrderItem... orderItems) {
            Delivery delivery = new Delivery();
            delivery.setAddress(member.getAddress());
            Order order = Order.createOrder(member, delivery, orderItems);
            return order;
        }

        private Book createBook(String name, int price) {
            Book book1 = new Book();
            book1.setName(name);
            book1.setPrice(price);
            book1.setStockQuantity(100);
            return book1;
        }

        private Member createMember(String name, String street, String zipcode) {
            Member member = new Member();
            member.setName(name);
            member.setAddress(new Address("서울", street, zipcode));
            return member;
        }

        public void dbInit2() {
            Member member = createMember("userB", "2", "22");
            em.persist(member);

            Book book1 = createBook("STRING1", 10000);

            Book book2 = createBook("STRING2", 20000);

            em.persist(book1);
            em.persist(book2);

            OrderItem orderItem1 = OrderItem.createOrderItem(book1, 10000, 1);
            OrderItem orderItem2 = OrderItem.createOrderItem(book2, 20000, 2);

            Order order = createOrder(member, orderItem1, orderItem2);
            em.persist(order);
        }
    }


}
