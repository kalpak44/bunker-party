package com.bunkerparty;

import static spark.Spark.*;

import com.bunkerparty.config.AppConfig;
import com.bunkerparty.config.CorsConfig;
import com.bunkerparty.config.StaticFilesConfig;
import com.bunkerparty.routes.HealthRoutes;
import com.bunkerparty.websocket.GameWebSocketHandler;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class SparkServer {
  private static final Logger logger = LoggerFactory.getLogger(SparkServer.class);

  private final GameWebSocketHandler webSocketHandler;
  private final HealthRoutes healthRoutes;
  private final AppConfig appConfig;
  private final CorsConfig corsConfig;
  private final StaticFilesConfig staticFilesConfig;

  /** Creates a new Spark server with injected dependencies. */
  @Inject
  public SparkServer(
      GameWebSocketHandler webSocketHandler,
      HealthRoutes healthRoutes,
      AppConfig appConfig,
      CorsConfig corsConfig,
      StaticFilesConfig staticFilesConfig) {
    this.webSocketHandler = webSocketHandler;
    this.healthRoutes = healthRoutes;
    this.appConfig = appConfig;
    this.corsConfig = corsConfig;
    this.staticFilesConfig = staticFilesConfig;
  }

  /** Starts the Spark server. */
  public void start() {
    int port = appConfig.getPort();
    port(port);

    webSocket("/ws", webSocketHandler);

    staticFilesConfig.configure();
    corsConfig.enable();

    healthRoutes.register();

    init();
    logger.info("Application started on port {}", port);
  }
}
