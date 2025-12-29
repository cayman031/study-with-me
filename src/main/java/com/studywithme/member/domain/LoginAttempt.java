package com.studywithme.member.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "login_attempt")
public class LoginAttempt {

    @Id
    @Column(length = 255)
    private String email;

    @Column(name = "fail_count", nullable = false)
    private int failCount;

    @Column(name = "blocked_until")
    private LocalDateTime blockedUntil;

    @Column(name = "last_failed_at")
    private LocalDateTime lastFailedAt;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    protected LoginAttempt() {
    }

    public LoginAttempt(String email) {
        this.email = email;
        this.failCount = 0;
    }

    public String getEmail() {
        return email;
    }

    public int getFailCount() {
        return failCount;
    }

    public LocalDateTime getBlockedUntil() {
        return blockedUntil;
    }

    public void recordFailure(int maxFailCount, int blockMinutes, LocalDateTime now) {
        this.failCount += 1;
        this.lastFailedAt = now;
        if (this.failCount >= maxFailCount) {
            this.blockedUntil = now.plusMinutes(blockMinutes);
        }
    }

    public void reset() {
        this.failCount = 0;
        this.blockedUntil = null;
        this.lastFailedAt = null;
    }
}
