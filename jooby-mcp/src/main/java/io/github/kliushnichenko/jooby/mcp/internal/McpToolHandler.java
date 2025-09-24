package io.github.kliushnichenko.jooby.mcp.internal;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.kliushnichenko.jooby.mcp.JoobyMcpServer;
import io.modelcontextprotocol.spec.McpSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class McpToolHandler {

    private static final Logger LOG = LoggerFactory.getLogger(McpToolHandler.class);

    private final ObjectMapper objectMapper;

    public McpToolHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public McpSchema.CallToolResult handle(McpSchema.CallToolRequest request, JoobyMcpServer server) {
        String toolName = request.name();
        try {
            if (!server.getTools().containsKey(toolName)) {
                throw new IllegalArgumentException("Tool '" + toolName + "' is not registered.");
            }

            Object result = server.invokeTool(toolName, request.arguments());

            if (result == null) {
                return new McpSchema.CallToolResult("null", false);
            } else if (result instanceof McpSchema.CallToolResult callToolResult) {
                return callToolResult;
            } else if (result instanceof String str) {
                return new McpSchema.CallToolResult(str, false);
            } else if (result instanceof McpSchema.Content content) {
                return new McpSchema.CallToolResult(List.of(content), false);
            } else {
                var resultStr = objectMapper.writeValueAsString(result);
                return new McpSchema.CallToolResult(resultStr, false);
            }
        } catch (Exception ex) {
            LOG.error("Error invoking tool '{}': {}", toolName, ex.getMessage(), ex);
            return new McpSchema.CallToolResult(ex.getMessage(), true);
        }
    }
}
