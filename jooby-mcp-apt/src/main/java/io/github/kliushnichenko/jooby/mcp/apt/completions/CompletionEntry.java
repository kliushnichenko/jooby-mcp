package io.github.kliushnichenko.jooby.mcp.apt.completions;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;

/**
 * Represents a completion entry with its metadata.
 * This record holds information about the completion's identifier, type,
 * the service class it belongs to, and the method that defines the completion.
 */
public record CompletionEntry(String identifier,
                              String argumentName,
                              String type,
                              String serverKey,
                              TypeElement serviceClass,
                              ExecutableElement method) {
}
