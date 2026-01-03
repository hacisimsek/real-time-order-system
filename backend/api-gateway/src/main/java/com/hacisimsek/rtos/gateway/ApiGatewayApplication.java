package com.hacisimsek.rtos.gateway;

import com.hacisimsek.rtos.gateway.config.GatewayProperties;
import com.hacisimsek.rtos.security.AppSecurityProperties;
import com.hacisimsek.rtos.security.JwtTokenValidator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.actuate.autoconfigure.security.reactive.ReactiveManagementWebSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.reactive.ReactiveSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@SpringBootApplication(exclude = {
        SecurityAutoConfiguration.class,
        ReactiveSecurityAutoConfiguration.class,
        ReactiveManagementWebSecurityAutoConfiguration.class
})
@EnableConfigurationProperties({AppSecurityProperties.class, GatewayProperties.class})
public class ApiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }

    @Bean
    JwtTokenValidator jwtTokenValidator(AppSecurityProperties properties) {
        return new JwtTokenValidator(properties);
    }
}
