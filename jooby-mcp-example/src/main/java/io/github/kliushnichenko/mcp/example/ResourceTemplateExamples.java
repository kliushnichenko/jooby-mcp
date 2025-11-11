package io.github.kliushnichenko.mcp.example;

import io.github.kliushnichenko.jooby.mcp.ResourceUri;
import io.github.kliushnichenko.jooby.mcp.annotation.CompleteArg;
import io.github.kliushnichenko.jooby.mcp.annotation.CompleteResourceTemplate;
import io.github.kliushnichenko.jooby.mcp.annotation.ResourceTemplate;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.inject.Singleton;

import java.util.List;
import java.util.Map;

/**
 * @author kliushnichenko
 */
@Singleton
public class ResourceTemplateExamples {

    private static final Map<String, String> PROJECTS = Map.of(
            "project-alpha", "This is Project Alpha.",
            "project-beta", "This is Project Beta.",
            "project-gamma", "This is Project Gamma."
    );

    @ResourceTemplate(name = "get_project", uriTemplate = "file:///project/{name}")
    public McpSchema.TextResourceContents getProject(String name, ResourceUri resourceUri) {
        String content = PROJECTS.getOrDefault(name, "<Project not found>");
        return new McpSchema.TextResourceContents(resourceUri.uri(), "text/markdown", content);
    }

    @CompleteResourceTemplate("get_project")
    public List<String> projectNameCompletion(@CompleteArg(name = "name") String partialInput) {
        return PROJECTS.keySet()
                .stream()
                .filter(name -> name.contains(partialInput))
                .toList();
    }
}
