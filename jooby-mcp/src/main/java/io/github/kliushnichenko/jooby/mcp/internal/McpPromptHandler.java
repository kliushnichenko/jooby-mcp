package io.github.kliushnichenko.jooby.mcp.internal;

import io.github.kliushnichenko.jooby.mcp.JoobyMcpServer;
import io.modelcontextprotocol.spec.McpError;
import io.modelcontextprotocol.spec.McpSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static io.modelcontextprotocol.spec.McpSchema.ErrorCodes.INTERNAL_ERROR;
import static io.modelcontextprotocol.spec.McpSchema.Role.USER;

public class McpPromptHandler {

    private final Logger log = LoggerFactory.getLogger(McpPromptHandler.class);

    public McpSchema.GetPromptResult handle(JoobyMcpServer server, McpSchema.GetPromptRequest request) {
        try {
            var promptName = request.name();
            if (!server.getPrompts().containsKey(promptName)) {
                throw new IllegalArgumentException("Prompt '" + promptName + "' is not registered.");
            }

            Object result = server.invokePrompt(promptName, request.arguments());

            if (result == null) {
                return new McpSchema.GetPromptResult(null, List.of());
            } else if (result instanceof McpSchema.GetPromptResult promptResult) {
                return promptResult;
            } else if (result instanceof McpSchema.PromptMessage promptMessage) {
                return new McpSchema.GetPromptResult(null, List.of(promptMessage));
            } else if (result instanceof McpSchema.Content content) {
                var promptMessage = new McpSchema.PromptMessage(USER, content);
                return new McpSchema.GetPromptResult(null, List.of(promptMessage));
            } else if (result instanceof String str) {
                var promptMessage = new McpSchema.PromptMessage(USER, new McpSchema.TextContent(str));
                return new McpSchema.GetPromptResult(null, List.of(promptMessage));
            } else {
                var promptMessage = new McpSchema.PromptMessage(USER, new McpSchema.TextContent(result.toString()));
                return new McpSchema.GetPromptResult(null, List.of(promptMessage));
            }
        } catch (Exception ex) {
            log.error("Unexpected error while handling prompt request", ex);
            throw new McpError(new McpSchema.JSONRPCResponse.JSONRPCError(
                    INTERNAL_ERROR,
                    ex.getMessage(),
                    null
            ));
        }
    }
}
