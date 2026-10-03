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
    private final VisitClassifier visitClassifier;
    private final BeaconProperties properties;
    private final Clock clock;

    public BeaconService(
            TrackingLinkRepository trackingLinkRepository,
            VisitRepository visitRepository,
            ClientMetadataService clientMetadataService,
            VisitClassifier visitClassifier,
            BeaconProperties properties
    ) {
        this.trackingLinkRepository = trackingLinkRepository;
        this.visitRepository = visitRepository;
        this.clientMetadataService = clientMetadataService;
        this.visitClassifier = visitClassifier;
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
        return recordVisit(uuid, request, false);
    }

    @Transactional
    public boolean recordVisit(UUID uuid, HttpServletRequest request, boolean testVisit) {
        if (!trackingLinkRepository.existsByUuid(uuid)) {
            return false;
        }

        Instant visitedAt = Instant.now(clock);
        String ipAddress = clientMetadataService.resolveIpAddress(request);
        String userAgent = clientMetadataService.resolveUserAgent(request);
        String visitorHash = visitClassifier.visitorHash(ipAddress, userAgent);
        boolean duplicate = visitRepository.existsRecentVisitorHash(
                uuid,
                visitorHash,
                visitedAt.minusSeconds(properties.deduplicationWindowSeconds()));

        visitRepository.create(
                visitedAt,
                ipAddress,
                userAgent,
                clientMetadataService.serializeSessionData(request),
                uuid,
                visitClassifier.classify(userAgent, testVisit),
                visitorHash,
                duplicate,
                testVisit);
        return true;
    }

    public String pixelUrl(UUID uuid) {
        return properties.publicBaseUrl() + "/emailBeacon?UUID=" + uuid;
    }

    public String statusUrl(UUID uuid) {
        return properties.publicBaseUrl() + "/emailBeaconStatus?UUID=" + uuid;
    }
}
