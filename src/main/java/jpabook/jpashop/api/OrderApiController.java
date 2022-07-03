package jpabook.jpashop.api;

import jpabook.jpashop.domain.Address;
import jpabook.jpashop.domain.Order;
import jpabook.jpashop.domain.OrderItem;
import jpabook.jpashop.repository.OrderRepository;
import jpabook.jpashop.repository.OrderSearch;
import jpabook.jpashop.repository.order.query.OrderFlatDto;
import jpabook.jpashop.repository.order.query.OrderItemQueryDto;
import jpabook.jpashop.repository.order.query.OrderQueryDto;
import jpabook.jpashop.repository.order.query.OrderQueryRepository;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.*;

/**
 * <h1>권장 순서</h1>
 * <p>1. 엔티티 조회 방식으로 우선 접근</p>
 *  <p>1-1) 페치조인으로 쿼리 수를 최적화</p>
 *  <p>1-2) 컬렉션 최적화</p>
 *      <p>1-2-1) 페이징 필요 O -> hibernate.default_batch_fetch_size, @BatchSize로 최적화</p>
 *      <p>1-2-2) 페이징 필요 X -> 페치조인 사용</p><br>
 *
 * <p>2. 엔티티 조회 방식으로 해결이 안되면 DTO 조회 방식 사용</p>
 * <p>3. DTO 조회 방식으로 해결이 안되면 NativeSQL or 스프링 JdbcTemplate</p><br>
 *
 * <h2>참고</h2>
 *  <p>엔티티 조회 방식은 페치조인이나, hibernate.default_batch_fetch_size, @BatchSize 같이 코드를 거의 수정하지 않고,</p>
 *  <p>옵션만 약간 변경해서 다양한 성능 최적화를 시도할 수 있다.</p>
 *  <p>반면에, DTO를 직접 조회하는 방식은 성능을 최적화 하거나 성능 최적화 방식을 변경할 때 많은 코드를 변경해야 한다.</p><br>
 *
 * <h2>여담</h2>
 *  <p>엔티티는 직접 캐싱을 하면 안된다.</p>
 *  <p>영속성 컨텍스트에서 관리하려고 하기 때문에 캐시에 잘못 올라가면 장애가 발생한다.</p>
 *  <p>따라서 캐싱하는 것은 DTO로 변환하고 DTO를 캐싱한다.(Redis or Local)</p><br>
 *
 * <h2>참고</h2>
 *  <p>개발자는 성능 최적화와 코드 복잡도 사이에서 줄타기를 해야한다...!</p>
 *  <p>항상 그런것은 아니지만, 보통 성능 최적화는 단순한 코드를 복짭한 코드로 몰고간다...ㅠ0ㅠ</p>
 *  <p>엔티티 조회 방식은 jpa가 많은 부분을 최적화 해주기 때문에, 단순한 코드를 유지하면서, 성능 최적화를 할수 있다.</p>
 *  <p>반면에 DTO 조회 방식은 SQL을 직접 다루는 것과 유사하기 때문에, 둘 사이에서 줄타기를 해야한다.</p>
 *
 */
@RestController
@RequiredArgsConstructor
public class OrderApiController {

    private final OrderRepository orderRepository;
    private final OrderQueryRepository orderQueryRepository;

    @GetMapping("/api/v1/orders")
    public Result ordersV1() {
        List<Order> all = orderRepository.findAllByString(new OrderSearch());
        for (Order order : all) {
            /**
             * 강제 초기화
             * hibernate5Module()에서 LAZY LOADING인것들 호출...
             * 양방향 관계인것들은 @JsonIgnore를 설정해줘야함
            */
            order.getDelivery().getAddress();
            order.getMember().getName();
            List<OrderItem> orderItems = order.getOrderItems();
            orderItems.stream().forEach(o -> o.getItem().getName());
        }

        return new Result(all);
    }

    /**
     * 11개의 쿼리 발생..
     * 성능이 안나온다.!
     * @return
     */
    @GetMapping("/api/v2/orders")
    public Result ordersV2() {
        List<Order> orders = orderRepository.findAllByString(new OrderSearch());
        List<OrderDto> result = orders.stream()
                .map(order -> new OrderDto(order))
                .collect(Collectors.toList());

        return new Result(result);
    }

