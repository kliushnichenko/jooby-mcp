package io.github.kliushnichenko.jooby.mcp.internal;

import io.github.kliushnichenko.jooby.mcp.JoobyMcpServer;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.spec.McpError;
import io.modelcontextprotocol.spec.McpSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

import static io.modelcontextprotocol.spec.McpSchema.ErrorCodes.INTERNAL_ERROR;

class McpResourceHandler {

    private static final Logger LOG = LoggerFactory.getLogger(McpResourceHandler.class);

    private final McpJsonMapper mcpJsonMapper;

    public McpResourceHandler(McpJsonMapper mcpJsonMapper) {
        this.mcpJsonMapper = mcpJsonMapper;
    }

    public McpSchema.ReadResourceResult handle(JoobyMcpServer server, McpSchema.ReadResourceRequest request) {
        var uri = request.uri();

        try {
            Object result = server.readResource(uri);
            return toResourceResult(result, uri);
        } catch (Exception ex) {
            LOG.error("Error reading resource by URI '{}': {}", uri, ex.getMessage(), ex);
            throw new McpError(new McpSchema.JSONRPCResponse.JSONRPCError(
                    INTERNAL_ERROR,
                    ex.getMessage(),
                    null
            ));
        }
    }

    private McpSchema.ReadResourceResult toResourceResult(Object result, String uri) throws RuntimeException, IOException {
        if (result == null) {
            return new McpSchema.ReadResourceResult(List.of());
        } else if (result instanceof McpSchema.ReadResourceResult resourceResult) {
            return resourceResult;
        } else if (result instanceof McpSchema.ResourceContents resourceContents) {
            return new McpSchema.ReadResourceResult(List.of(resourceContents));
        } else if (result instanceof List<?> contents) {
            if (contents.isEmpty()) {
                return new McpSchema.ReadResourceResult(List.of());
            } else {
                var item = contents.iterator().next();
                if (item instanceof McpSchema.ResourceContents) {
                    //noinspection unchecked
                    return new McpSchema.ReadResourceResult((List<McpSchema.ResourceContents>) contents);
                } else {
                    return toJsonResult(result, uri);
                }
            }
        } else {
            return toJsonResult(result, uri);
        }
    }

    private McpSchema.ReadResourceResult toJsonResult(Object result, String uri) throws IOException {
        var resultStr = mcpJsonMapper.writeValueAsString(result);
        var content = new McpSchema.TextResourceContents(uri, "application/json", resultStr);
        return new McpSchema.ReadResourceResult(List.of(content));
    }
}
