package app;

import io.github.kliushnichenko.jooby.mcp.annotation.Prompt;
import io.modelcontextprotocol.spec.McpSchema;

import java.util.List;

public class PromptsTestCases {

    @Prompt(name = "prompt_with_all_params_specified",
            title = "Summarization Prompt",
            description = "A prompt to summarize text.")
    public String prompt_with_all_params_specified() {
        return "Summary prompt";
    }

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
