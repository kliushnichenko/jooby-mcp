package app;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.kliushnichenko.jooby.mcp.internal.McpServerConfig;
import io.github.kliushnichenko.jooby.mcp.transport.JoobyStreamableServerTransportProvider;
import io.jooby.Jooby;
import io.jooby.jackson.JacksonModule;
import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.json.jackson.JacksonMcpJsonMapper;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;

public class StreamableTransportApp extends Jooby {

    private final ObjectMapper objectMapper = new ObjectMapper();

    {
        install(new JacksonModule(objectMapper));
        runMcpServer();
    }

    private void runMcpServer() {
        var serverConfig = new McpServerConfig("streamable-mcp-server", "1.0.0");
        var transportProvider = new JoobyStreamableServerTransportProvider(
                this,
                new JacksonMcpJsonMapper(objectMapper),
                serverConfig,
                request -> McpTransportContext.EMPTY
        );

        McpServerFeatures.SyncToolSpecification toolSpec =
                McpServerFeatures.SyncToolSpecification.builder()
                        .tool(McpSchema.Tool.builder()
                                .name("echo_tool")
                                        .description("A tool that echoes back the input it receives.")
//                                .inputSchema(new McpSchema.JsonSchema(""))
                                .build()
                        )
                        .callHandler((exchange, request) -> new McpSchema.CallToolResult(request.arguments().get("input").toString(), false))
                        .build();

        McpServer.sync(transportProvider)
                .serverInfo(serverConfig.getName(), serverConfig.getVersion())
                .capabilities(McpSchema.ServerCapabilities.builder()
                        .tools(true)
                        .build()
                )
                .tools(toolSpec)
                .build();
    }

    public static void main(String[] args) {
        runApp(args, StreamableTransportApp::new);
    }
}