    /**
     * 데이터 뻥튀기..를 막기위해
     * orderRepository.findAllWithItem() 추가
     *
     * 패치조인으로 sql 1번만 실행
     * `distinct`사용한 이유는 *1대다 조인이 있으므로 데이터베이스 row가 증가한다.
     * 그 결과 같은 order 엔티티의 조회수도 증가하게된다.
     * jpa의 distinct를 추가하고, 더해서 같은 엔티티가 조회되면, 애플리케이션에서 중복을 제거해준다.
     * 이 예에서 order가 컬렌셕 패치 조인 때문에 중복 조회되는 것을 막아준다.
     *
     * 단점은 페이징이 불가능하다..!?
     * 컬렉션 페치 조인을 사용하면 페이징이 불가하다.
     * 하이버네이트는 경고 로그를 남기면서 모든 데이터를 DB에서 읽어오고,
     * 메모리에 넣어서 애플리케이션에서 페이징 처리를함..(데이터가 많으면 큰일난다..)
     *
     * 추가로, 컬렉션 페치 조인은 1개만 사용가능하다.
     * 컬렌션 둘 이상에 페치 조인을 사용하면 안된다.
     * 데이터가 부정합하게 조회될 수 있다.
     *
     * @return
     */
    @GetMapping("/api/v3/orders")
    public Result ordersV3() {
        List<Order> orders = orderRepository.findAllWithItem();

        /**
         * distinct가 없으면 ref와 id값이 동일함.
         * order ref = jpabook.jpashop.domain.Order@6451be40, id = 4
         * order ref = jpabook.jpashop.domain.Order@6451be40, id = 4
         * order ref = jpabook.jpashop.domain.Order@22ae213, id = 11
         * order ref = jpabook.jpashop.domain.Order@22ae213, id = 11
         */
        for (Order order : orders) {
            System.out.println("order ref = "+order+", id = "+order.getId());
        }

        List<OrderDto> result = orders.stream()
                .map(order -> new OrderDto(order))
                .collect(Collectors.toList());

        return new Result(result);
    }

    /**
     * 컬렉션을 페치 조인하면 페이징이 불가능하다.
     *
     *  컬렉션을 패치 조인하면 1대N 조인이 발생하므로 데이터가 예측할수 없이 증가
     *  1대N에서 1을 기준으로 페이징하는 것이 목적이다.
     *  그런데 데이터는 N을 기준으로 row가 생성된다.
     *  즉 Order를 기준으로 페이징 하고 싶은데, N인 OrderItem을 조인하면 OrderItem이 기준이 되어버린다.
     *  이경우에 V3에서 언급한 것처럼 하이버네이트는 경고 로그를 남기고 모든 DB 데이터를 읽어서 메모리에서 페이징을 시도한다.(최악의 경우 장애 발생)
     *
     *
     * 그렇다면 페이징 + 컬렉션 엔티티를 함께 조회하려면 어떻게 해야하는가?
     *
     * 1. ToOne관계를 모두 페치조인한다. ToOne관계는 row수를 증가시키지 않으므로 페이징 쿼리에 영향을 주지 않는다.
     * 2. 컬렉션은 지연 로딩으로 조회한다.
     * 3. 지연 로딩 능 최적화를 위해 hibernate.default_batch_fetch_size, @BathSize를 적용한다.
     *  3-1) hibernate.default_batch_fetch_size: 글로벌 설정 -> IN쿼리 갯수
     *  3-2) @BathSize 개별 최적화
     *  위 2가지 옵션을 사용하면 컬렌션이나, 프록시 객체를 한꺼번에 설정한 size만큼 IN쿼리로 조회한다.
     *
     * 장점
     *  쿼리 호출 수가 1+N -> 1 + 1로 최적화 된다.
     *  조인보다 DB데이터 전송량이 최적화 된다.
     *  (Order와 OrderItem을 조인하면 Order가 OrderItem만큼 중복해서 조회된다. 이 방법은 각각 조회하므로 전송해야할 중복 데이터가 없다.)
     *  패치 조인 방식과 비교해서 쿼리 호출 수가 약간 증가하지만, DB 데이터 전송량이 감소
     *  컬렌션 페치 조인은 페이징이 불가능 하지만, 이 방법은 가능한다.
     *
     * 결론
     *  ToOne관게는 페치 조인해도 페이징에 영향을 주지 않는다. 따라서 ToOne관계는 페치조인으로 쿼리 수를 줄여서 해결하고,
     *  나머지는 hibernate.default_batch_fetch_size로 최적화 하자
     *
     *  hibernate.default_batch_fetch_size는 100 ~ 1000개를 권장함
     */
    @GetMapping("/api/v3.1/orders")
    public Result ordersV3_page(
            @RequestParam(value = "offset", defaultValue = "0") int offset,
            @RequestParam(value = "limit", defaultValue = "100") int limit) {
        //ToOne관계를 모두 페치조
        List<Order> orders = orderRepository.findAllWithMemberDelivery(offset, limit);

        List<OrderDto> result = orders.stream()
                .map(order -> new OrderDto(order))
                .collect(Collectors.toList());

        return new Result(result);
    }

