package io.github.kliushnichenko.jooby.mcp;

import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;

public interface McpSyncPrompt {

    McpSchema.Prompt specification();

    McpSchema.GetPromptResult handler(McpSyncServerExchange exchange, McpSchema.GetPromptRequest request);
}
