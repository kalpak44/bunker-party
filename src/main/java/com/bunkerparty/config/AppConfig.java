package com.bunkerparty.config;

import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class AppConfig {
  private static final int DEFAULT_PORT = 8000;
  private static final Logger logger = LoggerFactory.getLogger(AppConfig.class);

  /** Returns the port number to run the server on, from PORT env var or 8000 by default. */
  public int getPort() {
    String portEnv = getEnv("PORT");
    if (portEnv != null) {
      try {
        return Integer.parseInt(portEnv);
      } catch (NumberFormatException e) {
        logger.warn("Invalid PORT env var: {}", portEnv, e);
      }
    }
    return DEFAULT_PORT;
  }

  protected String getEnv(String key) {
    return System.getenv(key);
  }
}
