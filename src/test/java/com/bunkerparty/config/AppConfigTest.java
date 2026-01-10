package com.bunkerparty.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class AppConfigTest {

  @Test
  void testGetPort_Default() {
    AppConfig config = new AppConfig();
    String portEnv = System.getenv("PORT");
    int expected = (portEnv != null) ? Integer.parseInt(portEnv) : 8000;
    assertEquals(expected, config.getPort());
  }
}
