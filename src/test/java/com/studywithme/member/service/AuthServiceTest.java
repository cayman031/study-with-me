package com.studywithme.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.studywithme.global.exception.ApiException;
import com.studywithme.global.exception.ErrorCode;
import com.studywithme.member.domain.Member;
import com.studywithme.member.domain.MemberRole;
import com.studywithme.member.domain.MemberStatus;
import com.studywithme.member.dto.SignupRequest;
import com.studywithme.member.repository.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class AuthServiceTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("회원가입 시 이메일 중복이면 예외가 발생한다")
    void signup_duplicateEmail_throwsException() {
        Member member = new Member(
                "dup@studywithme.com",
                passwordEncoder.encode("password123"),
                "dup",
                MemberRole.PARTICIPANT,
                MemberStatus.ACTIVE
        );
        memberRepository.save(member);

        SignupRequest request = new SignupRequest(
                "dup@studywithme.com",
                "password123",
                "dup",
                MemberRole.PARTICIPANT
        );

        assertThatThrownBy(() -> authService.signup(request))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException apiException = (ApiException) ex;
                    assertThat(apiException.getErrorCode()).isEqualTo(ErrorCode.MEMBER_EMAIL_DUPLICATED);
                });
    }
}
