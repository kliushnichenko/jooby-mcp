package io.github.kliushnichenko.jooby.mcp.annotation;

import io.modelcontextprotocol.spec.McpSchema;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method as an MCP (Model Context Protocol) resource.
 * <p>
 * Methods annotated with {@code @Resource} will be automatically discovered by the annotation processor
 * and registered as MCP resources that can be accessed by MCP clients.
 * </p>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.SOURCE)
public @interface Resource {
    /**
     * The name of the resource
     * By default, the name is derived from the name of the annotated method.
     */
    String name() default "";

    /**
     * Optional human-readable name of the resource for display purposes.
     */
    String title() default "";

    /**
     * Optional description.
     */
    String description() default "";

    /**
     * Resource URI. Unique identifier for the resource.
     */
    String uri();

    /**
     * Optional MIME type.
     */
    String mimeType() default "";

    /**
     * Optional size in bytes.
     */
    int size() default -1;

    /**
     * Optional annotations that provide hints to clients about how to use or display the resource
     * Note that the default value of this annotation member is ignored.
     */
    Annotations annotations() default @Annotations(audience = McpSchema.Role.USER, lastModified = "", priority = 0.5);

    @Retention(RetentionPolicy.SOURCE)
    @Target(ElementType.ANNOTATION_TYPE)
    public @interface Annotations {

        /**
         * An array indicating the intended audience(s) for this resource. Valid values are "user" and "assistant".
         * For example, ["user", "assistant"] indicates content useful for both.
         */
        McpSchema.Role[] audience();

        /**
         * A number from 0.0 to 1.0 indicating the importance of this resource. A value of 1 means “most important”
         * (effectively required), while 0 means “least important” (entirely optional).
         */
        double priority();

        /**
         * An ISO 8601 formatted timestamp indicating when the resource was last modified (e.g. "2025-01-12T15:00:58Z").
         */
        String lastModified() default "";
    }
}
