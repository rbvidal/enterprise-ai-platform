package com.cognitera.platform.auth.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/** Configuration properties for auth: JWT issuer, secret, and token TTLs. */
@ConfigurationProperties(prefix = "platform.auth")
public class AuthProperties {

    private String issuer = "enterprise-ai-platform";
    private String jwtSecret = "change-this-development-secret-change-this-development-secret";
    private Duration accessTokenTtl = Duration.ofMinutes(15);
    private Duration refreshTokenTtl = Duration.ofDays(30);

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public String getJwtSecret() {
        return jwtSecret;
    }

    public void setJwtSecret(String jwtSecret) {
        this.jwtSecret = jwtSecret;
    }

    public Duration getAccessTokenTtl() {
        return accessTokenTtl;
    }

    public void setAccessTokenTtl(Duration accessTokenTtl) {
        this.accessTokenTtl = accessTokenTtl;
    }

    public Duration getRefreshTokenTtl() {
        return refreshTokenTtl;
    }

    public void setRefreshTokenTtl(Duration refreshTokenTtl) {
        this.refreshTokenTtl = refreshTokenTtl;
    }
}
