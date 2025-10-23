package io.github.kliushnichenko.jooby.mcp;

import io.github.kliushnichenko.jooby.mcp.internal.ToolSpec;
import io.jooby.Jooby;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.spec.McpSchema;

import java.util.List;
import java.util.Map;

public interface JoobyMcpServer {

    String getServerKey();

    void init(Jooby app, McpJsonMapper mcpJsonMapper);

    Object invokeTool(String toolName, Map<String, Object> args);

    Object invokePrompt(String promptName, Map<String, Object> args);

    Object invokeCompletion(String identifier, String argumentName, String input);

    Object readResource(String uri);

    Map<String, ToolSpec> getTools();

    Map<String, McpSchema.Prompt> getPrompts();

    List<McpSchema.Resource> getResources();

    List<McpSchema.ResourceTemplate> getResourceTemplates();

    List<McpSchema.CompleteReference> getCompletions();
}
