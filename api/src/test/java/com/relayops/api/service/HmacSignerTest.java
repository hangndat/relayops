package com.relayops.api.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class HmacSignerTest {

  private final HmacSigner signer = new HmacSigner();

  @Test
  void matchesAcceptsPrefixedAndRawSignatures() {
    String body = "{\"hello\":\"world\"}";
    String secret = "super-secret-value";
    String hex = signer.signHex(secret, body);
    assertTrue(signer.matches(secret, body, hex));
    assertTrue(signer.matches(secret, body, "sha256=" + hex));
    assertFalse(signer.matches(secret, body, "deadbeef"));
  }
}
