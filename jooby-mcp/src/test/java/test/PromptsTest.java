package test;

import app.TestApp;
import io.jooby.test.JoobyTest;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.spec.McpClientTransport;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@JoobyTest(value = TestApp.class, port = 8099)
public class PromptsTest {

    private static final McpSyncClient mcpClient;

    static {
        McpClientTransport transport = HttpClientStreamableHttpTransport
                .builder("http://localhost:8099/mcp")
                .build();

        mcpClient = McpClient.sync(transport).build();
    }

    @Test
    void list_prompt_messages() {
        var request = new McpSchema.GetPromptRequest("list_prompt_messages", Map.of());
        var result = mcpClient.getPrompt(request);

        assertThat(result.messages())
                .usingRecursiveComparison()
                .ignoringCollectionOrder()
                .isEqualTo(
                        List.of(
                                new McpSchema.PromptMessage(
                                        McpSchema.Role.ASSISTANT,
                                        new McpSchema.TextContent("You are a concise summarization assistant.")
                                ),
                                new McpSchema.PromptMessage(
                                        McpSchema.Role.ASSISTANT,
                                        new McpSchema.TextContent("Summarize the following text: {{text}}")
                                )
                        )
                );
    }
}
