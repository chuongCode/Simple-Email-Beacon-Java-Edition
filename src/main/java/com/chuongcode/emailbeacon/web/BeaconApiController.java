package com.chuongcode.emailbeacon.web;

import com.chuongcode.emailbeacon.model.TrackingLink;
import com.chuongcode.emailbeacon.model.Visit;
import com.chuongcode.emailbeacon.service.BeaconService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

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

    @GetMapping("/{uuid}")
    public BeaconResponse get(@PathVariable UUID uuid) {
        return beaconService.find(uuid)
                .map(this::toResponse)
                .orElseThrow(() -> notFound(uuid));
    }

    @PatchMapping("/{uuid}")
    public BeaconResponse rename(
            @PathVariable UUID uuid,
            @Valid @RequestBody UpdateBeaconRequest request
    ) {
        return beaconService.rename(uuid, request.name())
                .map(this::toResponse)
                .orElseThrow(() -> notFound(uuid));
    }

    @DeleteMapping("/{uuid}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID uuid) {
        if (!beaconService.delete(uuid)) {
            throw notFound(uuid);
        }
    }

    @GetMapping("/{uuid}/visits")
    public List<VisitResponse> visits(@PathVariable UUID uuid) {
        if (beaconService.find(uuid).isEmpty()) {
            throw notFound(uuid);
        }
        return beaconService.findVisits(uuid).stream().map(VisitResponse::from).toList();
    }

    private BeaconResponse toResponse(TrackingLink link) {
        List<Visit> visits = beaconService.findVisits(link.uuid());
        return new BeaconResponse(
                link.uuid(),
                link.name() == null ? "" : link.name(),
                link.createdAt(),
                beaconService.pixelUrl(link.uuid()),
                beaconService.statusUrl(link.uuid()),
                !visits.isEmpty(),
                visits.size(),
                visits.isEmpty() ? null : visits.getFirst().visitedAt());
    }

    private ResponseStatusException notFound(UUID uuid) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Beacon " + uuid + " was not found");
    }

    public record CreateBeaconRequest(@Size(max = 120) String name) {
    }

    public record UpdateBeaconRequest(@NotNull @Size(max = 120) String name) {
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

    public record VisitResponse(
            long id,
            Instant visitedAt,
            String ipAddress,
            String sessionData
    ) {
        static VisitResponse from(Visit visit) {
            return new VisitResponse(
                    visit.id(),
                    visit.visitedAt(),
                    visit.ipAddress(),
                    visit.sessionData());
        }
    }
}
