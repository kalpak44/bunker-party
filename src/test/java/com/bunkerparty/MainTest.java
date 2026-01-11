package com.bunkerparty;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

class MainTest {

  @Test
  void main_createsInjectorGetsSparkServerAndStartsIt() {
    Injector injector = mock(Injector.class);
    SparkServer sparkServer = mock(SparkServer.class);

    when(injector.getInstance(SparkServer.class)).thenReturn(sparkServer);

    try (MockedStatic<Guice> guice = mockStatic(Guice.class)) {
      guice
          .when(() -> Guice.createInjector(any(com.google.inject.Module.class)))
          .thenReturn(injector);

      Main.main();

      guice.verify(() -> Guice.createInjector(any(com.google.inject.Module.class)));
      verify(injector).getInstance(SparkServer.class);
      verify(sparkServer).start();
      verifyNoMoreInteractions(injector, sparkServer);
    }
  }
}
