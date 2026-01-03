package com.hacisimsek.rtos.gateway.filter;

import com.hacisimsek.rtos.gateway.config.GatewayProperties;
import com.hacisimsek.rtos.gateway.rate.GatewayRateLimiter;
import com.hacisimsek.rtos.security.JwtPrincipal;
import com.hacisimsek.rtos.security.JwtTokenValidator;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class GatewaySecurityFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(GatewaySecurityFilter.class);

    private static final String HEADER_SUBJECT = "X-RTOS-Subject";
    private static final String HEADER_ROLES = "X-RTOS-Roles";
    private static final String HEADER_CLIENT = "X-RTOS-Client";
    private static final String HEADER_CLIENT_ID = "X-Client-Id";
    private static final String HEADER_REQUEST_ID = "X-Request-Id";

    private static final List<HttpMethod> JSON_METHODS = List.of(HttpMethod.POST, HttpMethod.PUT, HttpMethod.PATCH);

    private final JwtTokenValidator validator;
    private final GatewayProperties properties;
    private final GatewayRateLimiter rateLimiter;

    public GatewaySecurityFilter(JwtTokenValidator validator,
                                 GatewayProperties properties,
                                 GatewayRateLimiter rateLimiter) {
        this.validator = validator;
        this.properties = properties;
        this.rateLimiter = rateLimiter;
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, org.springframework.cloud.gateway.filter.GatewayFilterChain chain) {
        var request = exchange.getRequest();
        String path = request.getURI().getPath();
        HttpMethod method = request.getMethod();

        if (method == null) {
            return reject(exchange, HttpStatus.METHOD_NOT_ALLOWED, "method_not_allowed", "unknown");
        }

        if (method == HttpMethod.OPTIONS || isPublicPath(path)) {
            return chain.filter(exchange);
        }

        if (!isAllowedMethod(method)) {
            return reject(exchange, HttpStatus.METHOD_NOT_ALLOWED, "method_not_allowed", "unknown");
        }

        long contentLength = request.getHeaders().getContentLength();
        if (contentLength > properties.getMaxBodyBytes()) {
            return reject(exchange, HttpStatus.PAYLOAD_TOO_LARGE, "payload_too_large", "unknown");
        }

        if (JSON_METHODS.contains(method) && requiresJson(contentLength)) {
            MediaType contentType = request.getHeaders().getContentType();
            if (contentType == null || !MediaType.APPLICATION_JSON.isCompatibleWith(contentType)) {
                return reject(exchange, HttpStatus.UNSUPPORTED_MEDIA_TYPE, "json_required", "unknown");
            }
        }

        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (!StringUtils.hasText(authHeader) || !authHeader.startsWith("Bearer ")) {
            return reject(exchange, HttpStatus.UNAUTHORIZED, "missing_bearer", "unknown");
        }

        String token = authHeader.substring(7).trim();
        if (!StringUtils.hasText(token)) {
            return reject(exchange, HttpStatus.UNAUTHORIZED, "missing_bearer", "unknown");
        }

        Optional<JwtPrincipal> principalOptional = validator.validate(token);
        if (principalOptional.isEmpty()) {
            return reject(exchange, HttpStatus.UNAUTHORIZED, "invalid_token", "unknown");
        }

        JwtPrincipal principal = principalOptional.get();
        String clientId = resolveClientId(request.getHeaders().getFirst(HEADER_CLIENT_ID), principal.subject());
        String routeKey = resolveRouteKey(path);

        if (!rateLimiter.tryConsume(routeKey, clientId)) {
            return reject(exchange, HttpStatus.TOO_MANY_REQUESTS, "rate_limited", clientId);
        }

        String requestId = resolveRequestId(request.getHeaders().getFirst(HEADER_REQUEST_ID), exchange.getRequest().getId());

        var mutatedRequest = request.mutate()
                .headers(headers -> {
                    headers.remove(HttpHeaders.AUTHORIZATION);
                    headers.remove(HEADER_SUBJECT);
                    headers.remove(HEADER_ROLES);
                    headers.remove(HEADER_CLIENT);
                    if (!StringUtils.hasText(headers.getFirst(HEADER_REQUEST_ID))) {
                        headers.add(HEADER_REQUEST_ID, requestId);
                    }
                    headers.add(HttpHeaders.AUTHORIZATION, "Bearer " + token);
                    if (StringUtils.hasText(principal.subject())) {
                        headers.add(HEADER_SUBJECT, principal.subject());
                    }
                    if (principal.roles() != null && !principal.roles().isEmpty()) {
                        headers.add(HEADER_ROLES, String.join(",", principal.roles()));
                    }
                    headers.add(HEADER_CLIENT, clientId);
                })
                .build();

        var mutatedExchange = exchange.mutate().request(mutatedRequest).build();
        long startNanos = System.nanoTime();
        return chain.filter(mutatedExchange)
                .doFinally(signalType -> logAccess(mutatedExchange, method, path, clientId, requestId, startNanos));
    }

    private boolean isPublicPath(String path) {
        if (path == null) {
            return false;
        }
        for (String prefix : properties.getPublicPaths()) {
            if (StringUtils.hasText(prefix) && path.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private boolean isAllowedMethod(HttpMethod method) {
        return properties.getAllowedMethods().contains(method.name());
    }

    private boolean requiresJson(long contentLength) {
        return contentLength != 0;
    }

    private String resolveClientId(String headerClientId, String subject) {
        if (StringUtils.hasText(headerClientId)) {
            return headerClientId.trim();
        }
        if (StringUtils.hasText(subject)) {
            return subject;
        }
        return "unknown";
    }

    private String resolveRouteKey(String path) {
        if (path == null) {
            return "default";
        }
        if (path.startsWith("/orders")) {
            return "orders";
        }
        if (path.startsWith("/inventory")) {
            return "inventory";
        }
        if (path.startsWith("/reports")) {
            return "reports";
        }
        return "default";
    }

    private String resolveRequestId(String headerRequestId, String fallback) {
        if (StringUtils.hasText(headerRequestId)) {
            return headerRequestId.trim();
        }
        return fallback != null ? fallback : "unknown";
    }

    private Mono<Void> reject(ServerWebExchange exchange, HttpStatus status, String reason, String clientId) {
        logBlocked(exchange, status, reason, clientId);
        exchange.getResponse().setStatusCode(status);
        return exchange.getResponse().setComplete();
    }

    private void logBlocked(ServerWebExchange exchange, HttpStatus status, String reason, String clientId) {
        var request = exchange.getRequest();
        String requestId = resolveRequestId(request.getHeaders().getFirst(HEADER_REQUEST_ID), request.getId());
        log.warn("gateway.blocked status={} reason={} method={} path={} client={} requestId={}",
                status.value(), reason, request.getMethod(), request.getURI().getPath(), clientId, requestId);
    }

    private void logAccess(ServerWebExchange exchange,
                           HttpMethod method,
                           String path,
                           String clientId,
                           String requestId,
                           long startNanos) {
        long durationMs = (System.nanoTime() - startNanos) / 1_000_000;
        HttpStatusCode status = exchange.getResponse().getStatusCode();
        int statusCode = status != null ? status.value() : 0;
        log.info("gateway.request method={} path={} status={} durationMs={} client={} requestId={}",
                method, path, statusCode, durationMs, clientId, requestId);
    }
}
