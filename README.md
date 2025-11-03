[![Maven Central](https://img.shields.io/maven-central/v/io.github.kliushnichenko/jooby-mcp.svg)](https://search.maven.org/artifact/io.github.kliushnichenko/jooby-mcp)
[![Javadoc](https://javadoc.io/badge/io.github.kliushnichenko/jooby-mcp/jooby-mcp.svg)](https://javadoc.io/doc/io.github.kliushnichenko/jooby-mcp/latest)  

## jooby-mcp

This module integrates the official [Java MCP SDK](https://github.com/modelcontextprotocol/java-sdk) with [Jooby](https://github.com/jooby-project/jooby)’s routing, server features, and dependency injection.  

This module enables declarative (annotation-based) registration of tools, prompts, and resources.
Annotations are discovered at build time using APT, eliminating the need for runtime reflection.
To use it, add the annotation processor alongside the module dependency.

Compatibility:

| Jooby Version | Jooby MCP Version |
|---------------|-------------------|
| 3.x           | 1.x               |

Features:

- [X] SSE and Streamable-HTTP transport
- [X] Multiple servers support
- [X] Tools
- [X] Prompts
- [X] Resources
- [X] Resource Templates
- [X] Prompt Completions
- [X] Resource Template Completions
- [X] Required input arguments validation in tools
- [X] Build time method signature and return type validation

Table of Contents:

- [Quick Start](#quick-start)
- [Tools & Prompts Example](#tools--prompts-example)
- [Resource Example](#resource-example)
- [Resource Template Example](#resource-template-example)
- [Prompt Completion Example](#prompt-completion-example)
- [Multiple Servers Support](#multiple-servers-support)
- [Customizing Default Server Name and Package](#customizing-default-server-name-and-package)
- [Supported return types in Tools](#supported-return-types-in-tools)
- [Supported return types in Prompts](#supported-return-types-in-prompts)
- [Supported return types in Resources and Resource Templates](#supported-return-types-in-resources-and-resource-templates)
- [Supported return types in Prompt Completions](#supported-return-types-in-prompt-completions)

## Quick Start

1. Add the dependency to your `pom.xml`:
    ```xml
    <dependency>
        <groupId>io.github.kliushnichenko</groupId>
        <artifactId>jooby-mcp</artifactId>
        <version>${jooby.mcp.version}</version>
    </dependency>
   ```
2. Add annotation processor
   ```xml
   <plugin>
       <groupId>org.apache.maven.plugins</groupId>
       <artifactId>maven-compiler-plugin</artifactId>
       <configuration>
           <annotationProcessorPaths>
               <path>
                   <groupId>io.github.kliushnichenko</groupId>
                   <artifactId>jooby-mcp-apt</artifactId>
                   <version>${jooby.mcp.version}</version>
               </path>
           </annotationProcessorPaths>
       </configuration>
   </plugin>
   ```

3. Add configuration to `application.conf`   
   ⚠️ Since version `1.4.0` default transport was changed from `SSE` to `Streamable HTTP`
   ```
   mcp.default {                       # `default` is the server key, can be customized over compiler arguments
     name: "my-awesome-mcp-server"     # Required
     version: "0.1.0"                  # Required
     transport: "sse"                  # Optional (default: streamable-http)
     sseEndpoint: "/mcp/sse"           # Optional (default: /mcp/sse), applicable only to SSE transport
     messageEndpoint: "/mcp/message"   # Optional (default: /mcp/message), applicable only to SSE transport
   }
   ```

   Full config for `Streamable HTTP` transport:
   ```
    mcp.default {
      name: "my-awesome-mcp-server"    # Required
      version: "0.1.0"                 # Required
      transport: "streamable-http"     # Optional (default: streamable-http)
      mcpEndpoint: "/mcp/streamable"   # Optional (default: /mcp), applicable only to Streamable HTTP transport
      disallowDelete: true             # Optional (default: false)
      keepAliveInterval: 45            # Optional (default: N/A), in seconds
    }
    ```
   `keepAliveInterval` - enables sending periodic keep-alive messages to the client.  
   Disabled by default to avoid excessive network overhead. Set to a positive integer value (in seconds) to enable.

4. Implement your features (tools, prompts, resources, etc.), see examples below or in
   the [example-project](https://github.com/kliushnichenko/jooby-mcp/blob/1.x/jooby-mcp-example/src/main/java/io/github/kliushnichenko/mcp/example)

5. Install the module. After compilation, you can observe generated `DefaultMcpServer` class. Now register its instance
   in the module:
   ```java
   {
      install(new JacksonModule());                    // a JSON encode/decoder is required for JSONRPC
      install(AvajeInjectModule.of());                 // or other DI module of your choice
      install(new McpModule(new DefaultMcpServer());   // register MCP server
   }
   ```

### Tools & Prompts Example

```java
import io.github.kliushnichenko.jooby.mcp.annotation.Tool;
import io.github.kliushnichenko.jooby.mcp.annotation.ToolArg;
import io.github.kliushnichenko.jooby.mcp.annotation.Prompt;

@Singleton
public class ToolsAndPromptsExample {

    @Tool(name = "add", description = "Adds two numbers together")
    public String add(
            @ToolArg(name = "first", description = "First number to add") int a,
            @ToolArg(name = "second", description = "Second number to add") int b
    ) {
        int result = a + b;
        return String.valueOf(result);
    }

    @Tool
    public String subtract(int a, int b) {
        int result = a - b;
        return String.valueOf(result);
    }

    @Prompt(name = "summarizeText", description = "Summarizes the provided text into a specified number of sentences")
    public String summarizeText(@PromptArg(name = "text") String text, String maxSentences) {
        return String.format("""
                Please provide a clear and concise summary of the following text in no more than %s sentences:
                %s
                """, maxSentences, text);
    }
} 
```

### Resource Example

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

Find more examples in
the [project](https://github.com/kliushnichenko/jooby-mcp/blob/1.x/jooby-mcp-example/src/main/java/io/github/kliushnichenko/mcp/example/ResourceExamples.java)

### Resource Template Example

```java
import io.github.kliushnichenko.jooby.mcp.annotation.ResourceTemplate;
import io.github.kliushnichenko.jooby.mcp.annotation.CompleteResourceTemplate;

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
```

### Prompt Completion Example

```java
import io.github.kliushnichenko.jooby.mcp.annotation.CompleteArg;
import io.github.kliushnichenko.jooby.mcp.annotation.CompletePrompt;
import io.github.kliushnichenko.jooby.mcp.annotation.Prompt;

@Singleton
public class PromptCompletionsExample {

    private static final List<String> SUPPORTED_LANGUAGES = List.of("Java", "Python", "JavaScript", "Go", "TypeScript");

    @Prompt(name = "code_review", description = "Code Review Prompt")
    public String codeReviewPrompt(String codeSnippet, String language) {
        return """
                You are a senior software engineer tasked with reviewing the following %s code snippet:
                   
                %s
                   
                Please provide feedback on:
                1. Code readability and maintainability.
                2. Potential bugs or issues.
                3. Suggestions for improvement.
                 """.formatted(language, codeSnippet);
    }

    @CompletePrompt("code_review")
    public List<String> completeCodeReviewLang(@CompleteArg(name = "language") String partialInput) {
        return SUPPORTED_LANGUAGES.stream()
                .filter(lang -> lang.toLowerCase().contains(partialInput.toLowerCase()))
                .toList();
    }
}
```

### Multiple Servers Support

Use `@McpServer` annotation to assign a tool or prompt to a specific server. Annotation can be applied at the class or
method level.

   ```java
   import io.github.kliushnichenko.jooby.mcp.annotation.McpServer;

@Singleton
@McpServer("weather")
public class WeatherService {

    public record Coordinates(double latitude, double longitude) {
    }

    @Tool(name = "get_weather")
    public String getWeather(Coordinates coordinates) {
            ...
    }
}
   ```

As a result, additional `WeatherMcpServer` class will be generated. Register it in the module:

   ```java
   {
        install(new McpModule(new DefaultMcpServer(),new WeatherMcpServer()));
   }
   ```

The weather MCP server should have its own configuration section in `application.conf`:

   ```
   mcp.weather {
     name: "weather-mcp-server"
     version: "0.1.0"
     mcpEndpoint: "/mcp/weather"
   }
   ```

### Customizing Default Server Name and Package

You can customize the default server name and the package where the generated server classes will be placed by providing
compiler arguments. For example, to set the default server name to `CalculatorMcpServer` and the package
to `com.acme.corp.mcp`, you can add the following configuration to your `pom.xml`:

   ```xml

<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <configuration>
        <annotationProcessorPaths>
            <path>
                <groupId>io.github.kliushnichenko</groupId>
                <artifactId>jooby-mcp-apt</artifactId>
                <version>${version}</version>
            </path>
        </annotationProcessorPaths>
        <compilerArgs>
            <arg>-Amcp.default.server.key=calculator</arg>
            <arg>-Amcp.target.package=com.acme.corp.mcp</arg>
        </compilerArgs>
    </configuration>
</plugin>
   ```

Mind, that `mcp.default.server.key` should match the configuration section in `application.conf`:

   ```
   mcp.calculator {
     name: "calculator-mcp-server"
     version: "0.1.0"
     mcpEndpoint: "/mcp/calculator"
   }
   ```

#### Supported return types in Tools

- `String`
- `McpSchema.CallToolResult`
- `McpSchema.Content`
- POJO (will be serialized to JSON)

#### Supported return types in Prompts

- `McpSchema.GetPromptResult`
- `McpSchema.PromptMessage`
- `McpSchema.Content`
- `String`
- POJO (`toString()` method will be invoked to get the string representation)

#### Supported return types in Resources and Resource Templates

- `McpSchema.ReadResourceResult`
- `McpSchema.ResourceContents`
- `List<McpSchema.ResourceContents>`
- `McpSchema.TextResourceContents`
- `McpSchema.BlobResourceContents`
- POJO (will be serialized to JSON)

#### Supported return types in Completions

- `McpSchema.CompleteResult`
- `McpSchema.CompleteResult.CompleteCompletion`
- `List<String>`
- `String`
