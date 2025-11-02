package com.hacisimsek.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.CollectionUtils;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@ConfigurationProperties(prefix = "app.security")
public class AppSecurityProperties {

    /**
     * Ordered list of shared secrets (first item used for signing, all used for verification).
     */
    private final List<String> secrets = new ArrayList<>();

    private String issuer;
    private String audience;
    private Duration clockSkew = Duration.ofSeconds(30);

    public List<String> getSecrets() {
        return Collections.unmodifiableList(secrets);
    }

    public void setSecrets(List<String> secrets) {
        this.secrets.clear();
        if (!CollectionUtils.isEmpty(secrets)) {
            secrets.stream()
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .forEach(this.secrets::add);
        }
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public String getAudience() {
        return audience;
    }

    public void setAudience(String audience) {
        this.audience = audience;
    }

    public Duration getClockSkew() {
        return clockSkew;
    }

    public void setClockSkew(Duration clockSkew) {
        if (clockSkew != null) {
            this.clockSkew = clockSkew;
        }
    }

    public boolean hasSecrets() {
        return !secrets.isEmpty();
    }

    public String primarySecret() {
        return secrets.isEmpty() ? null : secrets.get(0);
    }
}
