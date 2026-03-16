---
title: "Resources"
description: "Expose content by URI with @Resource. Static resources and resource templates with URI patterns and completions."
type: docs
weight: 5
---

Resources expose content to MCP clients by URI. You can expose **static resources** (one URI per method) or **resource templates** (a URI pattern with variables and optional completions).

## Static resources

Each method annotated with **@Resource** is registered as a resource the client can read by that URI.

```java
import io.github.kliushnichenko.jooby.mcp.annotation.Resource;

@Singleton
public class ResourceExamples {

    @Resource(uri = "file:///project/README.md", name = "README.md", title = "README", mimeType = "text/markdown")
    public McpSchema.TextResourceContents textResource() {
        String content = """
                # Project Title

                This is an example README file for the project.

                ## Features

                - Feature 1
                - Feature 2
                - Feature 3
                """;
        return new McpSchema.TextResourceContents("file:///project/README.md", "text/markdown", content);
    }
}
```

- **uri** — Identifier clients use to request this resource.
- **name** / **title** — Metadata for listing and display.
- **mimeType** — Content type of the returned body.

## Resource templates

Resource templates expose a family of resources under a URI pattern (e.g. `file:///project/{name}`). You implement a handler that receives the template variables and returns the content, and optionally a **completion** method so clients can discover or suggest valid values for a variable (e.g. project names).

```java
import io.github.kliushnichenko.jooby.mcp.annotation.ResourceTemplate;
import io.github.kliushnichenko.jooby.mcp.annotation.CompleteResourceTemplate;
import io.github.kliushnichenko.jooby.mcp.annotation.CompleteArg;

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
        return PROJECTS.keySet().stream()
                .filter(n -> n.contains(partialInput))
                .toList();
    }
}
```

- **@ResourceTemplate** — Binds a handler to a URI template. Method parameters map to template variables (and `ResourceUri` gives the resolved URI).
- **@CompleteResourceTemplate** — Binds a completion handler to that template. **@CompleteArg** maps parameters to template variables so the client can get suggestions as the user types.

Return types for resources and templates must be one of the [supported resource return types]({{< ref "appendix-return-types" >}}#resources-and-resource-templates). For more examples, see the [example project](https://github.com/kliushnichenko/jooby-mcp/blob/1.x/jooby-mcp-example/src/main/java/io/github/kliushnichenko/mcp/example/ResourceExamples.java).
