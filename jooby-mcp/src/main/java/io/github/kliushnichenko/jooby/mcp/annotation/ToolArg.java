package io.github.kliushnichenko.jooby.mcp.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Provides metadata for parameters of methods annotated with {@link Tool}.
 * <p>
 * This annotation allows specifying parameter names and descriptions
 * that will be used to generate MCP tool parameters.
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
 * @see Tool
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.SOURCE)
public @interface ToolArg {

    /**
     * The name of the parameter as it will appear in the MCP tool schema.
     * <p>
     * If not specified, the parameter name from the method signature will be used
     * (requires compilation with -parameters flag).
     * </p>
     *
     * @return the parameter name
     */
    String name() default "";

    /**
     * A human-readable description of the parameter.
     * <p>
     * This description will be included in the tool schema and can help
     * AI models understand how to use the parameter.
     * </p>
     *
     * @return the parameter description
     */
    String description() default "";

    /**
     * Indicates whether the parameter is required.
     * <p>
     * By default, parameters are considered required. Set to false if the parameter is optional.
     * </p>
     *
     * @return true if the parameter is required, false otherwise
     */
    boolean required() default true;

} 