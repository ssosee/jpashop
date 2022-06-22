package jpabook.jpashop.api;

import jpabook.jpashop.domain.Address;
import jpabook.jpashop.domain.Member;
import jpabook.jpashop.service.MemberService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
public class MemberApiController {
    private final MemberService memberService;

    /**
     * 엔티티를 직접 반환하면 안됨!!!!!!!!!
     * 엔티티를 @JsonIgnore 같은 응답에 맞는 로직이 추가되면 유지보수가 어려움
     * -> 나중에 해당 Entity로 어떤 값을 넘겨줘야 할지 모르기 때문..
     * 문제
     * 1. 엔티티에 프레젠테이션 계층을 위한 로직 추가
     * 2. 기본적으로 엔티티의 모든 값이 노출
     * 3. 응답 스펙을 맟주기 위해 로직 추가(@JsonIgnore)
     * 4. 엔티티에 대해 api용도에 따라 다양하게 적용하기 힘듦
     * 5. 추후 api스펙 변경시 기존 서비스에 장애 발생
     * 6. 현재 데이터가 [ { ... } ] 같은 array형식으로 반환함(추후 데이터 추가시 문제 발생)
     * 해결
     * api 응답용 dto를 반환
     * @return
     */
    @GetMapping("/api/v1/members")
    public List<Member> MembersV1() {
        return memberService.findMembers();
    }

    /**
     * Dto를 통해서 노출하고 싶은 것만 노출!
     * @return
     */
    @GetMapping("/api/v2/members")
    public Result memberV2() {
        List<Member> findMembers = memberService.findMembers();
        /**
         * List<Meber>를 List<MemberDto>에 담기
         */
        List<MemberDto> collect = findMembers
                .stream()
                .map(member -> new MemberDto(member.getName()))
                .collect(Collectors.toList());

        return new Result(collect.size(), collect);
    }

    @Data
    @AllArgsConstructor
    static class Result<T> {
        private int count;
        private T data;
    }

    @Data
    @AllArgsConstructor
    static class MemberDto {
        String name;
    }

    /**
     * 엔티티를 노출해서 개발하면 안좋다.
     * 엔티티가 변경되면 api 스펙이 변경 -> api를 이용하는 모든 팀 장애 -> 야근... ㅠ0ㅠ
     * 엔티티는 또한 어떤 파라미터가 넘어올지 모른다..
     * id, name, address, orders가 넘어오면 바인딩되서 db로 넘어가버림.
     * 개발자 입장에서는 api 문서를 조회하지 않는 이상 어떤 값들이 정확하게 넘어오는지 알수 없음.
     *
     * 결론
     * api를 개발할때는 엔티티를 파라미터로 사용하지 않는게 좋다.
     * 추가로 엔티티를 외부에 노출하기 때문에 안좋다.
     *
     * @param member
     * @return
     */
    @PostMapping("/api/v1/members")
    public CreateMemberResponse saveMemberV1(@RequestBody @Valid Member member) {
        Long id = memberService.join(member);
        return new CreateMemberResponse(id);
    }

    /**
     * dto를 만들면 어떤 값이 들어오는지 확인 가능!
     * 또한 파라미터마다 valid 되는 조건이 다를 수도 있기 때문에 V1같이 엔티티로 파라미터를 받으면 좋지 않다.
     * @param request
     * @return
     */
    @PostMapping("api/v2/members")
    public CreateMemberResponse saveMemberV2(@RequestBody @Valid CreateMemberRequest request) {
        Member member = new Member();
        member.setName(request.getName());
        Long id = memberService.join(member);

        return new CreateMemberResponse(id);
    }

    @PutMapping("api/v2/members/{id}")
    public UpdateMemberResponse updateMemberV2(@PathVariable("id") Long id,
                                               @RequestBody @Valid UpdateMemberRequest request) {
        memberService.update(id, request.getName(), request.getAddress());
        Member findMember = memberService.findMember(id);
        return new UpdateMemberResponse(findMember.getId(), findMember.getName(), findMember.getAddress());
    }

    @Data
    static class UpdateMemberRequest {
        private String name;
        private Address address;
    }

    @Data
    @AllArgsConstructor
    static class UpdateMemberResponse {
        private Long id;
        private String name;
        private Address address;
    }

    @Data
    static class CreateMemberRequest {
        @NotEmpty
        private String name;
    }

    @Data
    static class CreateMemberResponse {
        private Long id;

        public CreateMemberResponse(Long id) {
            this.id = id;
        }
    }
}
