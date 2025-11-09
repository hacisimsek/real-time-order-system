package com.hacisimsek.orders.security;

import com.hacisimsek.security.JwtTokenIssuer;
import com.hacisimsek.security.Roles;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

@Profile("dev")
@Component
public class DevTokenRunner implements CommandLineRunner {
    private final JwtTokenIssuer issuer;
    public DevTokenRunner(JwtTokenIssuer issuer){ this.issuer = issuer; }

    @Override public void run(String... args) {
        String admin = issuer.issue(
                "admin@rtos.local",
                List.of(
                        Roles.ORDER_READ,
                        Roles.ORDER_WRITE,
                        Roles.INVENTORY_READ,
                        Roles.INVENTORY_WRITE,
                        Roles.INVENTORY_OPS,
                        Roles.NOTIFICATION_READ,
                        Roles.REPORTING_READ,
                        Roles.REPORTING_EXPORT
                ),
                Duration.ofHours(1)
        );
        System.out.println("DEV ADMIN TOKEN (1h): Bearer " + admin);
    }
}
