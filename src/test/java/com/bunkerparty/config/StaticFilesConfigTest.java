package com.bunkerparty.config;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.io.IOException;
import java.net.ServerSocket;
import org.junit.jupiter.api.Test;
import spark.Spark;

class StaticFilesConfigTest {

  @Test
  void testConfigure() {
    StaticFilesConfig config = new StaticFilesConfig();
    assertDoesNotThrow(config::configure);
  }

  @Test
  void testConfigureSkipsWhenStaticFilesAlreadyMapped() throws IOException {
    Spark.port(findFreePort());
    Spark.get("/dummy", (req, res) -> "ok");
    Spark.awaitInitialization();
    try {
      StaticFilesConfig config = new StaticFilesConfig();
      assertDoesNotThrow(config::configure);
    } finally {
      Spark.stop();
      Spark.awaitStop();
    }
  }

  private static int findFreePort() throws IOException {
    try (ServerSocket socket = new ServerSocket(0)) {
      return socket.getLocalPort();
    }
  }
}
