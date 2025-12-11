package io.github.kliushnichenko.jooby.mcp.apt.tools;

import io.modelcontextprotocol.spec.McpSchema;
import org.jspecify.annotations.Nullable;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;

/**
 * Represents a tool entry with its metadata.
 * This record holds information about the tool's name, description, server key,
 * the service class it belongs to, and the method that defines the tool.
 *
 * @author kliushnichenko
 */
public record ToolEntry(String toolName,
                        String toolTitle,
                        String toolDescription,
                        @Nullable TypeMirror outputType,
                        McpSchema.ToolAnnotations annotations,
                        String serverKey,
                        TypeElement serviceClass,
                        ExecutableElement method) {
}
