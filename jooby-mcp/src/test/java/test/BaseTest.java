package test;

import ext.JoobyTestConfig;
import ext.JoobyTestSingleton;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.spec.McpClientTransport;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * @author kliushnichenko
 */
public abstract class BaseTest {

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
}
