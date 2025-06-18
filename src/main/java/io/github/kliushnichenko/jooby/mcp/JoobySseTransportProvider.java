package io.github.kliushnichenko.jooby.mcp;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.typesafe.config.Config;
import io.jooby.*;
import io.modelcontextprotocol.spec.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Provides SSE transport implementation for MCP server using Jooby framework.
 * Handles client connections, message routing, and session management.
 */
public class JoobySseTransportProvider implements McpServerTransportProvider {

    private static final Logger LOG = LoggerFactory.getLogger(JoobySseTransportProvider.class);

    private static final String MESSAGE_EVENT_TYPE = "message";
    private static final String ENDPOINT_EVENT_TYPE = "endpoint";
    private static final String SESSION_ID_KEY = "sessionId";

    private static final String DEFAULT_SSE_ENDPOINT = "/mcp/sse";
    private static final String DEFAULT_MESSAGE_ENDPOINT = "/mcp/message";

    private final Config moduleConfig;
    private final String messageEndpoint;
    private final ObjectMapper objectMapper;
    private final ConcurrentHashMap<String, McpServerSession> sessions = new ConcurrentHashMap<>();

    private McpServerSession.Factory sessionFactory;
    private final AtomicBoolean isClosing = new AtomicBoolean(false);

    /**
     * Constructs a new Jooby Reactive SSE transport provider instance.
     *
     * @param objectMapper The ObjectMapper to use for JSON serialization/deserialization of MCP messages
     * @param app          The Jooby application instance to register endpoints with
     * @param moduleConfig Module configuration properties
     */
    public JoobySseTransportProvider(ObjectMapper objectMapper, Jooby app, Config moduleConfig) {
        this.moduleConfig = moduleConfig;
        this.objectMapper = objectMapper;
        this.messageEndpoint = resolveConfigParam("messageEndpoint", DEFAULT_MESSAGE_ENDPOINT);
        String sseEndpoint = resolveConfigParam("sseEndpoint", DEFAULT_SSE_ENDPOINT);

        app.head(sseEndpoint, ctx -> {
            ctx.setResponseHeader("Content-Type", "text/event-stream");
            return StatusCode.OK;
        });
        app.sse(sseEndpoint, this::handleSseConnection);
        app.post(this.messageEndpoint, this::handleMessage);
    }

    private String resolveConfigParam(String configPath, String defaultValue) {
        return moduleConfig.hasPath(configPath) ? moduleConfig.getString(configPath) : defaultValue;
    }

    @Override
    public void setSessionFactory(McpServerSession.Factory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    @Override
    public Mono<Void> notifyClients(String method, Object params) {
        if (sessions.isEmpty()) {
            LOG.debug("No active sessions to broadcast message to");
            return Mono.empty();
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("Attempting to broadcast message to {} active sessions", sessions.size());
        }

        return Flux.fromIterable(sessions.values())
                .flatMap(session -> session.sendNotification(method, params)
                        .doOnError(
                                e -> LOG.error("Failed to send message to session {}: {}", session.getId(),
                                        e.getMessage()))
                        .onErrorComplete())
                .then();
    }

    @Override
    public Mono<Void> closeGracefully() {
        return Flux.fromIterable(sessions.values())
                .doFirst(() -> {
                    isClosing.set(true);
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Initiating graceful shutdown with {} active sessions", sessions.size());
                    }
                })
                .flatMap(McpServerSession::closeGracefully)
                .doFinally(signalType -> sessions.clear())
                .then();
    }

    private void handleSseConnection(ServerSentEmitter sse) {
        JoobyMcpSessionTransport transport = new JoobyMcpSessionTransport(sse);
        McpServerSession session = sessionFactory.create(transport);
        String sessionId = session.getId();
        transport.setSessionId(sessionId);

        LOG.debug("New SSE connection has been established. Session ID: {}", sessionId);
        sessions.put(sessionId, session);

        sse.onClose(() -> {
            LOG.debug("Session with ID {} has been cancelled", sessionId);
            sessions.remove(sessionId);
        });

        LOG.debug("Sending initial endpoint event to session: {}", sessionId);
        sse.send(new ServerSentMessage(this.messageEndpoint + "?sessionId=" + sessionId)
                .setEvent(ENDPOINT_EVENT_TYPE)
        );
    }

    private Object handleMessage(Context ctx) {
        if (isClosing.get()) {
            ctx.setResponseCode(StatusCode.SERVICE_UNAVAILABLE);
            return new McpError("Server is shutting down");
        }

        if (ctx.query(SESSION_ID_KEY).isMissing()) {
            ctx.setResponseCode(StatusCode.BAD_REQUEST);
            return new McpError("Session ID missing in message endpoint");
        }

        String sessionId = ctx.query(SESSION_ID_KEY).value();
        McpServerSession session = sessions.get(sessionId);

        if (session == null) {
            ctx.setResponseCode(StatusCode.NOT_FOUND);
            return new McpError("Session not found: " + sessionId);
        }

        try {
            var body = ctx.body().value();
            McpSchema.JSONRPCMessage message = McpSchema.deserializeJsonRpcMessage(this.objectMapper, body);

            return session.handle(message).then(Mono.just((Object) StatusCode.OK))
                    .onErrorResume(error -> {
                        LOG.error("Error processing  message: {}", error.getMessage());
                        return Mono.just(StatusCode.OK);
                    }).switchIfEmpty(Mono.just((Object) StatusCode.OK))
                    .block();
        } catch (IOException | IllegalArgumentException e) {
            LOG.error("Failed to deserialize message: {}", e.getMessage());
            return new McpError("Invalid message format");
        }
    }

    private class JoobyMcpSessionTransport implements McpServerTransport {

        private final ServerSentEmitter sse;
        private String sessionId;

        public JoobyMcpSessionTransport(ServerSentEmitter sse) {
            this.sse = sse;
        }

        public void setSessionId(String sessionId) {
            this.sessionId = sessionId;
        }

        @Override
        public Mono<Void> sendMessage(McpSchema.JSONRPCMessage message) {
            return Mono.fromRunnable(() -> {
                try {
                    String jsonText = objectMapper.writeValueAsString(message);
                    sse.send(new ServerSentMessage(jsonText).setEvent(MESSAGE_EVENT_TYPE));
                    LOG.debug("Message sent to session {}: {}", sessionId, message);
                } catch (Exception e) {
                    LOG.error("Failed to send message to session {}: {}", sessionId, e.getMessage());
                    sse.send("Error", e.getMessage());
                }
            });
        }

        @Override
        public <T> T unmarshalFrom(Object data, TypeReference<T> typeRef) {
            return objectMapper.convertValue(data, typeRef);
        }

        @Override
        public Mono<Void> closeGracefully() {
            return Mono.fromRunnable(sse::close);
        }

        @Override
        public void close() {
            sse.close();
        }
    }
}
