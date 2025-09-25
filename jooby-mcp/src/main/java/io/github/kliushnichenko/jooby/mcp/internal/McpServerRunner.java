package io.github.kliushnichenko.jooby.mcp.internal;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.kliushnichenko.jooby.mcp.JoobyMcpServer;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpServerTransportProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class McpServerRunner {

    private final static Logger log = LoggerFactory.getLogger(McpServerRunner.class);

    private final JoobyMcpServer joobyMcpServer;
    private final McpServerTransportProvider transportProvider;
    private final String serverName;
    private final String serverVersion;
    private final McpToolHandler toolHandler;
    private final McpPromptHandler promptHandler;

    public McpServerRunner(JoobyMcpServer joobyMcpServer,
                           McpServerTransportProvider transportProvider,
                           String serverName,
                           String serverVersion,
                           ObjectMapper objectMapper) {
        this.joobyMcpServer = joobyMcpServer;
        this.transportProvider = transportProvider;
        this.serverName = serverName;
        this.serverVersion = serverVersion;
        this.toolHandler = new McpToolHandler(objectMapper);
        this.promptHandler = new McpPromptHandler();
    }

    public McpSyncServer run() {
        McpSyncServer mcpServer = McpServer.sync(transportProvider)
                .serverInfo(serverName, serverVersion)
                .capabilities(computeCapabilities())
                .build();

        initTools(mcpServer);
        initPrompts(mcpServer);

        logMcpStart(mcpServer);
        return mcpServer;
    }

    private void initTools(McpSyncServer mcpServer) {
        for (Map.Entry<String, McpSchema.Tool> entry : joobyMcpServer.getTools().entrySet()) {
            var toolSpec = new McpServerFeatures.SyncToolSpecification.Builder()
                    .tool(entry.getValue())
                    .callHandler((exchange, callToolRequest) -> toolHandler.handle(callToolRequest, joobyMcpServer))
                    .build();

            mcpServer.addTool(toolSpec);
        }
    }

    private void initPrompts(McpSyncServer mcpServer) {
        for (Map.Entry<String, McpSchema.Prompt> entry : joobyMcpServer.getPrompts().entrySet()) {
            mcpServer.addPrompt(
                    new McpServerFeatures.SyncPromptSpecification(
                            entry.getValue(),
                            (mcpSyncServerExchange, request) -> promptHandler.handle(joobyMcpServer, request)
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
