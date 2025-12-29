package io.github.kliushnichenko.mcp.inspector;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.github.kliushnichenko.jooby.mcp.internal.McpServerConfig;
import io.jooby.*;
import io.jooby.exception.RegistryException;
import io.jooby.exception.StartupException;

import java.util.List;

import static io.github.kliushnichenko.jooby.mcp.internal.McpServerConfig.Transport.SSE;
import static io.github.kliushnichenko.jooby.mcp.internal.McpServerConfig.Transport.STREAMABLE_HTTP;

/**
 * MCP Inspector module for Jooby.
 *
 * <p>
 * The MCP Inspector module provides a web-based interface for inspecting and interacting with
 * local MCP server running on the same app. It serves a frontend application that allows users to
 * connect to MCP servers, view their capabilities, and test various protocol features.
 * </p>
 *
 * <h2>Usage</h2>
 *
 * <p>
 * Add the module to your application:
 * </p>
 *
 * <pre>{@code
 * {
 *   install(new McpInspectorModule());
 * }
 * }</pre>
 *
 * <h2>Configuration</h2>
 *
 * <p>
 * The module requires at least one MCP server to be configured in your Jooby application.
 * </p>
 *
 * <h2>Features</h2>
 *
 * <ul>
 *   <li>Serves a web-based MCP Inspector UI</li>
 *   <li>Automatically configures the inspector to connect to the local MCP server with respect to transport and endpoint</li>
 *   <li>Supports only direct connection and enables it automatically when the page loads</li>
 * </ul>
 *
 * @author kliushnichenko
 * @since 1.9.0
 */
public class McpInspectorModule implements Extension {

    private static final String DIST = "https://cdn.jsdelivr.net/npm/@modelcontextprotocol/inspector-client@0.18.0/dist";

    private static final String INDEX_HTML_TEMPLATE = """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <link rel="icon" type="image/svg+xml" href="%s/mcp.svg">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>MCP Inspector</title>
                <script type="module" crossorigin src="%s/assets/index-Dw52pmVD.js"></script>
                <link rel="stylesheet" crossorigin href="%s/assets/index-DoQUGvvr.css">
                <script>
                localStorage.setItem("lastConnectionType", "direct");
                </script>
            </head>
            <body>
                <div id="root" class="w-full"></div>
            </body>
            %s
            </html>
            """;

    private static final String AUTO_CONNECT_SCRIPT = """
            <script>
            const observer = new MutationObserver(() => {
              const btn = [...document.querySelectorAll('button')]
                .find(el => el.textContent.trim() === 'Connect');
              if (btn) {
                btn.click();
                observer.disconnect();
                console.log('Auto-connecting to MCP server...');
              }
            });
            
            observer.observe(document.body, { childList: true, subtree: true });
            </script>
            """;

    private static final String DEFAULT_ENDPOINT = "/mcp-inspector";

    private String inspectorEndpoint = DEFAULT_ENDPOINT;
    private boolean autoConnect = true;
    private McpServerConfig mcpSrvConfig;
    private String indexHtml;

    public McpInspectorModule path(@NonNull String inspectorEndpoint) {
        this.inspectorEndpoint = inspectorEndpoint;
        return this;
    }

    public McpInspectorModule autoConnect(boolean autoConnect) {
        this.autoConnect = autoConnect;
        return this;
    }

    @Override
    public void install(@NonNull Jooby app) {
        this.indexHtml = buildIndexHtml();
        this.mcpSrvConfig = resolveMcpServerConfig(app);

        app.get(inspectorEndpoint, ctx -> {
            if (ctx.query("MCP_PROXY_PORT").isMissing()) {
                return ctx.sendRedirect(inspectorEndpoint + "?MCP_PROXY_PORT=" + ctx.getPort());
            } else {
                return ctx.setResponseType(MediaType.html).render(this.indexHtml);
            }
        });

        app.get("/config", ctx -> {
            var location = resolveLocation(ctx);
            var configJson = buildConfigJson(mcpSrvConfig, location);
            return ctx.setResponseType(MediaType.json).render(configJson);
        });
    }

    private String buildIndexHtml() {
        var script = this.autoConnect ? AUTO_CONNECT_SCRIPT : "";
        return INDEX_HTML_TEMPLATE.formatted(DIST, DIST, DIST, script);
    }

    private String resolveLocation(Context ctx) {
        if (ctx.getPort() == 80) {
            return ctx.getScheme() + "://" + ctx.getHost();
        } else {
            return ctx.getScheme() + "://" + ctx.getHostAndPort();
        }
    }

    private static McpServerConfig resolveMcpServerConfig(Jooby app) {
        List<McpServerConfig> srvConfigs;
        try {
            srvConfigs = app.getServices().get(Reified.list(McpServerConfig.class));
        } catch (RegistryException ex) {
            throw new StartupException("MCP Inspector module requires at least one MCP server to be configured.");
        }

        return srvConfigs.get(0);
    }

    private String buildConfigJson(McpServerConfig config, String location) {
        var endpoint = resolveEndpoint(config);
        var transport = config.isSseTransport() ? SSE : STREAMABLE_HTTP;
        return """
                {
                  "defaultEnvironment": {
                  },
                  "defaultCommand": "",
                  "defaultArgs": "",
                  "defaultTransport": "%s",
                  "defaultServerUrl": "%s%s"
                }
                """.formatted(transport.getValue(), location, endpoint);
    }

    private String resolveEndpoint(McpServerConfig config) {
        if (config.isSseTransport()) {
            return config.getSseEndpoint();
        } else {
            return config.getMcpEndpoint();
        }
    }

    @Override
    public boolean lateinit() {
        return true;
    }
}
