package com.bunkerparty.config;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import spark.Filter;
import spark.Request;
import spark.Response;
import spark.Route;
import spark.Spark;

class CorsConfigTest {

  @Test
  void testEnable() {
    CorsConfig config = new CorsConfig();
    assertDoesNotThrow(config::enable);
  }

  @Test
  void enableRegistersCorsFilterAndOptionsRoute() throws Exception {
    try (MockedStatic<Spark> spark = mockStatic(Spark.class)) {
      CorsConfig config = new CorsConfig();
      config.enable();

      ArgumentCaptor<Filter> filterCaptor = ArgumentCaptor.forClass(Filter.class);
      spark.verify(() -> Spark.before(filterCaptor.capture()));
      ArgumentCaptor<Route> routeCaptor = ArgumentCaptor.forClass(Route.class);
      spark.verify(() -> Spark.options(eq("/*"), routeCaptor.capture()));

      Request request = mock(Request.class);
      Response response = mock(Response.class);
      filterCaptor.getValue().handle(request, response);
      verify(response).header("Access-Control-Allow-Origin", "*");
      verify(response).header("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
      verify(response).header("Access-Control-Allow-Headers", "Content-Type, Authorization");

      assertEquals("OK", routeCaptor.getValue().handle(request, response));
    }
  }
}
