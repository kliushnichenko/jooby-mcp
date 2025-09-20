package io.github.kliushnichenko.jooby.mcp.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.SOURCE)
public @interface Prompt {

    /**
     * The name of the prompt.
     *
     * @return the prompt name
     */
    String name() default "";

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
