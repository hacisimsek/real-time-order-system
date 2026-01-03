package com.hacisimsek.rtos.inventory.config;

import com.hacisimsek.rtos.security.JwtAuthenticationEntryPoint;
import com.hacisimsek.rtos.security.JwtAuthenticationFilter;
import com.hacisimsek.rtos.security.JwtSecurityConfiguration;
import com.hacisimsek.rtos.security.Roles;
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
    SecurityFilterChain inventorySecurityFilter(HttpSecurity http,
                                                JwtAuthenticationFilter jwtFilter,
                                                JwtAuthenticationEntryPoint entryPoint) throws Exception {
        http.csrf(csrf -> csrf.disable());
        http.sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/inventory/**").hasAnyAuthority(Roles.INVENTORY_READ, Roles.INVENTORY_WRITE)
                .requestMatchers(HttpMethod.PUT, "/inventory/**").hasAuthority(Roles.INVENTORY_WRITE)
                .requestMatchers(HttpMethod.POST, "/inventory/**").hasAuthority(Roles.INVENTORY_WRITE)
                .requestMatchers("/ops/**").hasAuthority(Roles.INVENTORY_OPS)
                .anyRequest().authenticated());
        http.exceptionHandling(ex -> ex.authenticationEntryPoint(entryPoint));
        http.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
