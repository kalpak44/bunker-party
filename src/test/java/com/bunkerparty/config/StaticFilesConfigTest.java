package com.bunkerparty.config;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.Test;

class StaticFilesConfigTest {

  @Test
  void testConfigure() {
    StaticFilesConfig config = new StaticFilesConfig();
    assertDoesNotThrow(config::configure);
  }
}
