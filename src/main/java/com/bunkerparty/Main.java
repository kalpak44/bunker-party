package com.bunkerparty;

import com.bunkerparty.config.AppConfig;
import com.bunkerparty.config.CorsConfig;
import com.bunkerparty.config.StaticFilesConfig;
import com.bunkerparty.di.ApplicationModule;
import com.bunkerparty.routes.HealthRoutes;
import com.bunkerparty.websocket.GameWebSocketHandler;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static spark.Spark.init;
import static spark.Spark.port;
import static spark.Spark.webSocket;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    public static void main(String[] args) {
        Injector injector = Guice.createInjector(new ApplicationModule());
        int port = AppConfig.getPort();
        port(port);
        webSocket("/ws", injector.getInstance(GameWebSocketHandler.class));
        StaticFilesConfig.configure();
        CorsConfig.enable();
        injector.getInstance(HealthRoutes.class).register();
        init();
        logger.info("Application started on port {}", port);
    }
}
