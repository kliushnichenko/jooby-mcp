package io.github.kliushnichenko.jooby.mcp.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method as an MCP (Model Context Protocol) tool.
 * <p>
 * Methods annotated with {@code @Tool} will be automatically discovered by the annotation processor
 * and registered as MCP tools that can be called by MCP clients.
 * </p>
 * 
 * <p>Example usage:</p>
 * <pre>
 * {@code
 * @Tool(name = "calculator", description = "Performs basic arithmetic operations")
 * public CalculatorResult calculate(
 *     @ToolArg(name = "operation", description = "The operation to perform") String operation,
 *     @ToolArg(name = "a", description = "First operand") double a,
 *     @ToolArg(name = "b", description = "Second operand") double b
 * ) {
 *     // implementation
 * }
 * }
 * </pre>
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
     * A human-readable description of what the tool does.
     * <p>
     * This description will be provided to MCP clients and can be used by AI models
     * to understand when and how to use the tool.
     * </p>
     *
     * @return the tool description
     */
    String description() default "";
}
