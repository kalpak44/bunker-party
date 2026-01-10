package com.bunkerparty.routes;

import com.bunkerparty.service.GameService;
import com.google.gson.Gson;
import jakarta.inject.Inject;

import java.util.Map;

import static spark.Spark.get;

public class HealthRoutes {

    private final GameService gameService;
    private final Gson gson = new Gson();

    @Inject
    public HealthRoutes(GameService gameService) {
        this.gameService = gameService;
    }

    public void register() {

        get("/health", (req, res) -> {
            res.type("application/json");
            return gson.toJson(Map.of(
                    "status", "ok",
                    "rooms", gameService.getAllRooms().size()
            ));
        });
    }
}
