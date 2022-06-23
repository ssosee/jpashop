package jpabook.jpashop.api;

import jpabook.jpashop.domain.Address;
import jpabook.jpashop.domain.Order;
import jpabook.jpashop.domain.OrderStatus;
import jpabook.jpashop.repository.OrderRepository;
import jpabook.jpashop.repository.OrderSearch;
import jpabook.jpashop.repository.order.simplequery.OrderSimpleQueryDto;
import jpabook.jpashop.repository.order.simplequery.OrderSimpleRepository;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * XToOne(ManyToOne, OneToOne)
 * Order
 * Oredr -> Member
 * Order -> Delivery
 *
 * 정리
 * 1. 엔티티를 dto로 변환하는 방법을 채택
 * 2. 필요하면 패치 조인으로 성능 최적화
 * 3. 2번으로 안되면 dto로 직접 조회
 * 4. jpa가 제공하는 네이티브SQL이나 JDBC Template을 사용해서 SQL을 직접 사용용 */
@RestController
@RequiredArgsConstructor
public class OrderSimpleApiController {

    private final OrderRepository orderRepository;
    private final OrderSimpleRepository orderSimpleRepository;

    @GetMapping("/api/v1/simple-orders")
    public List<Order> ordersV1() {
        List<Order> all = orderRepository.findAllByString(new OrderSearch());
        for (Order order : all) {
            //order.getMember()까지는 프록시 객체, getName()하면 실제로 DB에서 갖고옴
            order.getMember().getName(); //Lazy 강제 초기화
            order.getDelivery().getAddress();
        }
        return all;
    }

    //DTO 변환
    @GetMapping("/api/v2/simple-orders")
    public Result ordersV2() {
        /**
         * Order -> SQL 1번 -> 결과 주문수 2개
         *
         * 1. order 1번 조회(order의 결과 수가 N개)
         * 2. order -> member 지연로딩 조회 N번
         * 3. order -> delivery 지연로딩 조회 N번
         * 현재 order의 결과가 2개 이므로 최악의 경우 1 + 2 + 2 문제 발생(지연 로딩 대상이 영속성 컨텍스트에 있으면 조회 안함!)
         */
        List<Order> orders = orderRepository.findAllByString(new OrderSearch());
        /**
         * 2번 루프(order의 결과 수에 따라)
         */
        List<SimpleOrderDto> collect = orders.stream()
                .map(o -> new SimpleOrderDto(o))
                .collect(Collectors.toList());

        return new Result(collect);
    }

    //페치 조인
    @GetMapping("/api/v3/simple-orders")
    public Result ordersV3() {
        /**
         * 패치 조인을 사용하여 쿼리는 1개..!
         * 패치 조인으로 order -> member, order -> delivery는 이미 조회된 상태이므로 지연로딩X
         */
        List<Order> orders = orderRepository.findAllWithMemberDelivery();
        List<SimpleOrderDto> collect = orders.stream()
                .map(o -> new SimpleOrderDto(o))
                .collect(Collectors.toList());

        return new Result(collect);
    }

    //dto로 조회
    @GetMapping("/api/v4/simple-orders")
    public Result ordersV4() {
        /**
         * join하는 곳은 똑같음
         * 하지만, 원하는 것만 dto로 조회한다.
         *
         * jpql의 결과를 dto로 즉시 반환
         * 리포지토리의 재사용성이 떨어진다.(api스펙에 맞춘 코드가 리포지토리에 들어가는 단점)
         */
        List<OrderSimpleQueryDto> ordersDto = orderSimpleRepository.findOrdersDto();

        return new Result(ordersDto);
    }

    @Data
    static class SimpleOrderDto {
        private Long orderId;
        private String name;
        private LocalDateTime orderDate;
        private OrderStatus orderStatus;
        private Address address;

        public SimpleOrderDto(Order order) {
            orderId = order.getId();
            name = order.getMember().getName(); //Lazy 초기화
            orderDate = order.getOrderDate();
            orderStatus = order.getStatus();
            address = order.getDelivery().getAddress(); //Lazy 초기화
        }
    }

    @Data
    @AllArgsConstructor
    static class Result<T> {
        private T data;
    }
}
