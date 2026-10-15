package com.relayops.api.config;

import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@EnableConfigurationProperties(RelayOpsProperties.class)
public class AppConfig {

  @Bean
  HttpClient httpClient(RelayOpsProperties properties) {
    return HttpClient.newBuilder()
        .connectTimeout(Duration.ofMillis(properties.getDelivery().getHttpTimeoutMs()))
        .build();
  }
}
