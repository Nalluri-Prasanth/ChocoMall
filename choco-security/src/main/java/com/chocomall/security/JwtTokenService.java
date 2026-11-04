package com.chocomall.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

/** Issues and validates HS256 JWTs. A token is bound to one audience (admin or member). */
@Service
public class JwtTokenService {

    private final SecretKey key;
    private final Duration ttl;
    private final String audience;

    public JwtTokenService(SecurityProperties props) {
        SecurityProperties.Jwt jwt = props.jwt();
        if (jwt == null || jwt.secret() == null || jwt.secret().isBlank()) {
            throw new IllegalStateException("secure.jwt.secret must be configured");
        }
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwt.secret()));
        this.ttl = Duration.ofSeconds(jwt.expirationSeconds());
        this.audience = jwt.audience();
    }

    public String generate(String username) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(username)
                .audience().add(audience).and()
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(ttl)))
                .signWith(key)
                .compact();
    }

    /** Returns the username when the token is correctly signed, unexpired and issued for this application. */
    public Optional<String> validate(String token) {
        try {
            Claims claims = Jwts.parser().verifyWith(key).requireAudience(audience).build()
                    .parseSignedClaims(token).getPayload();
            return Optional.ofNullable(claims.getSubject());
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public long expirationSeconds() {
        return ttl.toSeconds();
    }
}
