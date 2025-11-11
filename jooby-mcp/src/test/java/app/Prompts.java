package app;

import io.github.kliushnichenko.jooby.mcp.annotation.Prompt;
import io.modelcontextprotocol.spec.McpSchema;

import java.util.List;

public class Prompts {

    @Prompt(name = "list_prompt_messages")
    public List<McpSchema.PromptMessage> listPromptMessages() {
        return List.of(
                new McpSchema.PromptMessage(
                        McpSchema.Role.ASSISTANT,
                        new McpSchema.TextContent("You are a concise summarization assistant.")
                ),
                new McpSchema.PromptMessage(
                        McpSchema.Role.ASSISTANT,
                        new McpSchema.TextContent("Summarize the following text: {{text}}")
                )
        );
    }
}
