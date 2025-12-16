package test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import ext.JoobyTestConfig;
import ext.JoobyTestSingleton;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.spec.McpClientTransport;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * @author kliushnichenko
 */
public abstract class BaseTest {

    protected static final JsonMapper JSON_MAPPER = new JsonMapper();
    private static final int PORT = 8099;
    protected static McpSyncClient mcpClient;

    @RegisterExtension
    protected static final JoobyTestSingleton jooby = JoobyTestSingleton.getExtension(
            new JoobyTestConfig(app.TestApp.class, PORT)
    );

    static {
        McpClientTransport transport = HttpClientStreamableHttpTransport
                .builder(String.format("http://localhost:%d/mcp", PORT))
                .build();

        mcpClient = McpClient.sync(transport).build();
    }

    protected McpSchema.Tool findTool(String name) {
        return mcpClient.listTools().tools().stream()
                .filter(tool -> tool.name().equals(name))
                .findFirst()
                .orElseThrow();
    }

    protected McpSchema.Prompt findPrompt(String name) {
        return mcpClient.listPrompts().prompts().stream()
                .filter(prompt -> prompt.name().equals(name))
                .findFirst()
                .orElseThrow();
    }

    protected String jsonStringify(Object value) {
        try {
            return JSON_MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
