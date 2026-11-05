package com.doceflux.doceflux_api.auth.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.security.jwt")
public record JwtProperties(String secret, long expirationSeconds) {
}
