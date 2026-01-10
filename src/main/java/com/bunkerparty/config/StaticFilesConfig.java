package com.bunkerparty.config;

import static spark.Spark.staticFiles;

import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public final class StaticFilesConfig {

  private static final Logger logger = LoggerFactory.getLogger(StaticFilesConfig.class);

  /** Configures Spark to serve static files from the /public classpath directory. */
  public void configure() {
    staticFiles.location("/public");
    logger.info("Serving static files from classpath: /public");
  }
}
