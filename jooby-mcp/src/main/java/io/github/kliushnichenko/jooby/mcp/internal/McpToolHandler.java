package io.github.kliushnichenko.jooby.mcp.internal;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.kliushnichenko.jooby.mcp.JoobyMcpServer;
import io.modelcontextprotocol.spec.McpSchema;

import java.util.List;
import java.util.Map;

public class McpToolHandler {

    private final ObjectMapper objectMapper;

    public McpToolHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public McpSchema.CallToolResult handle(String toolName, JoobyMcpServer server, Map<String, Object> args) {
        try {
            if (!server.getTools().containsKey(toolName)) {
                throw new IllegalArgumentException("Tool '" + toolName + "' is not registered.");
            }

            Object result = server.invokeTool(toolName, args);

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
            return new McpSchema.CallToolResult(ex.getMessage(), true);
        }
    }
}
