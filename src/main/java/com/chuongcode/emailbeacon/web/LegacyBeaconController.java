package com.chuongcode.emailbeacon.web;

import com.chuongcode.emailbeacon.model.Visit;
import com.chuongcode.emailbeacon.service.BeaconService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
public class LegacyBeaconController {

    private static final MediaType IMAGE_GIF = MediaType.parseMediaType("image/gif");

    private final BeaconService beaconService;

    public LegacyBeaconController(BeaconService beaconService) {
        this.beaconService = beaconService;
    }

    @GetMapping(value = "/generateUUID", produces = MediaType.TEXT_PLAIN_VALUE)
    public String generateUuid() {
        return beaconService.pixelUrl(beaconService.create("").uuid());
    }

    @GetMapping(value = "/emailBeacon", produces = "image/gif")
    public ResponseEntity<byte[]> recordVisit(
            @RequestParam(name = "UUID", required = false) String rawUuid,
            HttpServletRequest request
    ) {
        parseUuid(rawUuid).ifPresent(uuid -> beaconService.recordVisit(uuid, request));

        return ResponseEntity.ok()
                .contentType(IMAGE_GIF)
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.PRAGMA, "no-cache")
                .body(TrackingPixel.GIF_BYTES);
    }

    @GetMapping(value = "/emailBeaconStatus", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> status(@RequestParam(name = "UUID", required = false) String rawUuid) {
        var uuid = parseUuid(rawUuid);
        if (uuid.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Please add a valid UUID parameter."));
        }

        List<Visit> visits = beaconService.findVisits(uuid.get());
        if (visits.isEmpty()) {
            return ResponseEntity.ok().contentType(MediaType.TEXT_PLAIN).body("Unread...");
        }

        return ResponseEntity.ok(visits.stream().map(LegacyVisitResponse::from).toList());
    }

    private java.util.Optional<UUID> parseUuid(String value) {
        if (value == null || value.isBlank()) {
            return java.util.Optional.empty();
        }
        try {
            return java.util.Optional.of(UUID.fromString(value));
        } catch (IllegalArgumentException exception) {
            return java.util.Optional.empty();
        }
    }

    private record LegacyVisitResponse(
            long id,
            long timeVisited,
            String loggedIPAddress,
            String sessionData,
            String emailUUID
    ) {
        static LegacyVisitResponse from(Visit visit) {
            return new LegacyVisitResponse(
                    visit.id(),
                    visit.visitedAt().toEpochMilli(),
                    visit.ipAddress(),
                    visit.sessionData(),
                    visit.emailUuid().toString());
        }
    }
}

