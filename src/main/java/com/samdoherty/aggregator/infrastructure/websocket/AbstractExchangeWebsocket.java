package com.samdoherty.aggregator.infrastructure.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.samdoherty.aggregator.infrastructure.configuration.Pair;
import jakarta.websocket.*;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.scheduling.annotation.Scheduled;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.channels.UnresolvedAddressException;
import java.util.List;
import java.util.concurrent.*;


/**
 * Extendable websocket implementation to generalize re-connect and websocket domain logic
 */
@Slf4j
public abstract class AbstractExchangeWebsocket {

    public abstract void subscribe();

    public abstract void readMessage(@NotNull String message);

    public abstract @NotNull String getName();

    public abstract @NotNull List<Pair> getPairs();

    /**
     * Ensure connection is still active and not stale
     * <p>
     * Call close() if not to initiate reconnection logic
     */
    public abstract void healthCheck();

    private Session session;

    /**
     * Async rather than basic to allow for timeouts
     */
    private RemoteEndpoint.Async asyncWebsocketRemote;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final String websocketUri;

    public AbstractExchangeWebsocket(@NotNull String websocketUri) {
        this.websocketUri = websocketUri;
        // Preempt time serialisation issues
        objectMapper.registerModule(new JavaTimeModule());
    }

    /**
     * Sneaky throws as we want the application to shut down if cannot initially start.
     * We don't want the upstream orchestration to worry about this
     */
    @SneakyThrows({URISyntaxException.class, DeploymentException.class, IOException.class})
    protected void connect() {
        connectToServer();
    }

    private void connectToServer() throws URISyntaxException, DeploymentException, IOException {

        WebSocketContainer container = ContainerProvider.getWebSocketContainer();

        container.setAsyncSendTimeout(3000);

        Future<Session> future = null;

        try (ExecutorService executor = Executors.newSingleThreadExecutor()) {
            future = executor.submit(() -> container.connectToServer(this, new URI(websocketUri)));
            session = future.get(2, TimeUnit.SECONDS); // 2s timeout
            subscribe();
        } catch (TimeoutException | InterruptedException | ExecutionException e) {
            future.cancel(true); // Cancel the hanging connection attempt
            throw new DeploymentException("Connection attempt timed out", e);
        }
    }

    protected void reconnectToServer(int attempt) {
        try {
            log.info("Reconnecting to {} server", getName());
            connectToServer();
            // Indicative of network issues
        } catch (UnresolvedAddressException | DeploymentException e) {
            try {
                Thread.sleep(getSleepTimer(attempt));
            } catch (InterruptedException ignored) {
                return;
            }
            reconnectToServer(attempt + 1);
        } catch (URISyntaxException | IOException e) {
            log.error("Reconnection failed: {}", e.getMessage(), e);
        }
    }

    /**
     * Minor backoff implementation to a max of 5 seconds
     *
     * @param attempt number. Sleeps between retries scales by attempt
     * @return sleep time in ms
     */
    private int getSleepTimer(int attempt) {
        return Math.min(attempt * 500, 5000);
    }

    public <M> void sendMessage(@NotNull M message) throws IOException {
        if (asyncWebsocketRemote == null) {
            return;
        }

        asyncWebsocketRemote.sendText(objectMapper.writeValueAsString(message), result -> {
            if (!result.isOK()) {
                log.error("Failed to send message to {}: {}. Resetting connection.", getName(), result);
                close();
            }
        });
    }

    @OnOpen
    public void onOpen(Session userSession) {
        log.info("Listening to {} currency pairs {}", getName(), StringUtils.join(getPairs(), ", "));
        asyncWebsocketRemote = userSession.getAsyncRemote();
        asyncWebsocketRemote.setSendTimeout(2000);
    }

    @OnMessage
    public void onMessage(String message) {
        readMessage(message);
    }

    @OnClose
    public void onClose(Session ignored, CloseReason reason) {
        log.error("{} websocket close: {}", getName(), reason.getReasonPhrase());
        ensureClosed();
        reconnectToServer(1);
    }

    @OnError
    public void onError(Session ignored, Throwable error) {
        log.error("{} websocket error: {}", getName(), error.getMessage(), error);
        ensureClosed();
        reconnectToServer(1);
    }

    public void close() {
        try {
            if (session != null) {
                session.close();
            }
        } catch (IOException e) {
            log.error("Failed to manually close", e);
        }
    }

    private void ensureClosed() {
        if (session != null && session.isOpen()) {
            try {
                session.close();
                session = null;
            } catch (IOException ignored) {
            }
        }

        if (asyncWebsocketRemote != null) {
            asyncWebsocketRemote = null;
        }
    }

    @Scheduled(initialDelay = 2000, fixedRate = 3000)
    public void scheduledHealthCheck() {
        healthCheck();
    }
}
