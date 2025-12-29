package com.studywithme.member.controller;

import com.studywithme.global.response.ApiResponse;
import com.studywithme.global.security.SecurityUtils;
import com.studywithme.member.dto.MemberMeResponse;
import com.studywithme.member.service.MemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Member")
@RestController
@RequestMapping("/members")
public class MemberController {
    private final MemberService memberService;

    public MemberController(MemberService memberService) {
        this.memberService = memberService;
    }

    @Operation(summary = "내 프로필 조회")
    @GetMapping("/me")
    public ApiResponse<MemberMeResponse> me() {
        Long memberId = SecurityUtils.currentMemberId();
        return ApiResponse.success(memberService.getMe(memberId));
    }
}
