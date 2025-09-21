package com.hacisimsek.orders.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import java.io.IOException;
import java.util.List;

public class JwtAuthFilter implements Filter {
    private final JwtService jwt;

    public JwtAuthFilter(JwtService jwt){ this.jwt = jwt; }

    @Override public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest r = (HttpServletRequest) req;
        String auth = r.getHeader(HttpHeaders.AUTHORIZATION);
        if (StringUtils.hasText(auth) && auth.startsWith("Bearer ")) {
            try {
                var jws = jwt.parse(auth.substring(7));
                Claims c = jws.getBody();
                var roles = (List<?>) c.getOrDefault("roles", List.of());
                var authorities = roles.stream().map(Object::toString).map(SimpleGrantedAuthority::new).toList();
                var authToken = new UsernamePasswordAuthenticationToken(c.getSubject(), "N/A", authorities);
                SecurityContextHolder.getContext().setAuthentication(authToken);
            } catch (Exception ignored) {}
        }
        chain.doFilter(req, res);
    }
}
