package com.openrecordsmanager.auth.entity;

import com.openrecordsmanager.user.User;
import jakarta.persistence.*;

import java.time.Instant;

@Entity(name = "auth_token")
@SuppressWarnings("NotNullFieldNotInitialized")
public class AuthToken {
    @Id
    private String tokenValue;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private Instant expiryDate;

    @Deprecated
    protected AuthToken() {
    }

    public AuthToken(String tokenValue, User user, Instant expiryDate) {
        this.tokenValue = tokenValue;
        this.user = user;
        this.expiryDate = expiryDate;
    }

    public boolean isExpired() {
        return Instant.now().isAfter(this.expiryDate);
    }

    public Instant getExpiryDate() {
        return this.expiryDate;
    }

    public User getUser() {
        return this.user;
    }

    public String getToken() {
        return this.tokenValue;
    }
}
