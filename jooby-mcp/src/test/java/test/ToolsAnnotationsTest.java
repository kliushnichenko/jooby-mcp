package test;

import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

/**
 * @author kliushnichenko
 */
public class ToolsAnnotationsTest extends BaseTest {

    @Test
    void annotations_allSpecified() {
        var tool = findTool("test_tool_annotations_are_specified");

        var expectedAnnotations = new McpSchema.ToolAnnotations("With Annotations",
                true,
                false,
                true,
                false,
                null);

        assertThat(tool.annotations()).isEqualTo(expectedAnnotations);
    }

    @Test
    void annotations_partiallySpecified() {
        var tool = findTool("test_tool_annotations_are_partially_specified");

        var expectedAnnotations = new McpSchema.ToolAnnotations(null,
                true,
                true,
                false,
                false,
                null);

        assertThat(tool.annotations()).isEqualTo(expectedAnnotations);
    }

    @Test
    void annotations_notSpecified() {
        var tool = findTool("test_tool_annotations_are_not_specified");

        assertThat(tool.annotations()).isNull();
    }
}
