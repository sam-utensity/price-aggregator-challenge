package com.samdoherty.aggregator.infrastructure.websocket;

import com.samdoherty.aggregator.infrastructure.configuration.Pair;
import jakarta.websocket.CloseReason;
import jakarta.websocket.RemoteEndpoint;
import jakarta.websocket.SendHandler;
import jakarta.websocket.SendResult;
import jakarta.websocket.Session;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AbstractExchangeWebsocketTest {
    // Example concrete subclass for testing
    static class TestWebsocket extends AbstractExchangeWebsocket {
        boolean healthChecked = false;
        boolean subscribed = false;
        boolean messageRead = false;
        int reconnectCalled = 0;

        public TestWebsocket(String uri) {
            super(uri);
        }

        @Override
        public void subscribe() {
            subscribed = true;
        }

        @Override
        public void readMessage(@NotNull String message) {
            messageRead = true;
        }

        @Override
        public @NotNull String getName() {
            return "Test";
        }

        @Override
        public @NotNull List<Pair> getPairs() {
            return List.of();
        }

        @Override
        public void healthCheck() {
            healthChecked = true;
        }

        @Override
        protected void reconnectToServer(int attempt) {
            reconnectCalled++;
        }
    }

    private TestWebsocket websocket;

    @BeforeEach
    void setUp() {
        websocket = new TestWebsocket("ws://localhost");
    }

    @Test
    void testGetName() {
        assertEquals("Test", websocket.getName());
    }

    @Test
    void testGetPairs() {
        assertTrue(websocket.getPairs().isEmpty());
    }

    @Test
    void testOnOpenSetsAsyncRemote() {
        Session session = mock(Session.class);
        RemoteEndpoint.Async async = mock(RemoteEndpoint.Async.class);
        when(session.getAsyncRemote()).thenReturn(async);
        websocket.onOpen(session);
        verify(session).getAsyncRemote();
        verify(async).setSendTimeout(2000L);
    }

    @Test
    void testOnMessageDelegatesToReadMessage() {
        websocket.messageRead = false;
        websocket.onMessage("test");
        assertTrue(websocket.messageRead);
    }

    @Test
    void testOnCloseEnsuresClosedAndReconnects() {
        Session session = mock(Session.class);
        CloseReason reason = mock(CloseReason.class);
        when(reason.getReasonPhrase()).thenReturn("Closed");
        websocket.onClose(session, reason);
        assertEquals(1, websocket.reconnectCalled);
    }

    @Test
    void testOnErrorEnsuresClosedAndReconnects() {
        Session session = mock(Session.class);
        Throwable error = new RuntimeException("fail");
        websocket.onError(session, error);
        assertEquals(1, websocket.reconnectCalled);
    }

    @Test
    void testSendMessageWithNullAsyncRemoteDoesNothing() throws IOException {
        websocket.sendMessage("test");
    }

    @Test
    void testSendMessageWithAsyncRemoteSendsText() throws IOException, NoSuchFieldException, IllegalAccessException {
        var async = mock(RemoteEndpoint.Async.class);
        var field = AbstractExchangeWebsocket.class.getDeclaredField("asyncWebsocketRemote");
        field.setAccessible(true);
        field.set(websocket, async);
        doAnswer(invocation -> {
            SendHandler handler = invocation.getArgument(1);
            handler.onResult(new SendResult()); // success
            return null;
        }).when(async).sendText(anyString(), any(SendHandler.class));
        websocket.sendMessage("test");
        verify(async).sendText(contains("test"), any(SendHandler.class));
    }

    @Test
    void testSendMessageFailureTriggersClose() throws Exception {
        var async = mock(RemoteEndpoint.Async.class);
        var field = AbstractExchangeWebsocket.class.getDeclaredField("asyncWebsocketRemote");
        field.setAccessible(true);
        field.set(websocket, async);
        doAnswer(invocation -> {
            SendHandler handler = invocation.getArgument(1);
            handler.onResult(new SendResult(new Throwable("fail"))); // failure
            return null;
        }).when(async).sendText(anyString(), any(SendHandler.class));
        var spy = Mockito.spy(websocket);
        field.set(spy, async);
        spy.sendMessage("fail");
        verify(async).sendText(contains("fail"), any(SendHandler.class));
        verify(spy).close();
    }

    @Test
    void testCloseHandlesIOException() throws Exception {
        var session = mock(Session.class);
        doThrow(new IOException("fail")).when(session).close();
        var field = AbstractExchangeWebsocket.class.getDeclaredField("session");
        field.setAccessible(true);
        field.set(websocket, session);
        websocket.close();
        verify(session).close();
    }

    @Test
    void testScheduledHealthCheckCallsHealthCheck() {
        websocket.healthChecked = false;
        websocket.scheduledHealthCheck();
        assertTrue(websocket.healthChecked);
    }
} 