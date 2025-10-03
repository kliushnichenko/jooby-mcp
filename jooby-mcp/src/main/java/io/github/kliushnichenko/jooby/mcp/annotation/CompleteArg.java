package io.github.kliushnichenko.jooby.mcp.annotation;

/**
 * Annotation for customizing the name of a completed argument, like prompt argument or resource template argument.
 * <p>
 * A completion method must consume exactly one {@link String} argument.
 */
public @interface CompleteArg {

    /**
     * The name of the completed argument, by default is the name of the element.
     */
    String name() default "";
}
