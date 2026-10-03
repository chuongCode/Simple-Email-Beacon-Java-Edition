package com.chuongcode.emailbeacon.service;

import com.chuongcode.emailbeacon.config.BeaconProperties;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class ClientMetadataService {

    private final BeaconProperties properties;
    private final ObjectMapper objectMapper;

    public ClientMetadataService(BeaconProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public String resolveIpAddress(HttpServletRequest request) {
        if (properties.trustForwardedHeaders()) {
            String forwardedFor = request.getHeader("X-Forwarded-For");
            if (forwardedFor != null && !forwardedFor.isBlank()) {
                return forwardedFor.split(",", 2)[0].trim();
            }
        }
        return request.getRemoteAddr();
    }

    public String serializeSessionData(HttpServletRequest request) {
        Map<String, String> data = new LinkedHashMap<>();
        addIfPresent(data, "userAgent", request.getHeader("User-Agent"));
        addIfPresent(data, "referer", request.getHeader("Referer"));
        addIfPresent(data, "acceptLanguage", request.getHeader("Accept-Language"));

        try {
            return objectMapper.writeValueAsString(data);
        } catch (JacksonException exception) {
            throw new IllegalStateException("Could not serialize request metadata", exception);
        }
    }

    private void addIfPresent(Map<String, String> target, String key, String value) {
        if (value != null && !value.isBlank()) {
            target.put(key, value);
        }
    }
}
