package io.github.kliushnichenko.jooby.mcp.transport;

import io.jooby.Context;
import io.jooby.MediaType;
import io.jooby.StatusCode;
import io.modelcontextprotocol.spec.HttpHeaders;
import io.modelcontextprotocol.spec.McpSchema;

import java.util.List;

class Error {

    static McpSchema.JSONRPCResponse serverIsShuttingDown(Context ctx) {
        ctx.setResponseCode(StatusCode.SERVICE_UNAVAILABLE);
        return err(
                new McpSchema.JSONRPCResponse.JSONRPCError(
                        McpSchema.ErrorCodes.INTERNAL_ERROR,
                        "Server is shutting down",
                        null)
        );
    }

    static McpSchema.JSONRPCResponse invalidAcceptHeader(Context ctx, List<MediaType> acceptedTypes) {
        ctx.setResponseCode(StatusCode.BAD_REQUEST);
        return err(
                new McpSchema.JSONRPCResponse.JSONRPCError(
                        McpSchema.ErrorCodes.INVALID_REQUEST,
                        "Invalid Accept header. Expected: %s".formatted(acceptedTypes),
                        null)
        );
    }

    static McpSchema.JSONRPCResponse missingSessionId(Context ctx) {
        ctx.setResponseCode(StatusCode.BAD_REQUEST);
        return err(
                new McpSchema.JSONRPCResponse.JSONRPCError(
                        McpSchema.ErrorCodes.INVALID_REQUEST,
                        "Session ID required in %s header".formatted(HttpHeaders.MCP_SESSION_ID),
                        null)
        );
    }

    static McpSchema.JSONRPCResponse sessionNotFound(Context ctx, String sessionId) {
        ctx.setResponseCode(StatusCode.NOT_FOUND);
        return err(
                new McpSchema.JSONRPCResponse.JSONRPCError(
                        McpSchema.ErrorCodes.INVALID_REQUEST,
                        "Session %s not found".formatted(sessionId),
                        null)
        );
    }

    static McpSchema.JSONRPCResponse unknownMsgType(Context ctx, String sessionId) {
        ctx.setResponseCode(StatusCode.BAD_REQUEST);
        return err(
                new McpSchema.JSONRPCResponse.JSONRPCError(
                        McpSchema.ErrorCodes.INVALID_REQUEST,
                        "Unknown message type. Session ID: %s".formatted(sessionId),
                        null)
        );
    }

    static McpSchema.JSONRPCResponse msgParseError(Context ctx, String sessionId) {
        ctx.setResponseCode(StatusCode.BAD_REQUEST);
        return err(
                new McpSchema.JSONRPCResponse.JSONRPCError(
                        McpSchema.ErrorCodes.PARSE_ERROR,
                        "Invalid message format. Session ID: %s".formatted(sessionId),
                        null)
        );
    }

    static McpSchema.JSONRPCResponse deletionNotAllowed(Context ctx) {
        ctx.setResponseCode(StatusCode.METHOD_NOT_ALLOWED);
        return err(
                new McpSchema.JSONRPCResponse.JSONRPCError(
                        McpSchema.ErrorCodes.INVALID_REQUEST,
                        "Session deletion is not allowed",
                        null)
        );
    }

    static McpSchema.JSONRPCResponse internalError(Context ctx, String sessionId) {
        ctx.setResponseCode(StatusCode.SERVER_ERROR);
        return err(
                new McpSchema.JSONRPCResponse.JSONRPCError(
                        McpSchema.ErrorCodes.INTERNAL_ERROR,
                        "Internal Server Error. Session ID: %s".formatted(sessionId),
                        null)
        );
    }


    public static McpSchema.JSONRPCResponse error(Context ctx, StatusCode statusCode, int errCode, String msg) {
        ctx.setResponseCode(statusCode);
        return err(
                new McpSchema.JSONRPCResponse.JSONRPCError(errCode, msg, null)
        );
    }

    private static McpSchema.JSONRPCResponse err(McpSchema.JSONRPCResponse.JSONRPCError err) {
        return new McpSchema.JSONRPCResponse(McpSchema.JSONRPC_VERSION, null, null, err);
    }
}
