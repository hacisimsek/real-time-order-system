package com.hacisimsek.orders.security;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import java.util.List;

@Profile("dev")
@Component
public class DevTokenRunner implements CommandLineRunner {
    private final JwtService jwt;
    public DevTokenRunner(JwtService jwt){ this.jwt = jwt; }

    @Override public void run(String... args) {
        String admin = jwt.generate("admin@rtos.local", List.of("ROLE_ADMIN"), 3600);
        System.out.println("DEV ADMIN TOKEN (1h): Bearer " + admin);
    }
}
