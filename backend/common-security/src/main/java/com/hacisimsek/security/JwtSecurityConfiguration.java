package com.hacisimsek.security;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(AppSecurityProperties.class)
public class JwtSecurityConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public JwtTokenValidator jwtTokenValidator(AppSecurityProperties props) {
        return new JwtTokenValidator(props);
    }

    @Bean
    @ConditionalOnMissingBean
    public JwtTokenIssuer jwtTokenIssuer(AppSecurityProperties props) {
        return new JwtTokenIssuer(props);
    }

    @Bean
    @ConditionalOnMissingBean
    public JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint() {
        return new JwtAuthenticationEntryPoint();
    }

    @Bean
    @ConditionalOnMissingBean
    public JwtAuthenticationFilter jwtAuthenticationFilter(JwtTokenValidator validator) {
        return new JwtAuthenticationFilter(validator);
    }
}
