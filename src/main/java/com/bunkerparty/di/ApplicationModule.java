package com.bunkerparty.di;


import com.bunkerparty.service.GameService;
import com.bunkerparty.service.RoomManager;
import com.bunkerparty.websocket.helpers.WebSocketJsonSender;
import com.google.inject.AbstractModule;
import com.google.inject.Scopes;

public class ApplicationModule extends AbstractModule {

    /**
     * Configures dependency injection bindings.
     */
    @Override
    protected void configure() {
        bind(RoomManager.class).in(Scopes.SINGLETON);
        bind(GameService.class).in(Scopes.SINGLETON);
        bind(WebSocketJsonSender.class).in(Scopes.SINGLETON);
    }
}
