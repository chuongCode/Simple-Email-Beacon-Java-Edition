package com.chuongcode.emailbeacon.repository;

import com.chuongcode.emailbeacon.model.TrackingLink;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class TrackingLinkRepository {

    private final JdbcClient jdbcClient;

    public TrackingLinkRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public TrackingLink create(UUID uuid, String name, Instant createdAt) {
        jdbcClient.sql("""
                        INSERT INTO trackingLinks (name, timeGenerated, emailUUID)
                        VALUES (:name, :createdAt, :uuid)
                        """)
                .param("name", name)
                .param("createdAt", createdAt.toEpochMilli())
                .param("uuid", uuid.toString())
                .update();

        return findByUuid(uuid).orElseThrow();
    }

    public Optional<TrackingLink> findByUuid(UUID uuid) {
        return jdbcClient.sql("""
                        SELECT id, name, timeGenerated, emailUUID
                        FROM trackingLinks
                        WHERE emailUUID = :uuid
                        """)
                .param("uuid", uuid.toString())
                .query((rs, rowNum) -> new TrackingLink(
                        rs.getLong("id"),
                        UUID.fromString(rs.getString("emailUUID")),
                        rs.getString("name"),
                        Instant.ofEpochMilli(rs.getLong("timeGenerated"))))
                .optional();
    }

    public List<TrackingLink> findAll() {
        return jdbcClient.sql("""
                        SELECT id, name, timeGenerated, emailUUID
                        FROM trackingLinks
                        ORDER BY timeGenerated DESC
                        """)
                .query((rs, rowNum) -> new TrackingLink(
                        rs.getLong("id"),
                        UUID.fromString(rs.getString("emailUUID")),
                        rs.getString("name"),
                        Instant.ofEpochMilli(rs.getLong("timeGenerated"))))
                .list();
    }

    public boolean existsByUuid(UUID uuid) {
        return jdbcClient.sql("""
                        SELECT EXISTS(SELECT 1 FROM trackingLinks WHERE emailUUID = :uuid)
                        """)
                .param("uuid", uuid.toString())
                .query(Integer.class)
                .single() == 1;
    }
}
