package io.github.kliushnichenko.jooby.mcp.apt.completions;

import com.palantir.javapoet.ClassName;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.Getter;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;

/**
 * Represents a completion entry with its metadata.
 * This record holds information about the completion's identifier, type,
 * the service class it belongs to, and the method that defines the completion.
 */
public record CompletionEntry(String identifier,
                              String argumentName,
                              Type type,
                              String serverKey,
                              TypeElement serviceClass,
                              ExecutableElement method) {

    @Getter
    public enum Type {
        PROMPT("ref/prompt", ClassName.get(McpSchema.PromptReference.class)),
        RESOURCE("ref/resource", ClassName.get(McpSchema.ResourceReference.class));

        private final String value;
        private final ClassName className;

        Type(String value, ClassName className) {
            this.value = value;
            this.className = className;
        }
    }
}
