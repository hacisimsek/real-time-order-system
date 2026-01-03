package com.hacisimsek.rtos.notification.config;

import com.hacisimsek.rtos.security.JwtAuthenticationEntryPoint;
import com.hacisimsek.rtos.security.JwtAuthenticationFilter;
import com.hacisimsek.rtos.security.JwtSecurityConfiguration;
import com.hacisimsek.rtos.security.Roles;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
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
    SecurityFilterChain notificationSecurity(HttpSecurity http,
                                             JwtAuthenticationFilter jwtFilter,
                                             JwtAuthenticationEntryPoint entryPoint) throws Exception {
        http.csrf(csrf -> csrf.disable());
        http.sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/**").permitAll()
                .anyRequest().hasAuthority(Roles.NOTIFICATION_READ));
        http.exceptionHandling(ex -> ex.authenticationEntryPoint(entryPoint));
        http.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
