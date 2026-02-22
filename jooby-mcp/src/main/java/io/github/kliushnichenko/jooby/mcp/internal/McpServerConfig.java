package io.github.kliushnichenko.jooby.mcp.internal;

import com.typesafe.config.Config;
import io.jooby.exception.StartupException;
import lombok.Getter;
import lombok.Setter;

/**
 * @author kliushnichenko
 */
@Getter
@Setter
public class McpServerConfig {
    public static final String DEFAULT_SSE_ENDPOINT = "/mcp/sse";
    public static final String DEFAULT_MESSAGE_ENDPOINT = "/mcp/message";
    public static final String DEFAULT_MCP_ENDPOINT = "/mcp";

    private String name;
    private String version;
    private Transport transport;
    private String sseEndpoint;
    private String messageEndpoint;
    private String mcpEndpoint = DEFAULT_MCP_ENDPOINT;
    private boolean disallowDelete;
    private Integer keepAliveInterval;
    private String instructions;

    public McpServerConfig(String name, String version) {
        this.name = name;
        this.version = version;
    }

    @Getter
    public enum Transport {
        SSE("sse"),
        STREAMABLE_HTTP("streamable-http"),
        STATELESS_STREAMABLE_HTTP("stateless-streamable-http");

        private final String value;

        Transport(String value) {
            this.value = value;
        }

        public static Transport of(String value) {
            for (Transport transport : values()) {
                if (transport.value.equalsIgnoreCase(value)) {
                    return transport;
                }
            }
            throw new IllegalArgumentException("Unknown transport value: " + value);
        }
    }

    public static McpServerConfig fromConfig(Config config) {
        var srvConfig = new McpServerConfig(
                resolveRequiredParam(config, "name"),
                resolveRequiredParam(config, "version")
        );

        if (config.hasPath("transport")) {
            Transport transport = Transport.of(config.getString("transport"));
            srvConfig.setTransport(transport);
        } else {
            srvConfig.setTransport(Transport.STREAMABLE_HTTP);
        }

        srvConfig.setSseEndpoint(getStrProp("sseEndpoint", DEFAULT_SSE_ENDPOINT, config));
        srvConfig.setMessageEndpoint(getStrProp("messageEndpoint", DEFAULT_MESSAGE_ENDPOINT, config));
        srvConfig.setMcpEndpoint(getStrProp("mcpEndpoint", DEFAULT_MCP_ENDPOINT, config));
        srvConfig.setInstructions(getStrProp("instructions", null, config));
        srvConfig.setDisallowDelete(getBoolProp("disallowDelete", false, config));
        srvConfig.setKeepAliveInterval(getIntProp("keepAliveInterval", null, config));

        return srvConfig;
    }

    public boolean isSseTransport() {
        return this.transport == Transport.SSE;
    }

    private static String resolveRequiredParam(Config config, String configPath) {
        if (!config.hasPath(configPath)) {
            throw new StartupException("Missing required config path: " + configPath);
        }
        return config.getString(configPath);
    }

    private static String getStrProp(String propName, String defaultValue, Config config) {
        if (config.hasPath(propName)) {
            return config.getString(propName);
        } else {
            return defaultValue;
        }
    }

    private static boolean getBoolProp(String propName, boolean defaultValue, Config config) {
        if (config.hasPath(propName)) {
            return config.getBoolean(propName);
        } else {
            return defaultValue;
        }
    }

    private static Integer getIntProp(String propName, Integer defaultValue, Config config) {
        if (config.hasPath(propName)) {
            return config.getInt(propName);
        } else {
            return defaultValue;
        }
    }
}
