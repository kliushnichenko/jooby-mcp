package io.github.kliushnichenko.jooby.mcp;

import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;

import java.util.Map;

public interface McpSyncTool {

    McpSchema.Tool specification();

    McpSchema.CallToolResult handler(McpSyncServerExchange exchange, Map<String, Object> args);
}
