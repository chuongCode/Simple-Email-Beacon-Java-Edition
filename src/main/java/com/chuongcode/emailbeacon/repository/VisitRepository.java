package com.chuongcode.emailbeacon.repository;

import com.chuongcode.emailbeacon.model.Visit;
import com.chuongcode.emailbeacon.model.VisitClassification;
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

    public void create(
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
        jdbcClient.sql("""
                        INSERT INTO linkVisits (
                            timeVisited, loggedIPAddress, userAgent, sessionData, emailUUID,
                            classification, visitorHash, isDuplicate, testVisit
                        )
                        VALUES (
                            :visitedAt, :ipAddress, :userAgent, :sessionData, :emailUuid,
                            :classification, :visitorHash, :duplicate, :testVisit
                        )
                        """)
                .param("visitedAt", visitedAt.toEpochMilli())
                .param("ipAddress", ipAddress)
                .param("userAgent", userAgent)
                .param("sessionData", sessionData)
                .param("emailUuid", emailUuid.toString())
                .param("classification", classification.name())
                .param("visitorHash", visitorHash)
                .param("duplicate", duplicate ? 1 : 0)
                .param("testVisit", testVisit ? 1 : 0)
                .update();
    }

    public List<Visit> findByEmailUuid(UUID emailUuid) {
        return jdbcClient.sql("""
                        SELECT id, timeVisited, loggedIPAddress, userAgent, sessionData, emailUUID,
                               classification, visitorHash, isDuplicate, testVisit
                        FROM linkVisits
                        WHERE emailUUID = :emailUuid
                        ORDER BY timeVisited DESC
                        """)
                .param("emailUuid", emailUuid.toString())
                .query((rs, rowNum) -> new Visit(
                        rs.getLong("id"),
                        Instant.ofEpochMilli(rs.getLong("timeVisited")),
                        rs.getString("loggedIPAddress"),
                        rs.getString("userAgent"),
                        rs.getString("sessionData"),
                        UUID.fromString(rs.getString("emailUUID")),
                        VisitClassification.valueOf(rs.getString("classification")),
                        rs.getString("visitorHash"),
                        rs.getInt("isDuplicate") == 1,
                        rs.getInt("testVisit") == 1))
                .list();
    }

    public boolean existsRecentVisitorHash(UUID emailUuid, String visitorHash, Instant since) {
        return jdbcClient.sql("""
                        SELECT EXISTS(
                            SELECT 1
                            FROM linkVisits
                            WHERE emailUUID = :emailUuid
                              AND visitorHash = :visitorHash
                              AND timeVisited >= :since
                        )
                        """)
                .param("emailUuid", emailUuid.toString())
                .param("visitorHash", visitorHash)
                .param("since", since.toEpochMilli())
                .query(Integer.class)
                .single() == 1;
    }

    public void deleteByEmailUuid(UUID emailUuid) {
        jdbcClient.sql("DELETE FROM linkVisits WHERE emailUUID = :emailUuid")
                .param("emailUuid", emailUuid.toString())
                .update();
    }
}
