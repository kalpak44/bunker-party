package com.bunkerparty.config;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import spark.Filter;
import spark.Request;
import spark.Response;
import spark.Spark;

class CorsConfigTest {

  @Test
  void testEnable() {
    CorsConfig config = new CorsConfig();
    assertDoesNotThrow(config::enable);
  }

  @Test
  void enableRegistersCorsFilter() throws Exception {
    CorsConfig config = new CorsConfig();
    try (MockedStatic<Spark> spark = mockStatic(Spark.class)) {
      config.enable();

      ArgumentCaptor<Filter> filterCaptor = ArgumentCaptor.forClass(Filter.class);
      spark.verify(() -> Spark.before(filterCaptor.capture()));

      Request request = mock(Request.class);
      Response response = mock(Response.class);
      filterCaptor.getValue().handle(request, response);

      verify(response).header("Access-Control-Allow-Origin", "*");
      verify(response).header("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
      verify(response).header("Access-Control-Allow-Headers", "Content-Type, Authorization");
    }
  }
}
