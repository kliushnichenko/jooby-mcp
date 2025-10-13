package io.github.kliushnichenko.jooby.mcp;

import io.github.kliushnichenko.jooby.mcp.internal.McpServerConfig;
import io.jooby.*;
import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.json.TypeRef;
import io.modelcontextprotocol.server.McpTransportContextExtractor;
import io.modelcontextprotocol.spec.*;
import io.modelcontextprotocol.util.KeepAliveScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import static io.modelcontextprotocol.spec.McpSchema.ErrorCodes.INVALID_REQUEST;

public class JoobyStreamableServerTransportProvider implements McpStreamableServerTransportProvider {

    private static final Logger log = LoggerFactory.getLogger(JoobyStreamableServerTransportProvider.class);

    private static final MediaType TEXT_EVENT_STREAM = MediaType.valueOf("text/event-stream");

    /**
     * Event type for JSON-RPC messages sent through the SSE connection.
     */
    public static final String MESSAGE_EVENT_TYPE = "message";

    /**
     * Flag indicating whether DELETE requests are disallowed on the endpoint.
     */
    private final boolean disallowDelete;
    private final McpJsonMapper mcpJsonMapper;
    private McpStreamableServerSession.Factory sessionFactory;

    /**
     * Map of active client sessions, keyed by mcp-session-id.
     */
    private final ConcurrentHashMap<String, McpStreamableServerSession> sessions = new ConcurrentHashMap<>();

    private final McpTransportContextExtractor<Context> contextExtractor;

    /**
     * Flag indicating if the transport is shutting down.
     */
    private volatile boolean isClosing = false;
    private KeepAliveScheduler keepAliveScheduler;

    public JoobyStreamableServerTransportProvider(Jooby app,
                                                  McpJsonMapper jsonMapper,
                                                  McpServerConfig serverConfig,
                                                  McpTransportContextExtractor<Context> contextExtractor) {
        Objects.requireNonNull(contextExtractor, "McpTransportContextExtractor must not be null");

        this.mcpJsonMapper = jsonMapper;
        this.disallowDelete = serverConfig.isDisallowDelete();
        this.contextExtractor = contextExtractor;

        var mcpEndpoint = serverConfig.getMcpEndpoint();

        app.head(mcpEndpoint, ctx -> StatusCode.OK).produces(TEXT_EVENT_STREAM);
        app.sse(mcpEndpoint, this::handleGet);
        app.post(mcpEndpoint, this::handlePost);
        app.delete(mcpEndpoint, this::handleDelete);

        if (serverConfig.getKeepAliveInterval() != null) {
            var keepAliveInterval = Duration.ofSeconds(serverConfig.getKeepAliveInterval());
            this.keepAliveScheduler = KeepAliveScheduler
                    .builder(() -> (isClosing) ? Flux.empty() : Flux.fromIterable(this.sessions.values()))
                    .initialDelay(keepAliveInterval)
                    .interval(keepAliveInterval)
                    .build();

            this.keepAliveScheduler.start();
        }
    }

