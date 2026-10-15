package com.relayops.api.security;

import com.relayops.api.domain.ApiClient;
import com.relayops.api.repo.ApiClientRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class ApiKeyAuthFilter extends OncePerRequestFilter {

  private static final Set<String> PUBLIC_PREFIXES =
      Set.of("/actuator/health", "/actuator/info", "/v1/bootstrap");

  private final ApiClientRepository apiClientRepository;
  private final ApiKeyHasher apiKeyHasher;

  public ApiKeyAuthFilter(ApiClientRepository apiClientRepository, ApiKeyHasher apiKeyHasher) {
    this.apiClientRepository = apiClientRepository;
    this.apiKeyHasher = apiKeyHasher;
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    String path = request.getRequestURI();
    return PUBLIC_PREFIXES.stream().anyMatch(path::startsWith);
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    try {
      String apiKey = extractApiKey(request);
      if (apiKey == null || apiKey.isBlank()) {
        unauthorized(response, "Missing API key");
        return;
      }
      String hash = apiKeyHasher.hash(apiKey.trim());
      ApiClient client = apiClientRepository.findByApiKeyHash(hash).orElse(null);
      if (client == null) {
        unauthorized(response, "Invalid API key");
        return;
      }
      AuthContext.set(client);
      filterChain.doFilter(request, response);
    } finally {
      AuthContext.clear();
    }
  }

  private static String extractApiKey(HttpServletRequest request) {
    String authorization = request.getHeader("Authorization");
    if (authorization != null && authorization.regionMatches(true, 0, "Bearer ", 0, 7)) {
      return authorization.substring(7).trim();
    }
    return request.getHeader("X-API-Key");
  }

  private static void unauthorized(HttpServletResponse response, String message) throws IOException {
    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.getOutputStream()
        .write(("{\"error\":\"" + message + "\"}").getBytes(StandardCharsets.UTF_8));
  }
}
