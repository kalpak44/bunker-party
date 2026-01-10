package com.bunkerparty;

import com.bunkerparty.di.ApplicationModule;
import com.google.inject.Guice;
import com.google.inject.Injector;

public class Main {
  /** Entry point of the application. */
  static void main() {
    Injector injector = Guice.createInjector(new ApplicationModule());
    injector.getInstance(SparkServer.class).start();
  }
}
