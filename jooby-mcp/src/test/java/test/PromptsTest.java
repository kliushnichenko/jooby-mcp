package test;

import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class PromptsTest extends BaseTest {

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
