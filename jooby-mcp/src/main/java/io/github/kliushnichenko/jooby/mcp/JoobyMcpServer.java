package io.github.kliushnichenko.jooby.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.kliushnichenko.jooby.mcp.internal.ToolSpec;
import io.jooby.Jooby;
import io.modelcontextprotocol.spec.McpSchema;

import java.util.List;
import java.util.Map;

public interface JoobyMcpServer {

    String getServerKey();

    void init(Jooby app, ObjectMapper objectMapper);

    Object invokeTool(String toolName, Map<String, Object> args);

    Object invokePrompt(String promptName, Map<String, Object> args);

    Object invokeCompletion(String identifier, String argumentName, String input);

    Map<String, ToolSpec> getTools();

    Map<String, McpSchema.Prompt> getPrompts();

    List<McpSchema.CompleteReference> getCompletions();
}
