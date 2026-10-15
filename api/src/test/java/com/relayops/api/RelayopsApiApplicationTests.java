package com.relayops.api;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
class RelayopsApiApplicationTests {

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
    registry.add("relayops.worker.enabled", () -> "false");
  }

  @Test
  void contextLoads() {}
}
