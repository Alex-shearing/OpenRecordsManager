package com.openrecordsmanager.auth.entity;

import com.openrecordsmanager.user.User;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity(name = "auth_token")
@SuppressWarnings("NotNullFieldNotInitialized")
public class AuthToken {
    @Id
    private String tokenValue;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private LocalDateTime expiryDate;

    @Deprecated
    protected AuthToken() {
    }

    public AuthToken(String tokenValue, User user, LocalDateTime expiryDate) {
        this.tokenValue = tokenValue;
        this.user = user;
        this.expiryDate = expiryDate;
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.expiryDate);
    }

    public LocalDateTime getExpiryDate() {
        return this.expiryDate;
    }

    public User getUser() {
        return this.user;
    }

    public String getToken() {
        return this.tokenValue;
    }
}
