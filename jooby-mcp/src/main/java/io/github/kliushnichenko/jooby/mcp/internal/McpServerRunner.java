package io.github.kliushnichenko.jooby.mcp.internal;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.kliushnichenko.jooby.mcp.JoobyMcpServer;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.json.jackson.JacksonMcpJsonMapper;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpServerTransportProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class McpServerRunner {

    private final static Logger log = LoggerFactory.getLogger(McpServerRunner.class);

    private final JoobyMcpServer joobyMcpServer;
    private final McpServerTransportProvider transportProvider;
    private final String serverName;
    private final String serverVersion;
    private final McpToolHandler toolHandler;
    private final McpResourceHandler resourceHandler;
    private final McpJsonMapper mcpJsonMapper;

    public McpServerRunner(JoobyMcpServer joobyMcpServer,
                           McpServerTransportProvider transportProvider,
                           String serverName,
                           String serverVersion,
                           ObjectMapper objectMapper) {
        this.joobyMcpServer = joobyMcpServer;
        this.transportProvider = transportProvider;
        this.serverName = serverName;
        this.serverVersion = serverVersion;
        this.mcpJsonMapper = new JacksonMcpJsonMapper(objectMapper);
        this.toolHandler = new McpToolHandler(objectMapper);
        this.resourceHandler = new McpResourceHandler(objectMapper);
    }

    public McpSyncServer run() {
        List<McpServerFeatures.SyncCompletionSpecification> completions = initCompletions();
        McpSyncServer mcpServer = McpServer.sync(transportProvider)
                .serverInfo(serverName, serverVersion)
                .capabilities(computeCapabilities())
                .completions(completions)
                .build();

        initTools(mcpServer);
        initPrompts(mcpServer);
        initResources(mcpServer);

        logMcpStart(mcpServer);
        return mcpServer;
    }

    private List<McpServerFeatures.SyncCompletionSpecification> initCompletions() {
        List<McpServerFeatures.SyncCompletionSpecification> completions = new ArrayList<>();
        for (McpSchema.CompleteReference ref : joobyMcpServer.getCompletions()) {
            var completion = new McpServerFeatures.SyncCompletionSpecification(
                    ref,
                    (exchange, request) -> McpCompletionHandler.handle(joobyMcpServer, request)
            );
            completions.add(completion);
        }
        return completions;
    }

    private void initTools(McpSyncServer mcpServer) {
        for (Map.Entry<String, ToolSpec> entry : joobyMcpServer.getTools().entrySet()) {
            ToolSpec toolSpec = entry.getValue();
            McpSchema.Tool tool = McpSchema.Tool.builder()
                    .name(toolSpec.getName())
                    .title(toolSpec.getTitle())
                    .description(toolSpec.getDescription())
                    .inputSchema(mcpJsonMapper, toolSpec.getInputSchema())
                    .build();

            var syncToolSpec = new McpServerFeatures.SyncToolSpecification.Builder()
                    .tool(tool)
                    .callHandler((exchange, callToolRequest) -> toolHandler.handle(callToolRequest, joobyMcpServer))
                    .build();

            mcpServer.addTool(syncToolSpec);
        }
    }

    private void initPrompts(McpSyncServer mcpServer) {
        for (Map.Entry<String, McpSchema.Prompt> entry : joobyMcpServer.getPrompts().entrySet()) {
            mcpServer.addPrompt(
                    new McpServerFeatures.SyncPromptSpecification(
                            entry.getValue(),
                            (exchange, request) -> McpPromptHandler.handle(joobyMcpServer, request)
                    )
            );
        }
    }

    private void initResources(McpSyncServer mcpServer) {
        for (McpSchema.Resource resource : joobyMcpServer.getResources()) {
            mcpServer.addResource(
                    new McpServerFeatures.SyncResourceSpecification(
                            resource,
                            (exchange, request) -> resourceHandler.handle(joobyMcpServer, request)
                    )
            );
        }
    }

    private McpSchema.ServerCapabilities computeCapabilities() {
        var builder = McpSchema.ServerCapabilities.builder();

        if (!joobyMcpServer.getTools().isEmpty()) {
            builder.tools(true);
        }

        if (!joobyMcpServer.getPrompts().isEmpty()) {
            builder.prompts(true);
        }

        if (!joobyMcpServer.getCompletions().isEmpty()) {
            builder.completions();
        }

        if (!joobyMcpServer.getResources().isEmpty()) {
            builder.resources(true, true);
        }

        return builder.build();
    }

    private void logMcpStart(McpSyncServer mcpServer) {
        log.info("""
                                                
                        MCP server started with:
                            name: {}
                            version: {}
                            capabilities: {}
                        """,
                mcpServer.getServerInfo().name(),
                mcpServer.getServerInfo().version(),
                mcpServer.getServerCapabilities());
    }
}
