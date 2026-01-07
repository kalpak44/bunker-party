package com.bunkerparty.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

import static spark.Spark.staticFiles;

public class StaticFilesConfig {

    private static final Logger logger = LoggerFactory.getLogger(StaticFilesConfig.class);

    public static void configure() {
        var staticDir = System.getProperty("user.dir") + "/static";
        var staticFolder = new File(staticDir);

        if (staticFolder.exists()) {
            logger.info("Serving static files from {}", staticDir);
            staticFiles.externalLocation(staticDir);
        } else {
            staticFiles.location("/static");
        }
    }
}
