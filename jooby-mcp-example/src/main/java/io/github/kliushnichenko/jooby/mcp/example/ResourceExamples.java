package io.github.kliushnichenko.jooby.mcp.example;

import io.github.kliushnichenko.jooby.mcp.annotation.Resource;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.inject.Singleton;

import java.util.List;

/**
 * @author kliushnichenko
 */
@Singleton
public class ResourceExamples {

    @Resource(uri = "file:///project/README.md", name = "README.md", title = "README.md", mimeType = "text/markdown")
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

    @Resource(uri = "file:///blob")
    public McpSchema.BlobResourceContents blobResource() {
        String content = "blob";
        return new McpSchema.BlobResourceContents("file:///blob", null, content);
    }

    @Resource(uri = "file:///project/thread-stone",
            size = 10_563,
            annotations = @Resource.Annotations(
                    audience = {McpSchema.Role.USER, McpSchema.Role.ASSISTANT},
                    priority = 0.3)
    )
    public List<McpSchema.TextResourceContents> threadStone() {
        return List.of(
                new McpSchema.TextResourceContents("file:///folder/file1", null, "file1 content"),
                new McpSchema.TextResourceContents("file:///folder/file2", null, "file2 content")
        );
    }

    @Resource(uri = "file:///project/blackbriar/metadata.json")
    public Metadata blackBriar() {
        return new Metadata("Blackbriar", "1.0.0", List.of("Noah Vosen"));
    }

    public record Metadata(String name, String version, List<String> authors) {
    }
}
