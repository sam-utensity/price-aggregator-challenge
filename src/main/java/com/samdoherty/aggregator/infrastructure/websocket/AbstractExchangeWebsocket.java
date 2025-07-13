package com.samdoherty.aggregator.infrastructure.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.samdoherty.aggregator.infrastructure.configuration.Pair;
import jakarta.websocket.*;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;


@Slf4j
public abstract class AbstractExchangeWebsocket {

    public abstract void subscribe();

    public abstract void readMessage(@NotNull String message);

    public abstract @NotNull String getName();

    public abstract @NotNull List<Pair> getPairs();

    private Session session;
    private RemoteEndpoint.Basic basicRemote;

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

        //TODO retry logic for ONLY server connection issues e.g. wifi down
        session = container.connectToServer(this, new URI(websocketUri));

        subscribe();
    }

    private void reconnectToServer() {
        try {
            log.info("Reconnecting to {} server", getName());
            connectToServer();
        } catch (URISyntaxException | DeploymentException | IOException e) {
            // TODO figure out which of these are reconnect worthy
            // TODO backoff logic?
            log.error(e.getMessage(), e);
        } catch (Exception e) {
            throw e;
        }
    }

    public <M> void sendMessage(@NotNull M message) throws IOException {
        basicRemote.sendText(objectMapper.writeValueAsString(message));
    }

    @OnOpen
    public void onOpen(Session userSession) {
        log.info("Listening to {} currency pairs {}", getName(), StringUtils.join(getPairs(), ", "));
        basicRemote = userSession.getBasicRemote();
    }

    @OnMessage
    public void onMessage(String message) {
        readMessage(message);
    }

    @OnClose
    public void onClose(Session userSession, CloseReason reason) {
        log.error("{} websocket close: {}", getName(), reason.getReasonPhrase());
        ensureClosed();
        reconnectToServer();
    }

    @OnError
    public void onError(Session userSession, Throwable error) {
        log.error("{} websocket error: {}", getName(), error.getMessage(), error);
        ensureClosed();
        reconnectToServer();
    }

    protected void close() {
        try {
            session.close();
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

        if (basicRemote != null) {
            basicRemote = null;
        }
    }
}
