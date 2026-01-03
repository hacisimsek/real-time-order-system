package com.hacisimsek.rtos.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JwtTokenValidator {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenValidator.class);

    private final AppSecurityProperties properties;
    private final List<Key> verificationKeys;

    public JwtTokenValidator(AppSecurityProperties properties) {
        this.properties = properties;
        this.verificationKeys = buildKeys(properties.getSecrets());
    }

    private List<Key> buildKeys(List<String> secrets) {
        List<Key> keys = new ArrayList<>();
        if (!CollectionUtils.isEmpty(secrets)) {
            for (String s : secrets) {
                if (s != null && !s.isBlank()) {
                    keys.add(Keys.hmacShaKeyFor(s.getBytes(StandardCharsets.UTF_8)));
                }
            }
        }
        return keys;
    }

    public Optional<JwtPrincipal> validate(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        for (Key key : verificationKeys) {
            try {
                Jws<Claims> jws = Jwts.parserBuilder()
                        .requireIssuer(properties.getIssuer())
                        .requireAudience(properties.getAudience())
                        .setAllowedClockSkewSeconds(properties.getClockSkew().getSeconds())
                        .setSigningKey(key)
                        .build()
                        .parseClaimsJws(token);
                return Optional.of(toPrincipal(jws));
            } catch (Exception ex) {
                log.debug("JWT validation failed with provided key: {}", ex.getMessage());
            }
        }
        return Optional.empty();
    }

    @SuppressWarnings("unchecked")
    private JwtPrincipal toPrincipal(Jws<Claims> jws) {
        Claims c = jws.getBody();
        Object rolesClaim = c.get("roles");
        List<String> roles = new ArrayList<>();
        if (rolesClaim instanceof List<?> list) {
            for (Object item : list) {
                if (item != null) {
                    roles.add(String.valueOf(item));
                }
            }
        }
        return new JwtPrincipal(
                c.getSubject(),
                List.copyOf(roles),
                Instant.ofEpochMilli(c.getIssuedAt().getTime()),
                Instant.ofEpochMilli(c.getExpiration().getTime())
        );
    }
}
