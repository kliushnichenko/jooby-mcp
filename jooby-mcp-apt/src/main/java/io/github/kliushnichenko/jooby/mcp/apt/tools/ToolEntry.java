package io.github.kliushnichenko.jooby.mcp.apt.tools;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;

/**
 * Represents a tool entry with its metadata.
 * This record holds information about the tool's name, description, server key,
 * the service class it belongs to, and the method that defines the tool.
 */
public record ToolEntry(String toolName,
                        String toolDescription,
                        String serverKey,
                        TypeElement serviceClass,
                        ExecutableElement method) {
}
