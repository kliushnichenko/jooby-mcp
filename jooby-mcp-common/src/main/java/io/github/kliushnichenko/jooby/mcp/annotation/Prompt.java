package io.github.kliushnichenko.jooby.mcp.annotation;

import io.modelcontextprotocol.spec.McpSchema;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method as an MCP prompt.
 * <p>
 * Methods annotated with {@code @Prompt} will be automatically discovered by the annotation processor
 * and registered as MCP prompts that can be used by MCP clients.
 * </p>
 * The result of a "get prompt" operation is always represented as a {@link McpSchema.GetPromptResult }. However,
 * the annotated method can also return other types that are converted according to the following rules.
 * <ul>
 * <li>If it returns {@link McpSchema.PromptMessage} then the response contains the single prompt message object.</li>
 * <li>If it returns {@link McpSchema.Content} then the response contains the single prompt message object.</li>
 * <li>If it returns {@link java.lang.String} then the response contains the single prompt message object.</li>
 * <li>For any other type the `toString()` method will be executed to get prompt string representation.</li>
 * </ul>
 *
 * @see PromptArg
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.SOURCE)
public @interface Prompt {

    /**
     * A unique name of the prompt.
     * <p>
     * By default, the name is derived from the name of the annotated method.
     * @return the prompt name
     */
    String name() default "";

    /**
     * A human-readable name for this prompt.
     */
    String title() default "";

    /**
     * A human-readable description of what the prompt does.
     * <p>
     * This description will be provided to MCP clients and can be used by AI models
     * to understand when and how to use the prompt.
     * </p>
     *
     * @return the prompt description
     */
    String description() default "";
}
