package com.bunkerparty.di;

import static org.junit.jupiter.api.Assertions.*;

import com.bunkerparty.service.GameService;
import com.bunkerparty.service.RoomManager;
import com.bunkerparty.websocket.helpers.WebSocketJsonSender;
import com.google.inject.Guice;
import com.google.inject.Injector;
import java.util.Random;
import org.junit.jupiter.api.Test;

class ApplicationModuleTest {

  @Test
  void injector_resolves_all_bindings() {
    Injector injector = Guice.createInjector(new ApplicationModule());

    assertNotNull(injector.getInstance(Random.class));
    assertNotNull(injector.getInstance(RoomManager.class));
    assertNotNull(injector.getInstance(GameService.class));
    assertNotNull(injector.getInstance(WebSocketJsonSender.class));
  }

  @Test
  void random_is_bound_to_a_single_instance() {
    Injector injector = Guice.createInjector(new ApplicationModule());

    Random r1 = injector.getInstance(Random.class);
    Random r2 = injector.getInstance(Random.class);

    assertSame(
        r1, r2, "Random should be bound with toInstance(), so injector returns the same instance");
  }

  @Test
  void roomManager_is_singleton() {
    Injector injector = Guice.createInjector(new ApplicationModule());

    RoomManager a = injector.getInstance(RoomManager.class);
    RoomManager b = injector.getInstance(RoomManager.class);

    assertSame(a, b, "RoomManager should be a singleton");
  }

  @Test
  void gameService_is_singleton() {
    Injector injector = Guice.createInjector(new ApplicationModule());

    GameService a = injector.getInstance(GameService.class);
    GameService b = injector.getInstance(GameService.class);

    assertSame(a, b, "GameService should be a singleton");
  }

  @Test
  void webSocketJsonSender_is_singleton() {
    Injector injector = Guice.createInjector(new ApplicationModule());

    WebSocketJsonSender a = injector.getInstance(WebSocketJsonSender.class);
    WebSocketJsonSender b = injector.getInstance(WebSocketJsonSender.class);

    assertSame(a, b, "WebSocketJsonSender should be a singleton");
  }
}
