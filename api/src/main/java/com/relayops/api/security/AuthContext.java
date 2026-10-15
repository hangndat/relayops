package com.relayops.api.security;

import com.relayops.api.domain.ApiClient;

public final class AuthContext {
  private static final ThreadLocal<ApiClient> CURRENT = new ThreadLocal<>();

  private AuthContext() {}

  public static void set(ApiClient client) {
    CURRENT.set(client);
  }

  public static ApiClient require() {
    ApiClient client = CURRENT.get();
    if (client == null) {
      throw new IllegalStateException("Unauthenticated request");
    }
    return client;
  }

  public static void clear() {
    CURRENT.remove();
  }
}
