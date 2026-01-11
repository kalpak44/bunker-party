package com.bunkerparty.routes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static spark.Spark.get;

import com.bunkerparty.domain.Room;
import com.bunkerparty.service.GameService;
import com.google.gson.Gson;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import spark.Request;
import spark.Response;
import spark.Route;

class HealthRoutesTest {

  @Test
  void register_registersHealthEndpointAndReturnsExpectedPayload() throws Exception {
    GameService gameService = mock(GameService.class);
    when(gameService.getAllRooms())
        .thenReturn(List.of(mock(Room.class), mock(Room.class), mock(Room.class)));

    HealthRoutes routes = new HealthRoutes(gameService);

    try (MockedStatic<spark.Spark> spark = mockStatic(spark.Spark.class)) {
      ArgumentCaptor<Route> routeCaptor = ArgumentCaptor.forClass(Route.class);

      routes.register();

      spark.verify(() -> get(eq("/health"), routeCaptor.capture()));

      Route route = routeCaptor.getValue();
      Request request = mock(Request.class);
      Response response = mock(Response.class);

      Object result = route.handle(request, response);

      verify(response).type("application/json");

      Map<?, ?> json = new Gson().fromJson(result.toString(), Map.class);

      assertEquals("ok", json.get("status"));
      assertEquals(3.0, json.get("rooms"));

      verify(gameService).getAllRooms();
      verifyNoMoreInteractions(gameService);
    }
  }
}
