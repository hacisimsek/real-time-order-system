package com.hacisimsek.rtos.security;

import java.time.Instant;
import java.util.List;

public record JwtPrincipal(
        String subject,
        List<String> roles,
        Instant issuedAt,
        Instant expiresAt
) { }

