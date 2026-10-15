package com.relayops.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.relayops.api.config.RelayOpsProperties;
import com.relayops.api.domain.DeliveryStatus;
import com.relayops.api.service.HmacSigner;
import com.relayops.api.web.dto.BootstrapResponse;
import com.relayops.api.web.dto.DestinationResponse;
import com.relayops.api.web.dto.EventResponse;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class FailureReplayIntegrationTest {

  @LocalServerPort private int port;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private HmacSigner hmacSigner;
  @Autowired private RelayOpsProperties properties;

  private final HttpClient http = HttpClient.newHttpClient();
  private HttpServer sink;
  private final AtomicInteger hits = new AtomicInteger();
  private volatile int statusToReturn = 500;

  @DynamicPropertySource
  static void props(DynamicPropertyRegistry registry) {
    registry.add(
        "spring.datasource.url",
        () ->
            System.getenv()
                .getOrDefault("RELAYOPS_DB_URL", "jdbc:postgresql://127.0.0.1:5432/relayops"));
    registry.add(
        "spring.datasource.username",
        () ->
            System.getenv()
                .getOrDefault("RELAYOPS_DB_USER", System.getProperty("user.name")));
    registry.add(
        "spring.datasource.password",
        () -> System.getenv().getOrDefault("RELAYOPS_DB_PASSWORD", ""));
    registry.add("spring.data.redis.host", () -> "127.0.0.1");
    registry.add("relayops.bootstrap-api-key", () -> "test-bootstrap");
    registry.add("relayops.delivery.queue-key", () -> "relayops:test:delivery:replay");
    registry.add("relayops.delivery.max-attempts", () -> "2");
    registry.add("relayops.delivery.initial-backoff-ms", () -> "50");
    registry.add("relayops.delivery.max-backoff-ms", () -> "100");
    registry.add("relayops.delivery.poll-interval-ms", () -> "50");
  }

  @BeforeEach
  void startSink() throws Exception {
    hits.set(0);
    statusToReturn = 500;
    sink = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    sink.createContext(
        "/hook",
        exchange -> {
          hits.incrementAndGet();
          byte[] body = ("{\"status\":" + statusToReturn + "}").getBytes();
          exchange.sendResponseHeaders(statusToReturn, body.length);
          exchange.getResponseBody().write(body);
          exchange.close();
        });
    sink.start();
  }

  @AfterEach
  void stopSink() {
    if (sink != null) {
      sink.stop(0);
    }
  }

  @Test
  void terminalFailureAppearsInDlqAndReplaySucceeds() throws Exception {
    assertThat(properties.getDelivery().getMaxAttempts()).isEqualTo(2);

    BootstrapResponse bootstrap = bootstrap("replay-client");
    String secret = "destination-signing-secret";
    DestinationResponse destination =
        createDestination(
            bootstrap.apiKey(),
            "Flaky",
            "http://127.0.0.1:" + sink.getAddress().getPort() + "/hook",
            secret);

    String payloadJson = "{\"orderId\":\"fail-1\"}";
    String signature = "sha256=" + hmacSigner.signHex(secret, payloadJson);
    EventResponse created =
        ingest(bootstrap.apiKey(), destination.id().toString(), "fail-idem", payloadJson, signature);

    await()
        .atMost(Duration.ofSeconds(8))
        .untilAsserted(
            () -> {
              EventResponse current = get(bootstrap.apiKey(), created.id().toString());
              assertThat(current.status()).isEqualTo(DeliveryStatus.FAILED_TERMINAL);
              assertThat(current.attemptCount()).isEqualTo(2);
            });

    EventResponse[] dlq = listDlq(bootstrap.apiKey());
    assertThat(dlq).extracting(EventResponse::id).contains(created.id());

    statusToReturn = 200;
    EventResponse replayed = replay(bootstrap.apiKey(), created.id().toString());
    assertThat(replayed.status()).isIn(DeliveryStatus.QUEUED, DeliveryStatus.DELIVERING);

    await()
        .atMost(Duration.ofSeconds(8))
        .untilAsserted(
            () -> {
              EventResponse current = get(bootstrap.apiKey(), created.id().toString());
              assertThat(current.status()).isEqualTo(DeliveryStatus.SUCCEEDED);
            });
  }

  private BootstrapResponse bootstrap(String name) throws Exception {
    String body = "{\"name\":\"%s\",\"bootstrapKey\":\"test-bootstrap\"}".formatted(name);
    HttpResponse<String> response =
        http.send(
            HttpRequest.newBuilder(uri("/v1/bootstrap"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build(),
            HttpResponse.BodyHandlers.ofString());
    assertThat(response.statusCode()).isEqualTo(200);
    return objectMapper.readValue(response.body(), BootstrapResponse.class);
  }

  private DestinationResponse createDestination(
      String apiKey, String name, String url, String secret) throws Exception {
    String body =
        "{\"name\":\"%s\",\"targetUrl\":\"%s\",\"signingSecret\":\"%s\"}"
            .formatted(name, url, secret);
    HttpResponse<String> response =
        http.send(
            HttpRequest.newBuilder(uri("/v1/destinations"))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build(),
            HttpResponse.BodyHandlers.ofString());
    assertThat(response.statusCode()).isEqualTo(200);
    return objectMapper.readValue(response.body(), DestinationResponse.class);
  }

  private EventResponse ingest(
      String apiKey, String destinationId, String idem, String payloadJson, String signature)
      throws Exception {
    String body =
        "{\"destinationId\":\"%s\",\"idempotencyKey\":\"%s\",\"payload\":%s}"
            .formatted(destinationId, idem, payloadJson);
    HttpResponse<String> response =
        http.send(
            HttpRequest.newBuilder(uri("/v1/events"))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .header("X-RelayOps-Signature", signature)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build(),
            HttpResponse.BodyHandlers.ofString());
    assertThat(response.statusCode()).isEqualTo(200);
    return objectMapper.readValue(response.body(), EventResponse.class);
  }

  private EventResponse get(String apiKey, String eventId) throws Exception {
    HttpResponse<String> response =
        http.send(
            HttpRequest.newBuilder(uri("/v1/events/" + eventId))
                .header("Authorization", "Bearer " + apiKey)
                .GET()
                .build(),
            HttpResponse.BodyHandlers.ofString());
    assertThat(response.statusCode()).isEqualTo(200);
    return objectMapper.readValue(response.body(), EventResponse.class);
  }

  private EventResponse[] listDlq(String apiKey) throws Exception {
    HttpResponse<String> response =
        http.send(
            HttpRequest.newBuilder(uri("/v1/dlq"))
                .header("Authorization", "Bearer " + apiKey)
                .GET()
                .build(),
            HttpResponse.BodyHandlers.ofString());
    assertThat(response.statusCode()).isEqualTo(200);
    return objectMapper.readValue(response.body(), EventResponse[].class);
  }

  private EventResponse replay(String apiKey, String eventId) throws Exception {
    HttpResponse<String> response =
        http.send(
            HttpRequest.newBuilder(uri("/v1/events/" + eventId + "/replay"))
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.noBody())
                .build(),
            HttpResponse.BodyHandlers.ofString());
    assertThat(response.statusCode()).isEqualTo(200);
    return objectMapper.readValue(response.body(), EventResponse.class);
  }

  private URI uri(String path) {
    return URI.create("http://127.0.0.1:" + port + path);
  }
}
