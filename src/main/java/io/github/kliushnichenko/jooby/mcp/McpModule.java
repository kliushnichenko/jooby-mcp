package io.github.kliushnichenko.jooby.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.typesafe.config.Config;
import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.Extension;
import io.jooby.Jooby;
import io.jooby.ServiceKey;
import io.jooby.exception.StartupException;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.spec.McpSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Objects;
import java.util.Set;

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
 *   install(new McpModule()
 *     .tools(Set.of(MyTool.class, AnotherTool.class))
 *   );
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
 * mcp {
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
 *   <li>Tool registration and management for executing operations</li>
 *   <li>Server capabilities configuration</li>
 *   <li>Configurable endpoints</li>
 *   <li>Multiple instance support via config prefix</li>
 * </ul>
 *
 * <h2>Multiple Instances</h2>
 *
 * <p>
 * To run multiple MCP server instances in the same application, use a custom prefix:
 * </p>
 *
 * <pre>{@code
 * {
 *
 *   install(new McpModule("mcp1")
 *     .tools(Set.of(FirstTool.class))
 *   );
 *   install(new McpModule("mcp2")
 *     .tools(Set.of(SecondTool.class))
 *   );
 * }
 * }</pre>
 *
 * <p>
 * Each instance requires its own configuration block:
 * </p>
 *
 * <pre>{@code
 * mcp1 {
 *     name: "first-server"
 *     version: "1.0.0"
 *     sseEndpoint: "/mcp1/sse"
 *     messageEndpoint: "/mcp1/message"
 * }
 *
 * mcp2 {
 *     name: "second-server"
 *     version: "1.0.0"
 *     sseEndpoint: "/mcp2/sse"
 *     messageEndpoint: "/mcp2/message"
 * }
 * }</pre>
 *
 * @author kliushnichenko
 * @since 0.1.0
 */

public class McpModule implements Extension {

    private static final Logger log = LoggerFactory.getLogger(McpModule.class);

    private static final String DEFAULT_CONFIG_PREFIX = "mcp";
    private static final String SERVER_NAME_KEY = "name";
    private static final String VERSION_KEY = "version";

    private Jooby app;
    private Config moduleConfig;
    private ObjectMapper objectMapper = new ObjectMapper();
    private Set<Class<? extends McpSyncTool>> tools;
    private Set<Class<? extends McpSyncPrompt>> prompts;
    private McpSyncServer mcpServer;

    private final String prefix;

    public McpModule() {
        this.prefix = DEFAULT_CONFIG_PREFIX;
    }

    public McpModule(String prefix) {
        Objects.requireNonNull(prefix);
        this.prefix = prefix;
    }

    @Override
    public void install(@NonNull Jooby app) {
        this.app = app;
        this.moduleConfig = resolveModuleConfig(app.getConfig(), prefix);

        JoobySseTransportProvider transportProvider = new JoobySseTransportProvider(objectMapper, app, moduleConfig);
        this.mcpServer = McpServer.sync(transportProvider)
                .serverInfo(resolveRequiredParam(SERVER_NAME_KEY), resolveRequiredParam(VERSION_KEY))
                .capabilities(computeCapabilities())
                .build();

        initTools();
        initPrompts();

        if (DEFAULT_CONFIG_PREFIX.equals(prefix)) {
            app.getServices().put(McpSyncServer.class, this.mcpServer);
        } else {
            app.getServices().put(ServiceKey.key(McpSyncServer.class, prefix), this.mcpServer);
        }

        app.onStop(() -> this.mcpServer.close());
        logMcpStart();
    }

    private void initTools() {
        if (isEmpty(tools)) {
            return;
        }

        for (Class<? extends McpSyncTool> toolClass : tools) {
            McpSyncTool tool = app.require(toolClass);
            this.mcpServer.addTool(new McpServerFeatures.SyncToolSpecification(
                    tool.specification(),
                    tool::handler
            ));
        }
    }

    private void initPrompts() {
        if (isEmpty(prompts)) {
            return;
        }

        for (Class<? extends McpSyncPrompt> promptClass : prompts) {
            McpSyncPrompt prompt = app.require(promptClass);
            this.mcpServer.addPrompt(new McpServerFeatures.SyncPromptSpecification(
                    prompt.specification(),
                    prompt::handler
            ));
        }
    }

    private McpSchema.ServerCapabilities computeCapabilities() {
        var builder = McpSchema.ServerCapabilities.builder();

        if (!isEmpty(tools)) {
            builder.tools(true);
        }

        if (!isEmpty(prompts)) {
            builder.prompts(true);
        }

        return builder.build();
    }

    private Config resolveModuleConfig(Config config, String prefix) {
        if (!config.hasPath(prefix)) {
            throw new StartupException(String.format("McpModule config is missing for prefix: %s", prefix));
        }
        return config.getConfig(prefix);
    }

    private void logMcpStart() {
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

    private String resolveRequiredParam(String configPath) {
        if (!moduleConfig.hasPath(configPath)) {
            throw new StartupException("Missing required config path: " + configPath);
        }
        return moduleConfig.getString(configPath);
    }

    public McpModule tools(Set<Class<? extends McpSyncTool>> tools) {
        this.tools = tools;
        return this;
    }

    public McpModule prompts(Set<Class<? extends McpSyncPrompt>> prompts) {
        this.prompts = prompts;
        return this;
    }

    public McpModule objectMapper(ObjectMapper mapper) {
        this.objectMapper = mapper;
        return this;
    }

    public McpSyncServer mcpServer() {
        return mcpServer;
    }

    @Override
    public boolean lateinit() {
        return true;
    }

    private boolean isEmpty(Collection collection) {
        return collection == null || collection.isEmpty();
    }
}
