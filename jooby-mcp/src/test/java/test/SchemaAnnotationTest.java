package test;

import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

/**
 * @author kliushnichenko
 */
public class SchemaAnnotationTest extends BaseTest {

    @Test
    void testTool_outputSchemaHasNullable_nullableShouldPassJsonValidator() {
        var request = new McpSchema.CallToolRequest("test_nullable_field", null);
        var result = mcpClient.callTool(request);

        var content = (McpSchema.TextContent) result.content().get(0);

        assertThat(result.isError()).isEqualTo(false);
        assertThat(content.text()).contains("\"middleName\":null");
    }
}
