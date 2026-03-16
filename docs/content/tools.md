---
title: "Tools"
description: "Expose callable operations to MCP clients with @Tool and @ToolArg. Build-time discovery, validation, and schema generation."
type: docs
weight: 3
---

Tools expose callable operations to MCP clients. Annotate methods with **@Tool**. The annotation processor discovers them at build time and registers them with the generated MCP server.

## Example

```java
import io.github.kliushnichenko.jooby.mcp.annotation.Tool;
import io.github.kliushnichenko.jooby.mcp.annotation.ToolArg;

@Singleton
public class ToolsExample {

    @Tool(name = "add", description = "Adds two numbers together")
    public String add(
            @ToolArg(name = "first", description = "First number to add") int a,
            @ToolArg(name = "second", description = "Second number to add") int b
    ) {
        return String.valueOf(a + b);
    }

    @Tool
    public String subtract(int a, int b) {
        return String.valueOf(a - b);
    }
}
```

- **@Tool** — Exposes the method as a tool. Name and description can be inferred or set explicitly.
- **@ToolArg** — Describes parameters for the generated JSON schema and client UX.

## Output schema

The output schema is derived from the method’s return type. For example, a tool that returns a `Pet` produces a schema that matches that class.

If the return type is a reserved type (`String`, `McpSchema.CallToolResult`, or `McpSchema.Content`), no schema is generated automatically. Use **@OutputSchema** in that case:

- **@OutputSchema.From(MyClass.class)** — Use the schema for that class.
- **@OutputSchema.ArrayOf(MyClass.class)** — Array of that class.
- **@OutputSchema.MapOf(MyClass.class)** — Map with that class as value type.

Example for a tool that returns `CallToolResult` but should advertise a `Pet` schema:

```java
@Tool(name = "find_pet", description = "Finds a pet by its ID")
@OutputSchema.From(Pet.class)
public McpSchema.CallToolResult findPet(String petId) {
    // ...
    return new McpSchema.CallToolResult(pet, false);
}
```

Use **@OutputSchema.Suppressed** to skip output schema generation when you don’t want to expose a structured result.

## Enriching the JSON schema

You can refine the generated schema (for both arguments and return types) with OpenAPI annotations. The processor respects:

- **@Schema** — `description`, `requiredMode` (and related).
- **@JsonProperty** — The annotation’s `value` is used as the property name in the schema.

```java
import io.swagger.v3.oas.annotations.media.Schema;
import com.fasterxml.jackson.annotation.JsonProperty;

class User {
    @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, description = "The user's middle name")
    @JsonProperty("middle-name")
    private String middleName;
}
```
