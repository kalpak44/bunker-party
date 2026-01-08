package com.bunkerparty.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static spark.Spark.staticFiles;

public final class StaticFilesConfig {

    private static final Logger logger = LoggerFactory.getLogger(StaticFilesConfig.class);

    private StaticFilesConfig() {
    }

    public static void configure() {
        staticFiles.location("/public");
        logger.info("Serving static files from classpath: /public");
    }
}
