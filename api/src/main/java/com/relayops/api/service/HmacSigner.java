package com.relayops.api.service;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

@Component
public class HmacSigner {

  public String signHex(String secret, String body) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
      byte[] digest = mac.doFinal(body.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(digest);
    } catch (NoSuchAlgorithmException | InvalidKeyException ex) {
      throw new IllegalStateException("Unable to sign payload", ex);
    }
  }

  public boolean matches(String secret, String body, String providedSignature) {
    if (providedSignature == null || providedSignature.isBlank()) {
      return false;
    }
    String expected = signHex(secret, body);
    String normalized =
        providedSignature.startsWith("sha256=")
            ? providedSignature.substring("sha256=".length())
            : providedSignature;
    return MessageDigestEquals.constantTimeEquals(expected, normalized);
  }

  private static final class MessageDigestEquals {
    private MessageDigestEquals() {}

    static boolean constantTimeEquals(String a, String b) {
      if (a.length() != b.length()) {
        return false;
      }
      int result = 0;
      for (int i = 0; i < a.length(); i++) {
        result |= a.charAt(i) ^ b.charAt(i);
      }
      return result == 0;
    }
  }
}
