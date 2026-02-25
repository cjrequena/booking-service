package com.cjrequena.sample.command.handler.shared.common.util;

import java.util.Random;

public class BookingReferenceGenerator {

  // Method to generate a transfer booking reference
  public static String generateBookingReference(String productType) {
    // Ensure the city code is uppercase and limited to 3 characters
    productType = productType.toUpperCase().substring(0, Math.min(productType.length(), 3));

    // Generate a 4-digit random number
    int randomDigits = new Random().nextInt(9000) + 1000; // Ensures 4 digits (1000-9999)

    // Generate a random check character (A-Z)
    char checkCharacter = (char) ('A' + new Random().nextInt(26)); // Random letter from A-Z

    // Construct and return the booking reference
    return String.format("BKG-%s-%04d-%c",  productType, randomDigits, checkCharacter);
  }

}
