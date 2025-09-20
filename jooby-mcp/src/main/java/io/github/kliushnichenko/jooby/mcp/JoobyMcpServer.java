package io.github.kliushnichenko.jooby.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jooby.Jooby;
import io.modelcontextprotocol.spec.McpSchema;

import java.util.Map;

public interface JoobyMcpServer {

    String getServerKey();

    void init(Jooby app, ObjectMapper objectMapper);

    Object invokeTool(String toolName, Map<String, Object> args);

    Object invokePrompt(String toolName, Map<String, Object> args);

    Map<String, McpSchema.Tool> getTools();

    Map<String, McpSchema.Prompt> getPrompts();
}
