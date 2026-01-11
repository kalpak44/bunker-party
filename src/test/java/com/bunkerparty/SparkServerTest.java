package com.bunkerparty;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.bunkerparty.config.AppConfig;
import com.bunkerparty.config.CorsConfig;
import com.bunkerparty.config.StaticFilesConfig;
import com.bunkerparty.routes.HealthRoutes;
import com.bunkerparty.websocket.GameWebSocketHandler;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.MockedStatic;
import spark.Spark;

class SparkServerTest {

  @Test
  void start_configuresSparkAndRegistersRoutes() {
    GameWebSocketHandler webSocketHandler = mock(GameWebSocketHandler.class);
    HealthRoutes healthRoutes = mock(HealthRoutes.class);
    AppConfig appConfig = mock(AppConfig.class);
    CorsConfig corsConfig = mock(CorsConfig.class);
    StaticFilesConfig staticFilesConfig = mock(StaticFilesConfig.class);

    when(appConfig.getPort()).thenReturn(4567);

    SparkServer server =
        new SparkServer(webSocketHandler, healthRoutes, appConfig, corsConfig, staticFilesConfig);

    try (MockedStatic<Spark> spark = mockStatic(Spark.class)) {
      server.start();
      verify(appConfig).getPort();
      spark.verify(() -> Spark.port(4567));
      spark.verify(() -> Spark.webSocket("/ws", webSocketHandler));
      spark.verify(Spark::init);
      verify(staticFilesConfig).configure();
      verify(corsConfig).enable();
      verify(healthRoutes).register();
      verifyNoMoreInteractions(appConfig, staticFilesConfig, corsConfig, healthRoutes);
    }
  }

  @Test
  void start_callsStepsInExpectedOrder() {
    GameWebSocketHandler webSocketHandler = mock(GameWebSocketHandler.class);
    HealthRoutes healthRoutes = mock(HealthRoutes.class);
    AppConfig appConfig = mock(AppConfig.class);
    CorsConfig corsConfig = mock(CorsConfig.class);
    StaticFilesConfig staticFilesConfig = mock(StaticFilesConfig.class);

    when(appConfig.getPort()).thenReturn(9999);
    SparkServer server =
        new SparkServer(webSocketHandler, healthRoutes, appConfig, corsConfig, staticFilesConfig);

    try (MockedStatic<Spark> spark = mockStatic(Spark.class)) {
      server.start();
      InOrder inOrder = inOrder(appConfig, staticFilesConfig, corsConfig, healthRoutes);
      inOrder.verify(appConfig).getPort();
      inOrder.verify(staticFilesConfig).configure();
      inOrder.verify(corsConfig).enable();
      inOrder.verify(healthRoutes).register();
      spark.verify(() -> Spark.port(9999));
      spark.verify(() -> Spark.webSocket("/ws", webSocketHandler));
      spark.verify(Spark::init);
    }
  }
}
