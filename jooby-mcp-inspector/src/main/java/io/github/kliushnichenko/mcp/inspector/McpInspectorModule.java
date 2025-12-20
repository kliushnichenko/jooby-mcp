package io.github.kliushnichenko.mcp.inspector;

import com.typesafe.config.Config;
import edu.umd.cs.findbugs.annotations.NonNull;
import io.github.kliushnichenko.jooby.mcp.internal.McpServerConfig;
import io.jooby.*;
import io.jooby.exception.RegistryException;
import io.jooby.exception.StartupException;

import java.util.List;

public class McpInspectorModule implements Extension {

    private static final String DIST = "https://cdn.jsdelivr.net/npm/@modelcontextprotocol/inspector-client@0.18.0/dist";

    private static final String indexHtml = """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <link rel="icon" type="image/svg+xml" href="%s/mcp.svg">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>MCP Inspector</title>
                <script type="module" crossorigin src="%s/assets/index-Dw52pmVD.js"></script>
                <link rel="stylesheet" crossorigin href="%s/assets/index-DoQUGvvr.css">
                <script>localStorage.setItem("lastConnectionType", "direct");</script>
            </head>
            <body>
                <div id="root" class="w-full"></div>
            </body>
            </html>
            """.formatted(DIST, DIST, DIST);

    private static final String DEFAULT_ENDPOINT = "/mcp-inspector";

    private final String inspectorEndpoint;

    public McpInspectorModule(String inspectorEndpoint) {
        this.inspectorEndpoint = inspectorEndpoint;
    }

    public McpInspectorModule() {
        this.inspectorEndpoint = DEFAULT_ENDPOINT;
    }

    @Override
    public void install(@NonNull Jooby app) {
        McpServerConfig mcpSrvConfig = resolveMcpServerConfig(app);

        var port = resolvePort(app.getConfig());
        var configJson = buildConfigJson(mcpSrvConfig, port);

        app.get(inspectorEndpoint, ctx -> {
            if (ctx.query("MCP_PROXY_PORT").isMissing()) {
                return ctx.sendRedirect(inspectorEndpoint + "?MCP_PROXY_PORT=" + port);
            } else {
                return ctx.setResponseType(MediaType.html).render(indexHtml);
            }
        });

        app.get("/config", ctx -> ctx.setResponseType(MediaType.json).render(configJson));
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

    private String buildConfigJson(McpServerConfig config, int port) {
        var endpoint = McpServerConfig.Transport.STREAMABLE_HTTP == config.getTransport()
                ? config.getMcpEndpoint()
                : config.getSseEndpoint();

        return """
                {
                  "defaultEnvironment": {
                  },
                  "defaultCommand": "",
                  "defaultArgs": "",
                  "defaultTransport": "%s",
                  "defaultServerUrl": "http://localhost:%d%s"
                }
                """.formatted(config.getTransport().getValue(), port, endpoint);
    }

    private int resolvePort(Config config) {
        if (config.hasPath("server.port")) {
            return config.getInt("server.port");
        } else {
            return ServerOptions.SERVER_PORT;
        }
    }

    @Override
    public boolean lateinit() {
        return true;
    }
}
