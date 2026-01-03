package com.hacisimsek.rtos.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.util.Assert;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class JwtTokenIssuer {

    private final AppSecurityProperties properties;
    private final Key signingKey;

    public JwtTokenIssuer(AppSecurityProperties properties) {
        Assert.notNull(properties, "properties cannot be null");
        this.properties = properties;
        String secret = properties.primarySecret();
        Assert.hasText(secret, "app.security.secrets[0] must be provided");
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String issue(String subject, List<String> roles, Duration ttl) {
        Assert.hasText(subject, "subject must not be blank");
        Assert.notNull(ttl, "ttl must not be null");
        Instant now = Instant.now();
        return Jwts.builder()
                .setSubject(subject)
                .setIssuer(properties.getIssuer())
                .setAudience(properties.getAudience())
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plus(ttl)))
                .addClaims(Map.of("roles", roles == null ? List.of() : roles))
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }
}
