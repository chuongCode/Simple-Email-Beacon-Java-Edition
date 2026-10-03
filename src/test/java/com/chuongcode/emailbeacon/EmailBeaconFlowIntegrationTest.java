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

        HttpResponse<String> listed = get("/api/beacons", HttpResponse.BodyHandlers.ofString());
        assertThat(listed.statusCode()).isEqualTo(200);
        assertThat(listed.body()).contains("Launch note");
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

    private <T> HttpResponse<T> get(String path, HttpResponse.BodyHandler<T> bodyHandler) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(baseUri(path)).GET().build();
        return httpClient.send(request, bodyHandler);
    }

    private URI baseUri(String path) {
        return URI.create("http://localhost:" + port + path);
    }
}
