package test;

import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

/**
 * @author kliushnichenko
 */
public class ToolsStructuredContentTest extends BaseTest {

    @Test
    void testTool_returnTypeIsPet_structuredContentShouldReflectPet() {
        var request = new McpSchema.CallToolRequest("test_tool_structured_content_from_return_type", null);
        var result = mcpClient.callTool(request);

        var expectedContent = """
                {"name":"Buddy"}""";

        // https://github.com/modelcontextprotocol/java-sdk/commit/8a2f97f1b1995880c492159dd1b4c50266d0b579#diff-1580e5959202a3bd57eef693a4480dc8b3255681c3fa5002bbec3f1a7bbc54f2R372-R376
        // For backwards compatibility, a tool that returns structured
        // content SHOULD also return functionally equivalent unstructured
        // content. (For example, serialized JSON can be returned in a TextContent block.)
        // https://modelcontextprotocol.io/specification/2025-06-18/server/tools#structured-content

        var structuredContent = jsonStringify(result.structuredContent());
        var content = (McpSchema.TextContent) result.content().get(0);

        assertThat(structuredContent).isEqualTo(expectedContent);
        assertThat(content.text()).isEqualTo(expectedContent);
    }

    @Test
    void testTool_noOutputSchema_structuredContentShouldBeEmpty() {
        var request = new McpSchema.CallToolRequest("test_tool_without_output_schema", null);
        var result = mcpClient.callTool(request);

        var content = (McpSchema.TextContent) result.content().get(0);

        assertThat(result.structuredContent()).isNull();
        assertThat(content.text()).isEqualTo("Just a text");
    }
}
