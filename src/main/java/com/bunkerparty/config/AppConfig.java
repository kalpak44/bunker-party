package com.bunkerparty.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AppConfig {
    private static final int DEFAULT_PORT = 8000;
    private static final Logger logger = LoggerFactory.getLogger(AppConfig.class);

    /**
     * Returns the port number to run the server on, from PORT env var or 8000 by default.
     */
    public static int getPort() {
        String portEnv = System.getenv("PORT");
        if (portEnv != null) {
            try {
                return Integer.parseInt(portEnv);
            } catch (NumberFormatException e) {
                logger.warn("Invalid PORT env var: {}", portEnv);
            }
        }
        return DEFAULT_PORT;
    }
}
