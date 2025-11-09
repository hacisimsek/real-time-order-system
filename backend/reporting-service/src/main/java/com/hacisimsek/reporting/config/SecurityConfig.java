package com.hacisimsek.reporting.config;

import com.hacisimsek.security.JwtAuthenticationEntryPoint;
import com.hacisimsek.security.JwtAuthenticationFilter;
import com.hacisimsek.security.JwtSecurityConfiguration;
import com.hacisimsek.security.Roles;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
@Import(JwtSecurityConfiguration.class)
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http,
                                            JwtAuthenticationFilter jwtFilter,
                                            JwtAuthenticationEntryPoint entryPoint) throws Exception {
        http.csrf(csrf -> csrf.disable());
        http.sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/reports/orders/export")
                .hasAuthority(Roles.REPORTING_EXPORT)
                .requestMatchers(HttpMethod.GET, "/reports/**")
                .hasAnyAuthority(Roles.REPORTING_READ, Roles.REPORTING_EXPORT)
                .anyRequest().authenticated());
        http.exceptionHandling(ex -> ex.authenticationEntryPoint(entryPoint));
        http.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