    /**
     * Query: 루트 1번, 컬렉션 N번 실행
     * ToOne(N:1, 1:1)관계들을 먼저 조회하고, ToMany 관계는 각각 별도로 처리한다.
     *  why?
     *      ToOne 관계는 조인해도 데이터 row 수가 증가하지 않는다.
     *      ToMany 관계는 조인하면 row 수가 증가한다.
     * row 수가 증가하지 않는 ToOne관계는 조인으로 최적화 하기 쉬우므로 한번에 조회하고,
     * ToMany 관계는 최적화하기 어렵기 때문에 findOrderItems()같은 별도의 메서드로 조회한다.
     * @return
     */
    @GetMapping("/api/v4/orders")
    public Result ordersV4() {
        return new Result(orderQueryRepository.findOrderQueryDtos());
    }

    /**
     * Query: 루트 1번, 컬렉션 1번 실행
     * ToOne 관계들을 먼저 조회하고, 여기서 얻은 식별자 orderId로 ToMany 관계인 OrderItem을 한꺼번에 조회
     * Map을 이용해서 매칭 성능 향상 (O(1))
     * @return
     */
    @GetMapping("/api/v5/orders")
    public Result ordersV5() {
        return new Result(orderQueryRepository.findAllByDto_optimization());
    }

    /**
     * Query: 1번
     * 단점
     *  쿼리는 1번이지만, 조인으로 인해 DB에서 애플리에키션에 전달하는 데이터 중복 데이터가 추가되므로 상황에 따라 V5 보다 더 느릴 수도 있따.
     *  애플리케이션에서 추가 작업이 크다.
     *  페이징 불가(Order를 기준으로는 안된다.!!, OrderItem 기준으로는 가)
     * @return
     */
    @GetMapping("/api/v6/orders")
    public Result ordersV6() {

        List<OrderFlatDto> flats = orderQueryRepository.findAllByDto_flat();

        List<OrderQueryDto> collect = flats.stream()
                .collect(groupingBy(o -> new OrderQueryDto(o.getOrderId(), o.getName(), o.getOrderDate(), o.getOrderStatus(), o.getAddress())
                        , mapping(o -> new OrderItemQueryDto(o.getOrderId(), o.getItemName(), o.getOrderPrice(), o.getCount()), toList())
                )).entrySet().stream()
                .map(e -> new OrderQueryDto(
                        e.getKey().getOrderId(), e.getKey().getName(),
                        e.getKey().getOrderDate(), e.getKey().getOrderStatus(),
                        e.getKey().getAddress(), e.getValue())
                )
                .collect(toList());

        return new Result(collect);
    }


    @Getter
    static class OrderDto {
        private Long orderId;
        private String name;
        private LocalDateTime orderDate;
        private Address address;
//        private List<OrderItem> orderItems;
        private List<OrderItemDto> orderItems;
        public OrderDto(Order order) {
            this.orderId = order.getId();
            this.name = order.getMember().getName();
            this.orderDate = order.getOrderDate();
            this.address = order.getDelivery().getAddress();
            /**
             * Dto에 단순하게 감싸서 보내면 안된다.
             * 완전히 엔티티의 의존을 끊어야함!
             * OrderItem조차도 다 Dto로 바꿔야 한다..!!!
             * Address같은 Value Object는 상관 없음
             */
//            order.getOrderItems().stream().forEach(o -> o.getItem().getName());
//            this.orderItems = order.getOrderItems();
            this.orderItems = order.getOrderItems().stream()
                    .map(orderItem -> new OrderItemDto(orderItem))
                    .collect(Collectors.toList());
        }

        @Getter
        static class OrderItemDto {
            private String itemName;
            private int orderPrice;
            private int count;
            public OrderItemDto(OrderItem orderItem) {
                this.itemName = orderItem.getItem().getName();
                this.orderPrice = orderItem.getItem().getPrice();
                this.count = orderItem.getCount();
            }
        }
    }

    @Data
    @AllArgsConstructor
    static class Result<T> {
        private T data;
    }
}
