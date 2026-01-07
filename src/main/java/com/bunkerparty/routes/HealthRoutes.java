package com.bunkerparty.routes;

import com.bunkerparty.manager.RoomManager;
import com.google.gson.Gson;
import jakarta.inject.Inject;

import java.util.Map;

import static spark.Spark.*;

public class HealthRoutes {

    private final RoomManager roomManager;
    private final Gson gson = new Gson();

    @Inject
    public HealthRoutes(RoomManager roomManager) {
        this.roomManager = roomManager;
    }

    public void register() {

        get("/", (req, res) -> {
            res.redirect("/static/index.html");
            return null;
        });

        get("/health", (req, res) -> {
            res.type("application/json");
            return gson.toJson(Map.of(
                    "status", "ok",
                    "rooms", roomManager.getAllRooms().size()
            ));
        });
    }
}
