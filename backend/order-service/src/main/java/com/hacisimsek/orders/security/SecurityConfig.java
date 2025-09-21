package com.hacisimsek.orders.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

@Configuration
public class SecurityConfig {
    @Bean
    SecurityFilterChain chain(HttpSecurity http, JwtService jwt) throws Exception {
        http.csrf(csrf -> csrf.disable());
        http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/**").permitAll()
                .requestMatchers("/orders/**").authenticated()   // MVP: tüm sipariş uçları token ister
                .anyRequest().permitAll());
        http.addFilterBefore(new JwtAuthFilter(jwt), BasicAuthenticationFilter.class);
        http.httpBasic(Customizer.withDefaults());
        return http.build();
    }
}