    /**
     * Setups the listening SSE connections and message replay.
     *
     * @param sse ServerSentEmitter provided by Jooby for SSE communication
     */
    private void handleGet(ServerSentEmitter sse) {
        Context ctx = sse.getContext();
        if (this.isClosing) {
            ctx.setResponseCode(StatusCode.SERVICE_UNAVAILABLE).send("Server is shutting down");
            return;
        }

        if (!ctx.accept(TEXT_EVENT_STREAM)) {
            ctx.setResponseCode(StatusCode.BAD_REQUEST)
                    .send("Invalid Accept header. Expected 'text/event-stream'");
            return;
        }

        McpTransportContext transportContext = this.contextExtractor.extract(sse.getContext());

        if (ctx.header(HttpHeaders.MCP_SESSION_ID).isMissing()) {
            ctx.setResponseCode(StatusCode.BAD_REQUEST).send("Session ID required in mcp-session-id header");
            return;
        }

        String sessionId = ctx.header(HttpHeaders.MCP_SESSION_ID).value();
        McpStreamableServerSession session = this.sessions.get(sessionId);

        if (session == null) {
            ctx.setResponseCode(StatusCode.NOT_FOUND).send("Session not found: " + sessionId);
            return;
        }

        log.debug("Handling GET request for session: {}", sessionId);

        try {
            var sessionTransport = new JoobyStreamableMcpSessionTransport(sessionId, sse);

            // Check if this is a replay request
            if (ctx.header(HttpHeaders.LAST_EVENT_ID).isPresent()) {
                String lastId = ctx.header(HttpHeaders.LAST_EVENT_ID).value();

                try {
                    session.replay(lastId)
                            .contextWrite(reactorCtx -> reactorCtx.put(McpTransportContext.KEY, transportContext))
                            .toIterable()
                            .forEach(message -> {
                                try {
                                    sessionTransport.sendMessage(message)
                                            .contextWrite(reactorCtx -> reactorCtx.put(McpTransportContext.KEY, transportContext))
                                            .block();
                                } catch (Exception e) {
                                    log.error("Failed to replay message: {}", e.getMessage());
                                    sse.send("Error", e.getMessage());
                                }
                            });
                } catch (Exception e) {
                    log.error("Failed to replay messages: {}", e.getMessage());
                    sse.send("Error", e.getMessage());
                }
            } else {
                // Establish new listening stream
                McpStreamableServerSession.McpStreamableServerSessionStream listeningStream = session
                        .listeningStream(sessionTransport);

                sse.onClose(() -> {
                    log.debug("SSE connection has been closed for session: {}", sessionId);
                    listeningStream.close();
                });
            }
        } catch (Exception e) {
            log.error("Failed to handle GET request for session {}: {}", sessionId, e.getMessage());
            ctx.setResponseCode(StatusCode.SERVER_ERROR);
        }
    }

    /**
     * Handles POST requests for incoming JSON-RPC messages from clients.
     *
     * @param ctx The Jooby context for the incoming request
     */
    private Object handlePost(Context ctx) {
        if (this.isClosing) {
            ctx.setResponseCode(StatusCode.SERVICE_UNAVAILABLE);
            return McpError.builder(McpSchema.ErrorCodes.INTERNAL_ERROR)
                    .message("Server is shutting down")
                    .build();
        }

        if (!ctx.accept(TEXT_EVENT_STREAM) || !ctx.accept(MediaType.json)) {
            ctx.setResponseCode(StatusCode.BAD_REQUEST);
            return McpError.builder(INVALID_REQUEST)
                    .message("Invalid Accept headers. Expected 'text/event-stream' and 'application/json'")
                    .build();
        }

        McpTransportContext transportContext = this.contextExtractor.extract(ctx);

        try {
            var body = ctx.body().value();
            McpSchema.JSONRPCMessage message = McpSchema.deserializeJsonRpcMessage(mcpJsonMapper, body);

            // Handle initialization request
            if (message instanceof McpSchema.JSONRPCRequest jsonrpcRequest
                && jsonrpcRequest.method().equals(McpSchema.METHOD_INITIALIZE)) {

                McpSchema.InitializeRequest initRequest = mcpJsonMapper.convertValue(
                        jsonrpcRequest.params(),
                        McpSchema.InitializeRequest.class
                );
                McpStreamableServerSession.McpStreamableServerSessionInit initObj = this.sessionFactory
                        .startSession(initRequest);
                this.sessions.put(initObj.session().getId(), initObj.session());

                try {
                    McpSchema.InitializeResult initResult = initObj.initResult().block();

                    ctx.setResponseHeader(HttpHeaders.MCP_SESSION_ID, initObj.session().getId());
                    return new McpSchema.JSONRPCResponse(
                            McpSchema.JSONRPC_VERSION,
                            jsonrpcRequest.id(),
                            initResult,
                            null
                    );
                } catch (Exception e) {
                    log.error("Failed to initialize session: {}", e.getMessage());
                    ctx.setResponseCode(StatusCode.SERVER_ERROR);
                    return McpError.builder(McpSchema.ErrorCodes.INTERNAL_ERROR)
                            .message(e.getMessage())
                            .build();
                }
            }

            // Handle other messages that require a session
            if (ctx.header(HttpHeaders.MCP_SESSION_ID).isMissing()) {
                return McpError.builder(McpSchema.ErrorCodes.INVALID_REQUEST)
                        .message("Session ID is missing. Please provide mcp-session-id header")
                        .build();
            }

            String sessionId = ctx.header(HttpHeaders.MCP_SESSION_ID).value();
            McpStreamableServerSession session = this.sessions.get(sessionId);

            if (session == null) {
                return McpError.builder(McpSchema.ErrorCodes.RESOURCE_NOT_FOUND)
                        .message("Session not found: " + sessionId)
                        .build();
            }

            if (message instanceof McpSchema.JSONRPCResponse jsonrpcResponse) {
                session.accept(jsonrpcResponse)
                        .contextWrite(reactorCtx -> reactorCtx.put(McpTransportContext.KEY, transportContext))
                        .block();
                return StatusCode.ACCEPTED;
            } else if (message instanceof McpSchema.JSONRPCNotification jsonrpcNotification) {
                session.accept(jsonrpcNotification)
                        .contextWrite(reactorCtx -> reactorCtx.put(McpTransportContext.KEY, transportContext))
                        .block();
                return StatusCode.ACCEPTED;
            } else if (message instanceof McpSchema.JSONRPCRequest jsonrpcRequest) {
                ctx.setResponseType(TEXT_EVENT_STREAM, StandardCharsets.UTF_8);

                ctx.upgrade(sse -> {
                    sse.onClose(() -> {
                        log.debug("Request response stream completed for session: {}", sessionId);
                    });

                    JoobyStreamableMcpSessionTransport sessionTransport = new JoobyStreamableMcpSessionTransport(
                            sessionId, sse);

                    try {
                        session.responseStream(jsonrpcRequest, sessionTransport)
                                .contextWrite(reactorCtx -> reactorCtx.put(McpTransportContext.KEY, transportContext))
                                .block();
                    } catch (Exception e) {
                        log.error("Failed to handle request stream: {}", e.getMessage());
                        sse.send("Error", e.getMessage());
                    }
                });
                return StatusCode.OK; // Response is handled in the upgrade block
            } else {
                return McpError.builder(McpSchema.ErrorCodes.INTERNAL_ERROR)
                        .message("Unknown message type")
                        .build();
            }
        } catch (IllegalArgumentException | IOException e) {
            log.error("Failed to deserialize message: {}", e.getMessage());
            return McpError.builder(McpSchema.ErrorCodes.PARSE_ERROR)
                    .message("Invalid message format")
                    .build();
        } catch (Exception e) {
            log.error("Error handling message: {}", e.getMessage());
            return McpError.builder(McpSchema.ErrorCodes.INTERNAL_ERROR)
                    .message(e.getMessage())
                    .build();
        }
    }

