package app;

import io.github.kliushnichenko.jooby.mcp.annotation.OutputSchema;
import io.github.kliushnichenko.jooby.mcp.annotation.Tool;

/**
 * @author kliushnichenko
 */
public class ToolsAnnotationsTestCases {

    @Tool(name = "test_tool_annotations_are_specified", annotations = @Tool.Annotations(
            title = "With Annotations",
            destructiveHint = false,
            idempotentHint = true,
            readOnlyHint = true,
            openWorldHint = false
    ))
    public String testAnnotations() {
        return "";
    }

    @Tool(name = "test_tool_annotations_are_partially_specified", annotations = @Tool.Annotations(
            readOnlyHint = true,
            openWorldHint = false
    ))
    public String testAnnotationsPartiallySpecified() {
        return "";
    }

    @Tool(name = "test_tool_annotations_are_not_specified")
    @OutputSchema.Suppressed
    public String testAnnotationsNotSpecified() {
        return "";
    }
}
