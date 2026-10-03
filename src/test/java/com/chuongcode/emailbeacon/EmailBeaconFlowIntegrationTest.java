package com.chuongcode.emailbeacon;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.datasource.url=jdbc:sqlite:${java.io.tmpdir}/email-beacon-${random.uuid}.db?busy_timeout=5000&foreign_keys=on"
        }
)
class EmailBeaconFlowIntegrationTest {

    private static final Pattern UUID_PATTERN = Pattern.compile(
            "[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}",
            Pattern.CASE_INSENSITIVE);

    @LocalServerPort
    private int port;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Test
    void generatesTracksAndReportsAVisitThroughLegacyEndpoints() throws Exception {
        HttpResponse<String> generated = get("/generateUUID", HttpResponse.BodyHandlers.ofString());

        assertThat(generated.statusCode()).isEqualTo(200);
        Matcher matcher = UUID_PATTERN.matcher(generated.body());
        assertThat(matcher.find()).isTrue();
        UUID uuid = UUID.fromString(matcher.group());

        HttpResponse<String> unread = get(
                "/emailBeaconStatus?UUID=" + uuid,
                HttpResponse.BodyHandlers.ofString());
        assertThat(unread.statusCode()).isEqualTo(200);
        assertThat(unread.body()).isEqualTo("Unread...");

        HttpResponse<byte[]> pixel = get(
                "/emailBeacon?UUID=" + uuid,
                HttpResponse.BodyHandlers.ofByteArray());
        assertThat(pixel.statusCode()).isEqualTo(200);
        assertThat(pixel.headers().firstValue("content-type")).hasValue("image/gif");
        assertThat(pixel.body()).isNotEmpty();

        HttpResponse<String> opened = get(
                "/emailBeaconStatus?UUID=" + uuid,
                HttpResponse.BodyHandlers.ofString());
        assertThat(opened.statusCode()).isEqualTo(200);
        assertThat(opened.body())
                .contains(uuid.toString())
                .contains("loggedIPAddress")
                .contains("timeVisited");
    }

    @Test
    void createsAndListsBeaconsThroughTheConsolidatedApi() throws Exception {
        HttpRequest request = HttpRequest.newBuilder(baseUri("/api/beacons"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"name\":\"Launch note\"}", StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> created = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(created.statusCode()).isEqualTo(201);
        assertThat(created.body())
                .contains("Launch note")
                .contains("pixelUrl")
                .contains("statusUrl");

        Matcher idMatcher = UUID_PATTERN.matcher(created.body());
        assertThat(idMatcher.find()).isTrue();
        String id = idMatcher.group();

        HttpResponse<String> listed = get("/api/beacons", HttpResponse.BodyHandlers.ofString());
        assertThat(listed.statusCode()).isEqualTo(200);
        assertThat(listed.body()).contains("Launch note");

        HttpRequest renameRequest = HttpRequest.newBuilder(baseUri("/api/beacons/" + id))
                .header("Content-Type", "application/json")
                .method("PATCH", HttpRequest.BodyPublishers.ofString("{\"name\":\"Renamed beacon\"}"))
                .build();
        HttpResponse<String> renamed = httpClient.send(renameRequest, HttpResponse.BodyHandlers.ofString());
        assertThat(renamed.statusCode()).isEqualTo(200);
        assertThat(renamed.body()).contains("Renamed beacon");

        get("/emailBeacon?UUID=" + id, HttpResponse.BodyHandlers.discarding());
        HttpResponse<String> visits = get(
                "/api/beacons/" + id + "/visits",
                HttpResponse.BodyHandlers.ofString());
        assertThat(visits.statusCode()).isEqualTo(200);
        assertThat(visits.body()).contains("visitedAt").contains("ipAddress");

        HttpRequest deleteRequest = HttpRequest.newBuilder(baseUri("/api/beacons/" + id))
                .DELETE()
                .build();
        HttpResponse<Void> deleted = httpClient.send(deleteRequest, HttpResponse.BodyHandlers.discarding());
        assertThat(deleted.statusCode()).isEqualTo(204);

        HttpResponse<String> missing = get(
                "/api/beacons/" + id,
                HttpResponse.BodyHandlers.ofString());
        assertThat(missing.statusCode()).isEqualTo(404);
    }

    @Test
    void doesNotTrustForwardedAddressesByDefault() throws Exception {
        HttpResponse<String> generated = get("/generateUUID", HttpResponse.BodyHandlers.ofString());
        Matcher matcher = UUID_PATTERN.matcher(generated.body());
        assertThat(matcher.find()).isTrue();

        HttpRequest pixelRequest = HttpRequest.newBuilder(baseUri("/emailBeacon?UUID=" + matcher.group()))
                .header("X-Forwarded-For", "203.0.113.10")
                .GET()
                .build();
        httpClient.send(pixelRequest, HttpResponse.BodyHandlers.discarding());

        HttpResponse<String> status = get(
                "/emailBeaconStatus?UUID=" + matcher.group(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(status.body()).doesNotContain("203.0.113.10");
    }

    @Test
    void classifiesLoadsAndDeduplicatesEstimatedOpens() throws Exception {
        String id = createBeacon("Accuracy check");

        hitPixel(id, "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36");
        hitPixel(id, "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36");
        hitPixel(id, "Mozilla/5.0 GoogleImageProxy");
        hitPixel(id, "curl/8.7.1");

        HttpRequest testVisitRequest = HttpRequest.newBuilder(baseUri("/api/beacons/" + id + "/test-visit"))
                .header("User-Agent", "Mozilla/5.0")
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        HttpResponse<String> testVisit = httpClient.send(testVisitRequest, HttpResponse.BodyHandlers.ofString());

        assertThat(testVisit.statusCode()).isEqualTo(200);
        assertThat(testVisit.body())
                .contains("\"visitCount\":5")
                .contains("\"estimatedUniqueOpens\":2")
                .contains("\"likelyHumanLoadCount\":2")
                .contains("\"mailProxyLoadCount\":1")
                .contains("\"automatedLoadCount\":1")
                .contains("\"duplicateLoadCount\":1")
                .contains("\"testLoadCount\":1")
                .contains("\"firstOpenedAt\":")
                .contains("\"lastOpenedAt\":");

        HttpResponse<String> visits = get(
                "/api/beacons/" + id + "/visits",
                HttpResponse.BodyHandlers.ofString());
        assertThat(visits.body())
                .contains("\"classification\":\"HUMAN_LIKELY\"")
                .contains("\"classification\":\"MAIL_PROXY\"")
                .contains("\"classification\":\"AUTOMATED\"")
                .contains("\"classification\":\"TEST\"")
                .contains("\"duplicate\":true")
                .contains("\"userAgent\":");
    }

    private String createBeacon(String name) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(baseUri("/api/beacons"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"name\":\"" + name + "\"}"))
                .build();
        HttpResponse<String> created = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        Matcher matcher = UUID_PATTERN.matcher(created.body());
        assertThat(created.statusCode()).isEqualTo(201);
        assertThat(matcher.find()).isTrue();
        return matcher.group();
    }

    private void hitPixel(String id, String userAgent) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(baseUri("/emailBeacon?UUID=" + id))
                .header("User-Agent", userAgent)
                .GET()
                .build();
        HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
        assertThat(response.statusCode()).isEqualTo(200);
    }

    private <T> HttpResponse<T> get(String path, HttpResponse.BodyHandler<T> bodyHandler) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(baseUri(path)).GET().build();
        return httpClient.send(request, bodyHandler);
    }

    private URI baseUri(String path) {
        return URI.create("http://localhost:" + port + path);
    }
}
