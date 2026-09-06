package com.openrecordsmanager.auth;

import com.openrecordsmanager.auth.dto.SessionMode;
import com.openrecordsmanager.auth.dto.TokenPair;
import com.openrecordsmanager.database.DataRepository;
import com.openrecordsmanager.user.User;
import org.springframework.stereotype.Component;

@Component
public class TestAuthTokens {

    private final DataRepository repository;
    private final JwtSessionService jwtSessionService;

    public TestAuthTokens(DataRepository repository, JwtSessionService jwtSessionService) {
        this.repository = repository;
        this.jwtSessionService = jwtSessionService;
    }

    public String adminAccessToken() {
        return this.accessTokenFor("admin");
    }

    public String accessTokenFor(String username) {
        User user = this.repository.userRepo.findByUsername(username).orElseThrow();
        return this.tokenPairFor(user).accessToken();
    }

    public TokenPair tokenPairFor(User user) {
        return this.jwtSessionService.issueTokenPair(user, SessionMode.NORMAL);
    }

    public TokenPair tokenPairFor(String username) {
        User user = this.repository.userRepo.findByUsername(username).orElseThrow();
        return this.tokenPairFor(user);
    }

    public TokenPair degradedTokenPairFor(String username) {
        User user = this.repository.userRepo.findByUsername(username).orElseThrow();
        return this.jwtSessionService.issueTokenPair(user, SessionMode.DEGRADED_READ_ONLY);
    }
}
