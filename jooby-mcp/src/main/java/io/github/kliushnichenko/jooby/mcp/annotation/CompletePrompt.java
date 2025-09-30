package io.github.kliushnichenko.jooby.mcp.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.SOURCE;

/**
 * Annotates a method used to complete a prompt argument.
 */
@Retention(SOURCE)
@Target(METHOD)
public @interface CompletePrompt {

    /**
     * The name reference to a prompt. If not such {@link Prompt} exists then the build fails.
     */
    String value();

}
