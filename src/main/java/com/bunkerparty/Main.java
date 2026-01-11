package com.bunkerparty;

import com.bunkerparty.di.ApplicationModule;
import com.google.inject.Guice;
import com.google.inject.Injector;

public class Main {
  private Main() {
    // No instance needed
  }

  /** Entry point of the application. */
  public static void main() {
    Injector injector = Guice.createInjector(new ApplicationModule());
    injector.getInstance(SparkServer.class).start();
  }
}
