package io.github.kliushnichenko.jooby.mcp;

import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;

public interface McpSyncResource {

    McpSchema.Resource specification();

    McpSchema.ReadResourceResult handler(McpSyncServerExchange exchange, McpSchema.ReadResourceRequest request);
}
