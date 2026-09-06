package com.openrecordsmanager.auth;

import com.openrecordsmanager.api.builtin.BuiltinConfigs;
import com.openrecordsmanager.auth.dto.SessionMode;
import com.openrecordsmanager.auth.dto.TokenPair;
import com.openrecordsmanager.user.User;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class JwtSessionServiceTest {

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        com.openrecordsmanager.database.SqliteTestSupport.registerPrimaryMemoryDatabase(registry, JwtSessionServiceTest.class);
        registry.add(BuiltinConfigs.PLUGINS_SKIP_SYNC.key(), () -> "true");
        registry.add(BuiltinConfigs.COOKIE_SECURE.key(), () -> "false");
    }

    @Autowired
    private JwtSessionService jwtSessionService;

    @Autowired
    private TestAuthTokens testAuthTokens;

    @Autowired
    private com.openrecordsmanager.database.DataRepository repository;

    @Test
    void accessAndRefreshTokensHaveCorrectTypes() {
        TokenPair pair = this.testAuthTokens.tokenPairFor("admin");

        Claims access = this.jwtSessionService.verifyAccessToken(pair.accessToken());
        assertEquals(JwtSessionService.TOKEN_TYPE_ACCESS, access.get(JwtSessionService.CLAIM_TOKEN_TYPE, String.class));
        assertEquals(SessionMode.NORMAL.name(), access.get(JwtSessionService.CLAIM_ORM_MODE, String.class));

        Claims refresh = this.jwtSessionService.verifyRefreshToken(pair.refreshToken());
        assertEquals(JwtSessionService.TOKEN_TYPE_REFRESH, refresh.get(JwtSessionService.CLAIM_TOKEN_TYPE, String.class));

        assertThrows(BadCredentialsException.class, () -> this.jwtSessionService.verifyAccessToken(pair.refreshToken()));
        assertThrows(BadCredentialsException.class, () -> this.jwtSessionService.verifyRefreshToken(pair.accessToken()));
    }

    @Test
    void epochMismatchRejectsToken() {
        User admin = this.repository.userRepo.findByUsername("admin").orElseThrow();
        TokenPair pair = this.jwtSessionService.issueTokenPair(admin, SessionMode.NORMAL);

        admin.bumpSessionEpoch();
        this.repository.userRepo.saveAndFlush(admin);

        Claims claims = this.jwtSessionService.verifyAccessToken(pair.accessToken());
        assertThrows(BadCredentialsException.class, () -> this.jwtSessionService.resolveUser(claims));
    }

    @Test
    void tamperedTokenIsRejected() {
        TokenPair pair = this.testAuthTokens.tokenPairFor("admin");
        String tampered = pair.accessToken() + "x";
        assertThrows(BadCredentialsException.class, () -> this.jwtSessionService.verifyAccessToken(tampered));
    }
}
