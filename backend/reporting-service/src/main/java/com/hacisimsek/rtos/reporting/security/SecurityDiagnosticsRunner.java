package com.hacisimsek.rtos.reporting.security;

import com.hacisimsek.rtos.security.AppSecurityProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Emits a one-time log describing the JWT settings wired into the service so we
 * can quickly confirm env-specific secrets/issuer/audience. Enabled only in dev.
 */
@Profile("dev")
@Component
public class SecurityDiagnosticsRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(SecurityDiagnosticsRunner.class);

    private final AppSecurityProperties securityProperties;

    public SecurityDiagnosticsRunner(AppSecurityProperties securityProperties) {
        this.securityProperties = securityProperties;
    }

    @Override
    public void run(String... args) {
        log.info("Reporting security config → issuer='{}', audience='{}', secrets={}",
                securityProperties.getIssuer(),
                securityProperties.getAudience(),
                securityProperties.getSecrets().size());
    }
}
