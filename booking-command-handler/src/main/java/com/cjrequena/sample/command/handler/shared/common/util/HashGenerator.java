package com.cjrequena.sample.command.handler.shared.common.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class HashGenerator {
  public static String generateHashSha256(String input) {
    try {
      // Create a MessageDigest instance for SHA-256
      MessageDigest digest = MessageDigest.getInstance("SHA-256");

      // Compute the hash as a byte array
      byte[] hashBytes = digest.digest(input.getBytes());

      // Convert byte array into a hexadecimal string
      StringBuilder hexString = new StringBuilder();
      for (byte b : hashBytes) {
        String hex = Integer.toHexString(0xff & b);
        if (hex.length() == 1) {
          hexString.append('0');
        }
        hexString.append(hex);
      }
      return hexString.toString();
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException("Error generating hash", e);
    }
  }
}
