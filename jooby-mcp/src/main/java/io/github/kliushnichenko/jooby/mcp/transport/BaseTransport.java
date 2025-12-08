package io.github.kliushnichenko.jooby.mcp.transport;

import io.jooby.MediaType;

/**
 * @author kliushnichenko
 */
public abstract class BaseTransport {

    protected static final MediaType TEXT_EVENT_STREAM = MediaType.valueOf("text/event-stream");
    protected static final String MESSAGE_EVENT_TYPE = "message";
    protected static final String SSE_ERROR_EVENT = "Error";
}
