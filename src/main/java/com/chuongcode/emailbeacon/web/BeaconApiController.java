package com.chuongcode.emailbeacon.web;

import com.chuongcode.emailbeacon.model.TrackingLink;
import com.chuongcode.emailbeacon.model.Visit;
import com.chuongcode.emailbeacon.service.BeaconService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/beacons")
public class BeaconApiController {

    private final BeaconService beaconService;

    public BeaconApiController(BeaconService beaconService) {
        this.beaconService = beaconService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BeaconResponse create(@Valid @RequestBody(required = false) CreateBeaconRequest request) {
        String name = request == null ? "" : request.name();
        return toResponse(beaconService.create(name));
    }

    @GetMapping
    public List<BeaconResponse> list() {
        return beaconService.findAll().stream().map(this::toResponse).toList();
    }

    private BeaconResponse toResponse(TrackingLink link) {
        List<Visit> visits = beaconService.findVisits(link.uuid());
        return new BeaconResponse(
                link.uuid(),
                link.name(),
                link.createdAt(),
                beaconService.pixelUrl(link.uuid()),
                beaconService.statusUrl(link.uuid()),
                !visits.isEmpty(),
                visits.size(),
                visits.isEmpty() ? null : visits.getFirst().visitedAt());
    }

    public record CreateBeaconRequest(@Size(max = 120) String name) {
    }

    public record BeaconResponse(
            UUID id,
            String name,
            Instant createdAt,
            String pixelUrl,
            String statusUrl,
            boolean opened,
            int visitCount,
            Instant lastOpenedAt
    ) {
    }
}

