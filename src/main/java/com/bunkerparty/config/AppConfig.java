package com.bunkerparty.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AppConfig {

    private static final Logger logger = LoggerFactory.getLogger(AppConfig.class);

    public static int getPort() {
        String portEnv = System.getenv("PORT");
        if (portEnv != null) {
            try {
                return Integer.parseInt(portEnv);
            } catch (NumberFormatException e) {
                logger.warn("Invalid PORT env var: {}", portEnv);
            }
        }
        return 8000;
    }
}