    /**
     * Handles DELETE requests for session deletion.
     *
     * @param ctx The Jooby context for the incoming request
     * @return A ServerResponse indicating success or appropriate error status
     */
    private Object handleDelete(Context ctx) {
        if (this.isClosing) {
            ctx.setResponseCode(StatusCode.SERVICE_UNAVAILABLE);
            return McpError.builder(McpSchema.ErrorCodes.INTERNAL_ERROR)
                    .message("Server is shutting down")
                    .build();
        }

        if (this.disallowDelete) {
            ctx.setResponseCode(StatusCode.METHOD_NOT_ALLOWED);
            return McpError.builder(INVALID_REQUEST)
                    .message("Session deletion is not allowed")
                    .build();
        }

        McpTransportContext transportContext = this.contextExtractor.extract(ctx);

        if (ctx.header(HttpHeaders.MCP_SESSION_ID).isMissing()) {
            ctx.setResponseCode(StatusCode.BAD_REQUEST);
            return McpError.builder(INVALID_REQUEST)
                    .message("Session ID required in mcp-session-id header")
                    .build();
        }

        String sessionId = ctx.header(HttpHeaders.MCP_SESSION_ID).value();
        McpStreamableServerSession session = this.sessions.get(sessionId);

        if (session == null) {
            return McpError.builder(McpSchema.ErrorCodes.RESOURCE_NOT_FOUND)
                    .message("Session not found: " + sessionId)
                    .build();
        }

        try {
            session.delete()
                    .contextWrite(reactorCtx -> reactorCtx.put(McpTransportContext.KEY, transportContext))
                    .block();
            this.sessions.remove(sessionId);
            return StatusCode.OK;
        } catch (Exception e) {
            log.error("Failed to delete session {}: {}", sessionId, e.getMessage());
            return McpError.builder(McpSchema.ErrorCodes.INTERNAL_ERROR)
                    .message(e.getMessage())
                    .build();
        }
    }

