package jpabook.jpashop.repository;

import jpabook.jpashop.domain.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * spring data jpa 맛보기
 * 기본적인 구현체를 JpaRepository가 알아서 만들어서 넣어줌
 */
public interface MemberRepository extends JpaRepository<Member, Long> {
    //select m from Member m where m.name = ?
    List<Member> findByName(String name);
}
