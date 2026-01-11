package com.bunkerparty.config;

import static spark.Spark.before;
import static spark.Spark.options;

import jakarta.inject.Singleton;
import spark.Filter;
import spark.Route;

@Singleton
public class CorsConfig {

  /** Enables CORS for all routes. */
  public void enable() {
    Filter filter =
        (req, res) -> {
          res.header("Access-Control-Allow-Origin", "*");
          res.header("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
          res.header("Access-Control-Allow-Headers", "Content-Type, Authorization");
        };
    Route route = (req, res) -> "OK";

    before(filter);
    options("/*", route);
  }
}
