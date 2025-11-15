package io.github.kliushnichenko.jooby.mcp.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Provides metadata for parameters of methods annotated with {@link Prompt}.
 * <p>
 * This annotation allows specifying parameter names and descriptions
 * that will be used to generate MCP prompt parameters.
 * </p>
 *
 * @see Prompt
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.SOURCE)
public @interface PromptArg {

    /**
     * The name of the parameter.
     *
     * @return the parameter name
     */
    String name() default "";

    /**
     * A human-readable description of the parameter.
     *
     * @return the parameter description
     */
    String description() default "";

    /**
     * An argument is required by default.
     */
    boolean required() default true;
}
