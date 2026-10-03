package com.chuongcode.emailbeacon.model;

import java.time.Instant;
import java.util.UUID;

public record Visit(
        long id,
        Instant visitedAt,
        String ipAddress,
        String userAgent,
        String sessionData,
        UUID emailUuid,
        VisitClassification classification,
        String visitorHash,
        boolean duplicate,
        boolean testVisit
) {
}
