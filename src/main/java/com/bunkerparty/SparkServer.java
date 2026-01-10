package com.bunkerparty;

import com.bunkerparty.config.AppConfig;
import com.bunkerparty.config.CorsConfig;
import com.bunkerparty.config.StaticFilesConfig;
import com.bunkerparty.routes.HealthRoutes;
import com.bunkerparty.websocket.GameWebSocketHandler;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static spark.Spark.*;

@Singleton
public class SparkServer {
    private static final Logger logger = LoggerFactory.getLogger(SparkServer.class);

    private final GameWebSocketHandler webSocketHandler;
    private final HealthRoutes healthRoutes;

    @Inject
    public SparkServer(GameWebSocketHandler webSocketHandler, HealthRoutes healthRoutes) {
        this.webSocketHandler = webSocketHandler;
        this.healthRoutes = healthRoutes;
    }

    public void start() {
        int port = AppConfig.getPort();
        port(port);

        webSocket("/ws", webSocketHandler);

        StaticFilesConfig.configure();
        CorsConfig.enable();

        healthRoutes.register();

        init();
        logger.info("Application started on port {}", port);
    }
}
