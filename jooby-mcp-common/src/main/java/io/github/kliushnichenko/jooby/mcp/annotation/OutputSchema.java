package io.github.kliushnichenko.jooby.mcp.annotation;

import io.modelcontextprotocol.spec.McpSchema;

/**
 * Defines the structure of the tool's output.
 * <p>
 * If not specified, the output schema will be inferred from the method's return type,
 * unless return type is not in the list of the reserved types: {@link McpSchema.CallToolResult},
 * {@link McpSchema.Content}, {@link McpSchema.TextContent}, {@link McpSchema.ImageContent},
 * {@link McpSchema.AudioContent}, {@link McpSchema.EmbeddedResource}, {@link McpSchema.ResourceLink},
 * {@link Object},{@link String}, JsonNode, JsonArray, ObjectNode, JsonValue.
 * <p>
 * Use {@link OutputSchema.Suppressed} annotation to suppress output schema generation from return type.
 *
 * @see Tool
 */
public @interface OutputSchema {

    @interface From {
        Class<?> value();
    }

    @interface ArrayOf {
        Class<?> value();
    }

    @interface MapOf {
        Class<?> value();
    }

    @interface Suppressed {
    }
}
