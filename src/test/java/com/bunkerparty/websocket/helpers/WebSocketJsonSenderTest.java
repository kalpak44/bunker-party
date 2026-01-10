package com.bunkerparty.websocket.helpers;

import com.google.gson.JsonObject;
import org.eclipse.jetty.websocket.api.RemoteEndpoint;
import org.eclipse.jetty.websocket.api.Session;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.mockito.Mockito.*;

class WebSocketJsonSenderTest {

    @Test
    void shouldSendJsonWhenSessionIsOpen() throws IOException {
        WebSocketJsonSender sender = new WebSocketJsonSender();
        Session session = mock(Session.class);
        RemoteEndpoint remote = mock(RemoteEndpoint.class);
        when(session.isOpen()).thenReturn(true);
        when(session.getRemote()).thenReturn(remote);
        JsonObject json = new JsonObject();
        json.addProperty("test", "data");

        sender.send(session, json);

        verify(remote).sendString(anyString());
    }

    @Test
    void shouldNotSendWhenSessionIsClosed() throws IOException {
        WebSocketJsonSender sender = new WebSocketJsonSender();
        Session session = mock(Session.class);
        when(session.isOpen()).thenReturn(false);

        sender.send(session, new JsonObject());

        verify(session, never()).getRemote();
    }
}
