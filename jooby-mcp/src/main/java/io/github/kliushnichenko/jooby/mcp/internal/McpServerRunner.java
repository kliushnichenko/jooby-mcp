package io.github.kliushnichenko.jooby.mcp.internal;

import io.github.kliushnichenko.jooby.mcp.JoobyMcpServer;
import io.github.kliushnichenko.jooby.mcp.transport.JoobySseTransportProvider;
import io.github.kliushnichenko.jooby.mcp.transport.JoobyStreamableServerTransportProvider;
import io.jooby.Jooby;
import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.spec.McpSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class McpServerRunner {

    private final static Logger log = LoggerFactory.getLogger(McpServerRunner.class);

    private final Jooby app;
    private final JoobyMcpServer joobyMcpServer;
    private final McpServerConfig serverConfig;
    private final McpToolHandler toolHandler;
    private final McpResourceHandler resourceHandler;
    private final McpResourceTemplateHandler resourceTemplateHandler;
    private final McpJsonMapper mcpJsonMapper;

    public McpServerRunner(Jooby app,
                           JoobyMcpServer joobyMcpServer,
                           McpServerConfig serverConfig,
                           McpJsonMapper mcpJsonMapper) {
        this.app = app;
        this.joobyMcpServer = joobyMcpServer;
        this.serverConfig = serverConfig;
        this.mcpJsonMapper = mcpJsonMapper;
        this.toolHandler = new McpToolHandler(mcpJsonMapper);
        this.resourceHandler = new McpResourceHandler(mcpJsonMapper);
        this.resourceTemplateHandler = new McpResourceTemplateHandler(mcpJsonMapper);
    }

    public McpSyncServer run() {
        McpSyncServer mcpServer = initMcpServer();

        initTools(mcpServer);
        initPrompts(mcpServer);
        initResources(mcpServer);
        initResourceTemplates(mcpServer);

        logMcpStart(mcpServer);
        return mcpServer;
    }

    private McpSyncServer initMcpServer() {
        List<McpServerFeatures.SyncCompletionSpecification> completions = initCompletions();

        if (McpServerConfig.Transport.SSE == serverConfig.getTransport()) {
            var transportProvider = new JoobySseTransportProvider(app, serverConfig, mcpJsonMapper);
            return McpServer.sync(transportProvider)
                    .serverInfo(serverConfig.getName(), serverConfig.getVersion())
                    .capabilities(computeCapabilities())
                    .completions(completions)
                    .build();
        } else if (McpServerConfig.Transport.STREAMABLE_HTTP == serverConfig.getTransport()) {
            var transportProvider = new JoobyStreamableServerTransportProvider(
                    app,
                    mcpJsonMapper,
                    serverConfig,
                    request -> McpTransportContext.EMPTY
            );

            return McpServer.sync(transportProvider)
                    .serverInfo(serverConfig.getName(), serverConfig.getVersion())
                    .capabilities(computeCapabilities())
                    .completions(completions)
                    .build();
        } else {
            throw new IllegalStateException("Unsupported transport: " + serverConfig.getTransport());
        }
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

    private void initResourceTemplates(McpSyncServer mcpServer) {
        for (McpSchema.ResourceTemplate template : joobyMcpServer.getResourceTemplates()) {
            var syncTemplateSpec = new McpServerFeatures.SyncResourceTemplateSpecification(
                    template,
                    (exchange, request) -> resourceTemplateHandler.handle(joobyMcpServer, template, request)
            );
            mcpServer.addResourceTemplate(syncTemplateSpec);
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
                            transport: {}
                            keepAliveInterval: {}
                            disallowDelete: {}
                            capabilities: {}
                        """,
                mcpServer.getServerInfo().name(),
                mcpServer.getServerInfo().version(),
                serverConfig.getTransport().getValue(),
                serverConfig.getKeepAliveInterval() == null ? "N/A" : serverConfig.getKeepAliveInterval() + " s",
                serverConfig.isDisallowDelete(),
                mcpServer.getServerCapabilities());
    }
}
