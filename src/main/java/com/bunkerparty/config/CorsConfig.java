package com.bunkerparty.config;

import static spark.Spark.before;
import static spark.Spark.options;

public class CorsConfig {

    public static void enable() {
        before((req, res) -> {
            res.header("Access-Control-Allow-Origin", "*");
            res.header("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            res.header("Access-Control-Allow-Headers", "Content-Type, Authorization");
        });

        options("/*", (req, res) -> "OK");
    }
}
