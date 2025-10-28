package io.github.kliushnichenko.mcp.example;

import io.github.kliushnichenko.jooby.mcp.ResourceUri;
import io.github.kliushnichenko.jooby.mcp.annotation.ResourceTemplate;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.inject.Singleton;

import java.util.Map;

@Singleton
public class ResourceTemplateExamples {

    private static final Map<String, String> PROJECTS = Map.of(
            "project-alpha", "This is Project Alpha.",
            "project-beta", "This is Project Beta.",
            "project-gamma", "This is Project Gamma."
    );

    @ResourceTemplate(uriTemplate = "file:///project/{name}")
    public McpSchema.TextResourceContents getProject(String name, ResourceUri resourceUri) {
        String content = PROJECTS.getOrDefault(name, "<Project not found>");
        return new McpSchema.TextResourceContents(resourceUri.uri(), "text/markdown", content);
    }
}
