package com.bunkerparty.config;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.Test;

class CorsConfigTest {

  @Test
  void testEnable() {
    CorsConfig config = new CorsConfig();
    assertDoesNotThrow(config::enable);
  }
}
