package io.github.kliushnichenko.jooby.mcp.annotation;

import io.modelcontextprotocol.spec.McpSchema;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.SOURCE;

/**
 * Annotates a method used to complete a prompt argument.
 * <p>
 * The result of a "complete" operation is always represented as a {@link McpSchema.CompleteResult}.
 * However, the annotated method can also return other types that are converted according to the following rules.
 * <ul>
 * <li>If the method returns {@link String} then the response contains the single value.</li>
 * <li>If the method returns a {@link java.util.List} of {@link String} then
 * the response contains the list of values.</li>
 * </ul>
 * In other words, the return type must be one of the following list:
 * <ul>
 * <li>{@code McpSchema.CompleteResult}</li>
 * <li>{@code McpSchema.CompleteResult.CompleteCompletion}</li>
 * <li>{@code List<String>}</li>
 * <li>{@code String}</li>
 * </ul>
 * <p>
 * A prompt completion method must consume exactly one {@link String} argument.
 *
 * @see Prompt#name()
 */
@Retention(SOURCE)
@Target(METHOD)
public @interface CompletePrompt {

    /**
     * The name reference to a prompt. If no such {@link Prompt} exists then the build fails.
     */
    String value();
}
