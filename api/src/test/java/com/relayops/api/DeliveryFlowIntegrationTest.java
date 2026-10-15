package com.relayops.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
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
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DeliveryFlowIntegrationTest {

  @LocalServerPort private int port;

  @Autowired private ObjectMapper objectMapper;
  @Autowired private HmacSigner hmacSigner;

  private final HttpClient http = HttpClient.newHttpClient();
  private HttpServer sink;
  private final AtomicInteger hits = new AtomicInteger();

  @DynamicPropertySource
  static void props(DynamicPropertyRegistry registry) {
    registry.add(
        "spring.datasource.url",
        () ->
            System.getenv().getOrDefault(
                "RELAYOPS_DB_URL", "jdbc:postgresql://127.0.0.1:5432/relayops"));
    registry.add(
        "spring.datasource.username",
        () ->
            System.getenv()
                .getOrDefault("RELAYOPS_DB_USER", System.getProperty("user.name")));
    registry.add(
        "spring.datasource.password",
        () -> System.getenv().getOrDefault("RELAYOPS_DB_PASSWORD", ""));
    registry.add("spring.data.redis.host", () -> "127.0.0.1");
    registry.add("spring.data.redis.port", () -> "6379");
    registry.add("relayops.bootstrap-api-key", () -> "test-bootstrap");
    registry.add("relayops.delivery.queue-key", () -> "relayops:test:delivery:happy");
    registry.add("relayops.delivery.max-attempts", () -> "5");
    registry.add("relayops.delivery.initial-backoff-ms", () -> "100");
    registry.add("relayops.delivery.max-backoff-ms", () -> "200");
    registry.add("relayops.delivery.http-timeout-ms", () -> "1000");
    registry.add("relayops.delivery.poll-interval-ms", () -> "100");
  }

  @BeforeEach
  void startSink() throws Exception {
    hits.set(0);
    sink = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    sink.createContext(
        "/hook",
        exchange -> {
          hits.incrementAndGet();
          byte[] body = "{\"ok\":true}".getBytes();
          exchange.sendResponseHeaders(200, body.length);
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
  void ingestIsIdempotentAndDeliversWithSignature() throws Exception {
    BootstrapResponse bootstrap = bootstrapClient("integration-client");
    String apiKey = bootstrap.apiKey();

    String secret = "destination-signing-secret";
    DestinationResponse destination =
        createDestination(
            apiKey,
            "Primary",
            "http://127.0.0.1:" + sink.getAddress().getPort() + "/hook",
            secret);

    JsonNode payload = objectMapper.valueToTree(Map.of("orderId", "A-1", "amount", 42));
    String payloadJson = objectMapper.writeValueAsString(payload);
    String signature = "sha256=" + hmacSigner.signHex(secret, payloadJson);

    EventResponse first =
        ingest(apiKey, destination.id().toString(), "idem-1", payloadJson, signature);
    EventResponse duplicate =
        ingest(apiKey, destination.id().toString(), "idem-1", payloadJson, signature);

    assertThat(first.id()).isEqualTo(duplicate.id());
    assertThat(first.status())
        .isIn(DeliveryStatus.QUEUED, DeliveryStatus.DELIVERING, DeliveryStatus.SUCCEEDED);

    await()
        .atMost(Duration.ofSeconds(8))
        .untilAsserted(
            () -> {
              EventResponse current = getEvent(apiKey, first.id().toString());
              assertThat(current.status()).isEqualTo(DeliveryStatus.SUCCEEDED);
              assertThat(current.attemptCount()).isGreaterThanOrEqualTo(1);
              assertThat(hits.get()).isGreaterThanOrEqualTo(1);
            });
  }

  @Test
  void rejectsInvalidSignature() throws Exception {
    BootstrapResponse bootstrap = bootstrapClient("bad-sig-client");
    DestinationResponse destination =
        createDestination(
            bootstrap.apiKey(),
            "Sink",
            "http://127.0.0.1:" + sink.getAddress().getPort() + "/hook",
            "destination-signing-secret");

    String body =
        """
        {"destinationId":"%s","idempotencyKey":"x","payload":{"a":1}}
        """
            .formatted(destination.id());
    HttpResponse<String> response =
        http.send(
            HttpRequest.newBuilder(uri("/v1/events"))
                .header("Authorization", "Bearer " + bootstrap.apiKey())
                .header("Content-Type", "application/json")
                .header("X-RelayOps-Signature", "sha256=deadbeef")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build(),
            HttpResponse.BodyHandlers.ofString());
    assertThat(response.statusCode()).isEqualTo(403);
  }

  private BootstrapResponse bootstrapClient(String name) throws Exception {
    String body =
        """
        {"name":"%s","bootstrapKey":"test-bootstrap"}
        """
            .formatted(name);
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
        """
        {"name":"%s","targetUrl":"%s","signingSecret":"%s"}
        """
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
        """
        {"destinationId":"%s","idempotencyKey":"%s","payload":%s}
        """
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

  private EventResponse getEvent(String apiKey, String eventId) throws Exception {
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

  private URI uri(String path) {
    return URI.create("http://127.0.0.1:" + port + path);
  }
}
