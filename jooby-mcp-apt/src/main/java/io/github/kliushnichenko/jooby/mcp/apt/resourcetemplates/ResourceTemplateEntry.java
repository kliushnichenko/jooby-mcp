package io.github.kliushnichenko.jooby.mcp.apt.resourcetemplates;

import io.github.kliushnichenko.jooby.mcp.apt.resources.ResourceEntry;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;

public record ResourceTemplateEntry(
        String name,
        String title,
        String description,
        String uriTemplate,
        String mimeType,
        ResourceEntry.Annotations annotations,
        String serverKey,
        TypeElement serviceClass,
        ExecutableElement method
) {
}
