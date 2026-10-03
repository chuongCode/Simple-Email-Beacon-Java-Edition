package com.chuongcode.emailbeacon.model;

import java.time.Instant;
import java.util.UUID;

public record Visit(
        long id,
        Instant visitedAt,
        String ipAddress,
        String sessionData,
        UUID emailUuid
) {
}

