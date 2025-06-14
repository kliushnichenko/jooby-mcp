package com.github.kliushnichenko.jooby.mcp;

import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;

import java.util.Map;

public interface McpSyncTool {

    McpSchema.Tool specification();

    McpSchema.CallToolResult call(McpSyncServerExchange exchange, Map<String, Object> args);
}
