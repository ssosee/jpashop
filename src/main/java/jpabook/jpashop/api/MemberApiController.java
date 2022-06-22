package jpabook.jpashop.api;

import jpabook.jpashop.domain.Member;
import jpabook.jpashop.service.MemberService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;

@RestController
@RequiredArgsConstructor
public class MemberApiController {
    private final MemberService memberService;

    /**
     * Entity를 노출해서 개발하면 안좋다.
     * Entity가 변경되면 api 스펙이 변경 -> api를 이용하는 모든 팀 장애 -> 야근... ㅠ0ㅠ
     * Entity는 또한 어떤 파라미터가 넘어올지 모른다..
     * id, name, address, orders가 넘어오면 바인딩되서 db로 넘어가버림.
     * 개발자 입장에서는 api 문서를 조회하지 않는 이상 어떤 값들이 정확하게 넘어오는지 알수 없음.
     *
     * 결론
     * api를 개발할때는 Entity를 파라미터로 사용하지 않는게 좋다.
     * 추가로 Entity를 외부에 노출하기 때문에 안좋다.
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
     * 또한 파라미터마다 valid 되는 조건이 다를 수도 있기 때문에 V1같이 entity로 파라미터를 받으면 좋지 않다.
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
        memberService.update(id, request.getName());
        Member findMember = memberService.findMember(id);
        return new UpdateMemberResponse(findMember.getId(), findMember.getName());
    }

    @Data
    static class UpdateMemberRequest {
        private String name;
    }

    @Data
    @AllArgsConstructor
    static class UpdateMemberResponse {
        private Long id;
        private String name;
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
