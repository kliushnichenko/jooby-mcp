package io.github.kliushnichenko.jooby.mcp.transport;

import io.jooby.MediaType;
import lombok.experimental.UtilityClass;

/**
 * @author kliushnichenko
 */
@UtilityClass
class TransportConstants {

    public static final MediaType TEXT_EVENT_STREAM = MediaType.valueOf("text/event-stream");
    public static final String MESSAGE_EVENT_TYPE = "message";
    public static final String SSE_ERROR_EVENT = "Error";
}
