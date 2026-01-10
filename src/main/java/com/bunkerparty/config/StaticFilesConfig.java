package com.bunkerparty.config;

import static spark.Spark.staticFiles;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class StaticFilesConfig {

  private static final Logger logger = LoggerFactory.getLogger(StaticFilesConfig.class);

  private StaticFilesConfig() {
    // No instance required
  }

  /** Configures Spark to serve static files from the /public classpath directory. */
  public static void configure() {
    staticFiles.location("/public");
    logger.info("Serving static files from classpath: /public");
  }
}
