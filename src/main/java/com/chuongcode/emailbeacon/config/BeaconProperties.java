package com.chuongcode.emailbeacon.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "beacon")
public record BeaconProperties(
        String publicBaseUrl,
        List<String> allowedOrigins,
        boolean trustForwardedHeaders
) {
    public BeaconProperties {
        publicBaseUrl = normalizeBaseUrl(publicBaseUrl);
        allowedOrigins = allowedOrigins == null ? List.of() : List.copyOf(allowedOrigins);
    }

    private static String normalizeBaseUrl(String value) {
        if (value == null || value.isBlank()) {
            return "http://localhost:8080";
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}

