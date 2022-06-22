package jpabook.jpashop.domain;

import lombok.Builder;
import lombok.Getter;

import javax.persistence.Embeddable;

@Embeddable
@Getter
public class Address {
    private String city;
    private String street;
    private String zipcode;

    protected Address() {

    }

    /***
     * 값 타입은 아래와 같이 생성자에서 설정하는 것이 유지보수에 편함.
     * @param city
     * @param street
     * @param zipcode
     */
    public Address(String city, String street, String zipcode) {
        this.city = city;
        this.street = street;
        this.zipcode = zipcode;
    }
}
