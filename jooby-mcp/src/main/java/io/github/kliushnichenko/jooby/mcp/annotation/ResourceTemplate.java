package io.github.kliushnichenko.jooby.mcp.annotation;

import io.github.kliushnichenko.jooby.mcp.annotation.Resource.Annotations;
import io.modelcontextprotocol.spec.McpSchema;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark a method as a resource template provider.
 * Resource templates allow servers to expose parameterized resources using URI templates.
 * Arguments may be auto-completed through the completion API.
 * </p>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.SOURCE)
public @interface ResourceTemplate {
    /**
     * The name of the resource template
     * By default, the name is derived from the name of the annotated method.
     */
    String name() default "";

    /**
     * Optional human-readable name of the resource template for display purposes.
     */
    String title() default "";

    /**
     * Optional description.
     */
    String description() default "";

    /**
     * The Level 1 URI template that can be used to construct resource URIs.
     * <p>
     * See <a href="https://datatracker.ietf.org/doc/html/rfc6570#section-1.2">the RFC 6570</a> for syntax definition.
     */
    String uriTemplate();

    /**
     * Optional MIME type.
     */
    String mimeType() default "";

    /**
     * Optional annotations for the client.
     * <p>
     * Note that the default value of this annotation member is ignored. In other words, the annotations have to be declared
     * explicitly in order to be included in Resource metadata.
     */
    Annotations annotations() default @Annotations(audience = McpSchema.Role.USER, lastModified = "", priority = 0.5);
}
