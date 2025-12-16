package app;

import io.github.kliushnichenko.jooby.mcp.annotation.Tool;
import io.modelcontextprotocol.spec.McpSchema;

/**
 * @author kliushnichenko
 */
public class ToolsStructuredContentTestCases {

    @Tool(name = "test_tool_structured_content_from_return_type")
    public Pet structuredContentByDefault() {
        return new Pet("Buddy");
    }

    @Tool(name = "test_tool_without_output_schema")
    public McpSchema.CallToolResult noOutputSchema() {
        return McpSchema.CallToolResult.builder()
                .addTextContent("Just a text")
                .build();
    }
}
