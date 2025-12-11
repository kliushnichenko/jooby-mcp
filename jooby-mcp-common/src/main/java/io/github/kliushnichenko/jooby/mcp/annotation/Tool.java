package io.github.kliushnichenko.jooby.mcp.annotation;

import io.modelcontextprotocol.spec.McpSchema;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method as an MCP tool.
 * <p>
 * Methods annotated with {@link Tool} will be automatically discovered by the annotation processor
 * and registered as MCP tools that can be called by MCP clients.
 * </p>
 * The result of a "tool invocation" operation is always represented as a {@link McpSchema.CallToolResult}. However,
 * the annotated method can also return other types that are converted according to the following rules.
 * <ul>
 * <li>If it returns {@link McpSchema.Content} then the response contains the single content object.</li>
 * <li>If it returns a {@link java.lang.String} then the response contains
 * the single {@link McpSchema.TextContent} object.</li>
 * <li>Any other type will be encoded to JSON string.</li>
 * </ul>
 *
 * @see ToolArg
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.SOURCE)
public @interface Tool {

    /**
     * The name of the tool. This will be used as the tool identifier in the MCP protocol.
     * <p>
     * If not specified, the method name will be used as the tool name.
     * </p>
     *
     * @return the tool name
     */
    String name() default "";

    /**
     * A human-readable title for the tool.
     */
    String title() default "";

    /**
     * A human-readable description of what the tool does.
     * <p>
     * This description will be provided to MCP clients and can be used by AI models
     * to understand when and how to use the tool.
     * </p>
     *
     * @return the tool description
     */
    String description() default "";

    /**
     * Additional hints for clients.
     * <p>
     * Note that the default value of this annotation member is ignored. In other words, the annotations
     * have to be declared explicitly in order to be included in Tool metadata.
     */
    Annotations annotations() default @Annotations;

    @Retention(RetentionPolicy.SOURCE)
    @Target(ElementType.ANNOTATION_TYPE)
    @interface Annotations {

        /**
         * A human-readable title for the tool.
         */
        String title() default "";

        /**
         * If true, the tool does not modify its environment.
         */
        boolean readOnlyHint() default false;

        /**
         * If true, the tool may perform destructive updates to its environment. If false, the tool performs only
         * additive updates. (This property is meaningful only when readOnlyHint == false)
         */
        boolean destructiveHint() default true;

        /**
         * If true, calling the tool repeatedly with the same arguments will have no additional effect
         * on the its environment. (This property is meaningful only when readOnlyHint == false)
         */
        boolean idempotentHint() default false;

        /**
         * If true, this tool may interact with an “open world” of external entities. If false, the tool’s domain
         * of interaction is closed. For example, the world of a web search tool is open,
         * whereas that of a memory tool is not.
         */
        boolean openWorldHint() default true;
    }
}
