package com.bunkerparty.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class AppConfigTest {

  @Test
  void returnsDefaultWhenPortNotSet() {
    AppConfig config =
        new AppConfig() {
          @Override
          protected String getEnv(String key) {
            return null;
          }
        };

    assertEquals(8000, config.getPort());
  }

  @Test
  void returnsPortWhenValidEnvSet() {
    AppConfig config =
        new AppConfig() {
          @Override
          protected String getEnv(String key) {
            return "9090";
          }
        };

    assertEquals(9090, config.getPort());
  }

  @Test
  void returnsDefaultWhenEnvIsInvalid() {
    AppConfig config =
        new AppConfig() {
          @Override
          protected String getEnv(String key) {
            return "not-a-number";
          }
        };

    assertEquals(8000, config.getPort());
  }

  @Test
  void getEnvDelegatesToSystemEnvironment() {
    AppConfig config = new AppConfig();

    assertEquals(System.getenv("PORT"), config.getEnv("PORT"));
  }
}
