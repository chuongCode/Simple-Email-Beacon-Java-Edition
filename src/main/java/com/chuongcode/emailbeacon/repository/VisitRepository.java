package com.chuongcode.emailbeacon.repository;

import com.chuongcode.emailbeacon.model.Visit;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public class VisitRepository {

    private final JdbcClient jdbcClient;

    public VisitRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public void create(Instant visitedAt, String ipAddress, String sessionData, UUID emailUuid) {
        jdbcClient.sql("""
                        INSERT INTO linkVisits (timeVisited, loggedIPAddress, sessionData, emailUUID)
                        VALUES (:visitedAt, :ipAddress, :sessionData, :emailUuid)
                        """)
                .param("visitedAt", visitedAt.toEpochMilli())
                .param("ipAddress", ipAddress)
                .param("sessionData", sessionData)
                .param("emailUuid", emailUuid.toString())
                .update();
    }

    public List<Visit> findByEmailUuid(UUID emailUuid) {
        return jdbcClient.sql("""
                        SELECT id, timeVisited, loggedIPAddress, sessionData, emailUUID
                        FROM linkVisits
                        WHERE emailUUID = :emailUuid
                        ORDER BY timeVisited DESC
                        """)
                .param("emailUuid", emailUuid.toString())
                .query((rs, rowNum) -> new Visit(
                        rs.getLong("id"),
                        Instant.ofEpochMilli(rs.getLong("timeVisited")),
                        rs.getString("loggedIPAddress"),
                        rs.getString("sessionData"),
                        UUID.fromString(rs.getString("emailUUID"))))
                .list();
    }

    public void deleteByEmailUuid(UUID emailUuid) {
        jdbcClient.sql("DELETE FROM linkVisits WHERE emailUUID = :emailUuid")
                .param("emailUuid", emailUuid.toString())
                .update();
    }
}
