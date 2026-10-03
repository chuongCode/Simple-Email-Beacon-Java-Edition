package com.chuongcode.emailbeacon.model;

import java.time.Instant;
import java.util.UUID;

public record TrackingLink(long id, UUID uuid, String name, Instant createdAt) {
}

