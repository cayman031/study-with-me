package com.studywithme.member.repository;

import com.studywithme.member.domain.LoginAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoginAttemptRepository extends JpaRepository<LoginAttempt, String> {
}
