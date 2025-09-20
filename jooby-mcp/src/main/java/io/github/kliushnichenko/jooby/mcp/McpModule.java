package io.github.kliushnichenko.jooby.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.typesafe.config.Config;
import edu.umd.cs.findbugs.annotations.NonNull;
import io.github.kliushnichenko.jooby.mcp.internal.McpServerRunner;
import io.jooby.Extension;
import io.jooby.Jooby;
import io.jooby.ServiceKey;
import io.jooby.exception.StartupException;
import io.modelcontextprotocol.server.McpSyncServer;

import java.util.ArrayList;
import java.util.List;

/**
 * MCP (Model Context Protocol) module for Jooby.
 *
 * <p>
 * The MCP module provides integration with the Model Context Protocol server, enabling standardized
 * communication between clients and servers. It allows applications to:
 * </p>
 *
 * <ul>
 *   <li>Expose server capabilities as tools, resources, and prompts</li>
 *   <li>Handle client connections and sessions via SSE</li>
 *   <li>Process protocol messages and events</li>
 *   <li>Manage server capabilities and tool specifications</li>
 * </ul>
 *
 * <h2>Usage</h2>
 *
 * <p>
 * Add the module to your application:
 * </p>
 *
 * <pre>{@code
 * {
 *   install(new JacksonModule());
 *   install(new McpModule(new DefaultMcpServer()));
 * }
 * }</pre>
 *
 * <h2>Configuration</h2>
 *
 * <p>
 * The module requires the following configuration in your application.conf:
 * </p>
 *
 * <pre>{@code
 * mcp.default {
 *     name: "my-awesome-mcp-server"     # Required
 *     version: "0.0.1"                  # Required
 *     sseEndpoint: "/mcp/sse"           # Optional (default: /mcp/sse)
 *     messageEndpoint: "/mcp/message"   # Optional (default: /mcp/message)
 * }
 * }</pre>
 *
 * <h2>Features</h2>
 *
 * <ul>
 *   <li>MCP server implementation with SSE transport</li>
 *   <li>Tools Auto-discovery at build time</li>
 *   <li>Server capabilities configuration</li>
 *   <li>Configurable endpoints</li>
 *   <li>Multiple servers support</li>
 * </ul>
 *
 * <h2>Multiple servers</h2>
 *
 * <p>
 * To run multiple MCP server instances in the same application, use a @McpServer("calculator") annotation:
 * </p>
 *
 * <pre>{@code
 * {
 *
 *   install(new JacksonModule());
 *   install(new McpModule(new DefaultMcpServer()).server(new CalculatorMcpServer());
 * }
 * }</pre>
 *
 * <p>
 * Each instance requires its own configuration block:
 * </p>
 *
 * <pre>{@code
 * mcp {
 *  default {
 *    name: "default-mcp-server"
 *    version: "1.0.0"
 *    sseEndpoint: "/mcp/sse"
 *    messageEndpoint: "/mcp/message"
 *  }
 *  calculator {
 *    name: "calculator-mcp-server"
 *    version: "1.0.0"
 *    sseEndpoint: "/mcp/calculator/sse"
 *    messageEndpoint: "/mcp/calculator/message"
 *  }
 * }
 *
 * }</pre>
 *
 * @author kliushnichenko
 * @since 1.0.0
 */
public class McpModule implements Extension {

    private static final String MODULE_CONFIG_PREFIX = "mcp";
    private static final String SERVER_NAME_KEY = "name";
    private static final String VERSION_KEY = "version";

    private ObjectMapper objectMapper = new ObjectMapper();
    private final List<JoobyMcpServer> mcpServers = new ArrayList<>();

    public McpModule(JoobyMcpServer joobyMcpServer) {
        mcpServers.add(joobyMcpServer);
    }

    public McpModule server(@NonNull JoobyMcpServer joobyMcpServer) {
        mcpServers.add(joobyMcpServer);
        return this;
    }

    @Override
    public void install(@NonNull Jooby app) {
        Config config = app.getConfig();
        if (!config.hasPath(MODULE_CONFIG_PREFIX)) {
            throw new StartupException("Missing required config path: " + MODULE_CONFIG_PREFIX);
        }

        for (JoobyMcpServer joobyMcpServer : mcpServers) {
            Config serverConfig = resolveServerConfig(config, joobyMcpServer.getServerKey());
            joobyMcpServer.init(app, this.objectMapper);
            JoobySseTransportProvider transportProvider = new JoobySseTransportProvider(objectMapper, app, serverConfig);

            var runner = new McpServerRunner(
                    joobyMcpServer,
                    transportProvider,
                    resolveRequiredParam(serverConfig, SERVER_NAME_KEY),
                    resolveRequiredParam(serverConfig, VERSION_KEY),
                    objectMapper
            );

            McpSyncServer mcpServer = runner.run();
            addToJoobyRegistry(app, joobyMcpServer, mcpServer);
            app.onStop(mcpServer::close);
        }
    }

    private void addToJoobyRegistry(Jooby app, JoobyMcpServer joobyMcpServer, McpSyncServer mcpServer) {
        if (this.mcpServers.size() == 1) {
            app.getServices().put(McpSyncServer.class, mcpServer);
        } else {
            var serviceKey = ServiceKey.key(McpSyncServer.class, joobyMcpServer.getServerKey());
            app.getServices().put(serviceKey, mcpServer);
        }
    }

    private Config resolveServerConfig(Config config, String serverKey) {
        String path = MODULE_CONFIG_PREFIX + "." + serverKey;
        if (!config.hasPath(path)) {
            throw new StartupException(String.format("Missing required config path: %s", path));
        }
        return config.getConfig(path);
    }

    private String resolveRequiredParam(Config config, String configPath) {
        if (!config.hasPath(configPath)) {
            throw new StartupException("Missing required config path: " + configPath);
        }
        return config.getString(configPath);
    }

    public McpModule objectMapper(ObjectMapper mapper) {
        this.objectMapper = mapper;
        return this;
    }

    @Override
    public boolean lateinit() {
        return true;
    }
}
