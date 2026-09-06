package com.openrecordsmanager.auth;

import com.openrecordsmanager.api.builtin.BuiltinConfigs;
import com.openrecordsmanager.auth.dto.SessionMode;
import com.openrecordsmanager.auth.dto.TokenPair;
import com.openrecordsmanager.config.ConfigService;
import com.openrecordsmanager.database.DataRepository;
import com.openrecordsmanager.user.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtSessionService {

    public static final String CLAIM_TOKEN_TYPE = "token_type";
    public static final String CLAIM_EPOCH = "epoch";
    public static final String CLAIM_ORM_MODE = "orm_mode";
    public static final String CLAIM_USERNAME = "username";
    public static final String TOKEN_TYPE_ACCESS = "access";
    public static final String TOKEN_TYPE_REFRESH = "refresh";

    private final DataRepository repository;
    private final SecretKey signingKey;
    private final long accessTokenExpirationSeconds;
    private final long degradedAccessTokenExpirationSeconds;
    private final long refreshTokenExpirationSeconds;
    private final long degradedRefreshTokenExpirationSeconds;

    public JwtSessionService(DataRepository repository, ConfigService config) {
        this.repository = repository;
        String key = config.getOrThrow(BuiltinConfigs.JWT_SIGNING_KEY);
        this.signingKey = Keys.hmacShaKeyFor(key.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpirationSeconds = config.getOrThrow(BuiltinConfigs.ACCESS_TOKEN_EXPIRATION_SECONDS);
        this.degradedAccessTokenExpirationSeconds = config.getOrThrow(BuiltinConfigs.DEGRADED_ACCESS_TOKEN_EXPIRATION_SECONDS);
        this.refreshTokenExpirationSeconds = config.getOrThrow(BuiltinConfigs.REFRESH_TOKEN_EXPIRATION_SECONDS);
        this.degradedRefreshTokenExpirationSeconds = config.getOrThrow(BuiltinConfigs.DEGRADED_REFRESH_TOKEN_EXPIRATION_SECONDS);
    }

    public TokenPair issueTokenPair(User user, SessionMode mode) {
        Instant now = Instant.now();
        long accessTtl = mode == SessionMode.NORMAL
                ? this.accessTokenExpirationSeconds
                : this.degradedAccessTokenExpirationSeconds;
        long refreshTtl = mode == SessionMode.NORMAL
                ? this.refreshTokenExpirationSeconds
                : this.degradedRefreshTokenExpirationSeconds;

        Instant accessExpires = now.plusSeconds(accessTtl);
        Instant refreshExpires = now.plusSeconds(refreshTtl);

        String accessToken = this.sign(user, mode, TOKEN_TYPE_ACCESS, now, accessExpires);
        String refreshToken = this.sign(user, mode, TOKEN_TYPE_REFRESH, now, refreshExpires);

        return new TokenPair(accessToken, accessExpires, refreshToken, refreshExpires, mode);
    }

    public Claims verifyAccessToken(String token) {
        Claims claims = this.parse(token);
        if (!TOKEN_TYPE_ACCESS.equals(claims.get(CLAIM_TOKEN_TYPE, String.class))) {
            throw new BadCredentialsException("Token is not an access token");
        }
        return claims;
    }

    public Claims verifyRefreshToken(String token) {
        Claims claims = this.parse(token);
        if (!TOKEN_TYPE_REFRESH.equals(claims.get(CLAIM_TOKEN_TYPE, String.class))) {
            throw new BadCredentialsException("Token is not a refresh token");
        }
        return claims;
    }

    public User resolveUser(Claims claims) {
        UUID userId;
        try {
            userId = UUID.fromString(claims.getSubject());
        } catch (IllegalArgumentException e) {
            throw new BadCredentialsException("Invalid token subject");
        }

        User user = this.repository.userRepo.findById(userId)
                .orElseThrow(() -> new BadCredentialsException("User not found"));

        if (!user.isEnabled()) {
            throw new DisabledException("User account is disabled");
        }

        int epoch = claims.get(CLAIM_EPOCH, Integer.class);
        if (epoch != user.getSessionEpoch()) {
            throw new BadCredentialsException("Session has been revoked");
        }

        return user;
    }

    public SessionMode sessionModeFromClaims(Claims claims) {
        String mode = claims.get(CLAIM_ORM_MODE, String.class);
        if (SessionMode.DEGRADED_READ_ONLY.name().equals(mode)) {
            return SessionMode.DEGRADED_READ_ONLY;
        }
        return SessionMode.NORMAL;
    }

    private String sign(User user, SessionMode mode, String tokenType, Instant issuedAt, Instant expires) {
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(user.getId().toString())
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expires))
                .claim(CLAIM_USERNAME, user.getUsername())
                .claim(CLAIM_EPOCH, user.getSessionEpoch())
                .claim(CLAIM_ORM_MODE, mode.name())
                .claim(CLAIM_TOKEN_TYPE, tokenType)
                .signWith(this.signingKey)
                .compact();
    }

    private Claims parse(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(this.signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception e) {
            throw new BadCredentialsException("Invalid or expired token", e);
        }
    }
}
