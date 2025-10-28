package io.github.kliushnichenko.jooby.mcp.internal;

import io.github.kliushnichenko.jooby.mcp.JoobyMcpServer;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.spec.McpSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public class McpToolHandler {

    private static final Logger LOG = LoggerFactory.getLogger(McpToolHandler.class);

    private final McpJsonMapper mcpJsonMapper;

    public McpToolHandler(McpJsonMapper mcpJsonMapper) {
        this.mcpJsonMapper = mcpJsonMapper;
    }

    public McpSchema.CallToolResult handle(McpSchema.CallToolRequest request, JoobyMcpServer server) {
        String toolName = request.name();
        try {
            ToolSpec toolSpec = server.getTools().get(toolName);

            if (toolSpec == null) {
                throw new IllegalArgumentException("Tool not found: " + toolName);
            }

            verifyRequiredArguments(request.arguments(), toolSpec.getRequiredArguments());

            Object result = server.invokeTool(toolName, request.arguments());

            if (result == null) {
                return new McpSchema.CallToolResult("null", false);
            } else if (result instanceof McpSchema.CallToolResult callToolResult) {
                return callToolResult;
            } else if (result instanceof String str) {
                return new McpSchema.CallToolResult(str, false);
            } else if (result instanceof McpSchema.Content content) {
                return McpSchema.CallToolResult.builder().content(List.of(content)).isError(false).build();
            } else {
                var resultStr = mcpJsonMapper.writeValueAsString(result);
                return new McpSchema.CallToolResult(resultStr, false);
            }
        } catch (Exception ex) {
            LOG.error("Error invoking tool '{}':", toolName, ex);
            return new McpSchema.CallToolResult(ex.getMessage(), true);
        }
    }

    private void verifyRequiredArguments(Map<String, Object> actualArguments, List<String> requiredArguments) {
        for (String requiredArg : requiredArguments) {
            var argument = actualArguments.get(requiredArg);
            if (argument == null) {
                throw new IllegalArgumentException("Missing required argument: " + requiredArg);
            }

            if (argument instanceof String str && str.isEmpty()) {
                throw new IllegalArgumentException("Required argument is empty: " + requiredArg);
            }
        }
    }
}
