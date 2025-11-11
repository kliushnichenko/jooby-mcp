package io.github.kliushnichenko.jooby.mcp.apt.resources;

import io.modelcontextprotocol.spec.McpSchema;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;

/**
 * @author kliushnichenko
 */
public record ResourceEntry(
        String name,
        String title,
        String description,
        String uri,
        String mimeType,
        int size,
        Annotations annotations,
        String serverKey,
        TypeElement serviceClass,
        ExecutableElement method
) {

    public record Annotations(McpSchema.Role[] audience, double priority, String lastModified) {
    }
}