    @Override
    public void setSessionFactory(McpStreamableServerSession.Factory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    @Override
    public Mono<Void> notifyClients(String method, Object params) {
        if (this.sessions.isEmpty()) {
            log.debug("No active sessions to broadcast message to");
            return Mono.empty();
        }

        log.debug("Attempting to broadcast message to {} active sessions", this.sessions.size());

        return Mono.fromRunnable(() -> {
            this.sessions.values().parallelStream().forEach(session -> {
                try {
                    session.sendNotification(method, params).block();
                } catch (Exception e) {
                    log.error("Failed to send message to session {}: {}", session.getId(), e.getMessage());
                }
            });
        });
    }

    @Override
    public Mono<Void> closeGracefully() {
        return Mono.fromRunnable(() -> {
            this.isClosing = true;
            log.debug("Initiating graceful shutdown with {} active sessions", this.sessions.size());

            this.sessions.values().parallelStream().forEach(session -> {
                try {
                    session.closeGracefully().block();
                } catch (Exception e) {
                    log.error("Failed to close session {}: {}", session.getId(), e.getMessage());
                }
            });

            this.sessions.clear();
            log.debug("Graceful shutdown completed");
        }).then().doOnSuccess(v -> {
            if (this.keepAliveScheduler != null) {
                this.keepAliveScheduler.shutdown();
            }
        });
    }

    private class JoobyStreamableMcpSessionTransport implements McpStreamableServerTransport {

        private final String sessionId;

        private final ServerSentEmitter sse;

        private volatile boolean closed = false;

        JoobyStreamableMcpSessionTransport(String sessionId, ServerSentEmitter sse) {
            this.sessionId = sessionId;
            this.sse = sse;
            log.debug("Streamable session transport {} initialized with SSE", sessionId);
        }

        /**
         * Sends a JSON-RPC message to the client through the SSE connection.
         *
         * @param message The JSON-RPC message to send
         * @return A Mono that completes when the message has been sent
         */
        @Override
        public Mono<Void> sendMessage(McpSchema.JSONRPCMessage message) {
            return sendMessage(message, null);
        }

        /**
         * Sends a JSON-RPC message to the client through the SSE connection with a
         * specific message ID.
         *
         * @param message   The JSON-RPC message to send
         * @param messageId The message ID for SSE event identification
         * @return A Mono that completes when the message has been sent
         */
        @Override
        public Mono<Void> sendMessage(McpSchema.JSONRPCMessage message, String messageId) {
            return Mono.fromRunnable(() -> {
                try {
                    if (this.closed) {
                        log.debug("Session {} was closed during message send attempt", this.sessionId);
                        return;
                    }

                    String jsonText = mcpJsonMapper.writeValueAsString(message);
                    sse.send(new ServerSentMessage(jsonText)
                            .setId(messageId != null ? messageId : this.sessionId)
                            .setEvent(MESSAGE_EVENT_TYPE));
                    log.debug("Message sent to session {} with ID {}", this.sessionId, messageId);
                } catch (Exception e) {
                    log.error("Failed to send message to session {}: {}", this.sessionId, e.getMessage());
                    try {
                        sse.send("Error", e.getMessage());
                    } catch (Exception errorEx) {
                        log.error("Failed to send error to SSE session {}: {}", this.sessionId, errorEx.getMessage());
                    }
                }
            });
        }

        /**
         * Converts data from one type to another using the configured McpJsonMapper.
         *
         * @param data    The source data object to convert
         * @param typeRef The target type reference
         * @param <T>     The target type
         * @return The converted object of type T
         */
        @Override
        public <T> T unmarshalFrom(Object data, TypeRef<T> typeRef) {
            return mcpJsonMapper.convertValue(data, typeRef);
        }

        /**
         * Initiates a graceful shutdown of the transport.
         *
         * @return A Mono that completes when the shutdown is complete
         */
        @Override
        public Mono<Void> closeGracefully() {
            return Mono.fromRunnable(JoobyStreamableMcpSessionTransport.this::close);
        }

        /**
         * Closes the transport immediately.
         */
        @Override
        public void close() {
            try {
                if (this.closed) {
                    log.debug("Session transport {} already closed", this.sessionId);
                    return;
                }

                this.closed = true;
                sse.close();
                log.debug("Successfully closed SSE session {}", sessionId);
            } catch (Exception e) {
                log.warn("Failed to close SSE session {}: {}", sessionId, e.getMessage());
            }
        }
    }
}
