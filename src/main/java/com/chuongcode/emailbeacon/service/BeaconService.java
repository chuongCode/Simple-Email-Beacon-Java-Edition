package com.chuongcode.emailbeacon.service;

import com.chuongcode.emailbeacon.config.BeaconProperties;
import com.chuongcode.emailbeacon.model.TrackingLink;
import com.chuongcode.emailbeacon.model.Visit;
import com.chuongcode.emailbeacon.repository.TrackingLinkRepository;
import com.chuongcode.emailbeacon.repository.VisitRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class BeaconService {

    private final TrackingLinkRepository trackingLinkRepository;
    private final VisitRepository visitRepository;
    private final ClientMetadataService clientMetadataService;
    private final BeaconProperties properties;
    private final Clock clock;

    public BeaconService(
            TrackingLinkRepository trackingLinkRepository,
            VisitRepository visitRepository,
            ClientMetadataService clientMetadataService,
            BeaconProperties properties
    ) {
        this.trackingLinkRepository = trackingLinkRepository;
        this.visitRepository = visitRepository;
        this.clientMetadataService = clientMetadataService;
        this.properties = properties;
        this.clock = Clock.systemUTC();
    }

    @Transactional
    public TrackingLink create(String name) {
        return trackingLinkRepository.create(
                UUID.randomUUID(),
                name == null ? "" : name.strip(),
                Instant.now(clock));
    }

    @Transactional(readOnly = true)
    public List<TrackingLink> findAll() {
        return trackingLinkRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<TrackingLink> find(UUID uuid) {
        return trackingLinkRepository.findByUuid(uuid);
    }

    @Transactional(readOnly = true)
    public List<Visit> findVisits(UUID uuid) {
        return visitRepository.findByEmailUuid(uuid);
    }

    @Transactional
    public Optional<TrackingLink> rename(UUID uuid, String name) {
        String normalizedName = name == null ? "" : name.strip();
        if (!trackingLinkRepository.updateName(uuid, normalizedName)) {
            return Optional.empty();
        }
        return trackingLinkRepository.findByUuid(uuid);
    }

    @Transactional
    public boolean delete(UUID uuid) {
        if (!trackingLinkRepository.existsByUuid(uuid)) {
            return false;
        }
        visitRepository.deleteByEmailUuid(uuid);
        return trackingLinkRepository.deleteByUuid(uuid);
    }

    @Transactional
    public boolean recordVisit(UUID uuid, HttpServletRequest request) {
        if (!trackingLinkRepository.existsByUuid(uuid)) {
            return false;
        }

        visitRepository.create(
                Instant.now(clock),
                clientMetadataService.resolveIpAddress(request),
                clientMetadataService.serializeSessionData(request),
                uuid);
        return true;
    }

    public String pixelUrl(UUID uuid) {
        return properties.publicBaseUrl() + "/emailBeacon?UUID=" + uuid;
    }

    public String statusUrl(UUID uuid) {
        return properties.publicBaseUrl() + "/emailBeaconStatus?UUID=" + uuid;
    }
}
