---
title: "Quick Start"
description: "Add the jooby-mcp dependency, configure the annotation processor, and run your first MCP server with Jooby."
type: docs
weight: 2
---

This guide gets you from zero to a running MCP server with jooby-mcp in a few steps.

## 1. Add the dependency

In your `pom.xml`:

```xml
<dependency>
    <groupId>io.github.kliushnichenko</groupId>
    <artifactId>jooby-mcp</artifactId>
    <version>${jooby.mcp.version}</version>
</dependency>
```

## 2. Add the annotation processor

Configure the Maven Compiler Plugin so the APT processor generates the MCP server class:

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

## 3. Configure the MCP server

In `application.conf`, define at least one MCP server. The key (e.g. `default`) must match what the annotation processor expects (see [Customizing default server]({{< ref "customizing-server" >}}) to change it).

> **Note:** Since **1.4.0** the default transport is **Streamable HTTP**, not SSE. Set `transport: "sse"` explicitly if you need SSE.

**SSE transport**

```hocon
mcp.default {
  name: "my-awesome-mcp-server"     # Required
  version: "0.1.0"                  # Required
  transport: "sse"                  # Optional (default: streamable-http)
  sseEndpoint: "/mcp/sse"           # Optional (default: /mcp/sse)
  messageEndpoint: "/mcp/message"   # Optional (default: /mcp/message)
}
```

**Streamable HTTP transport** (default)

```hocon
mcp.default {
  name: "my-awesome-mcp-server"
  version: "0.1.0"
  transport: "streamable-http"
  mcpEndpoint: "/mcp/streamable"    # Optional (default: /mcp)
  disallowDelete: true              # Optional (default: false)
  keepAliveInterval: 45             # Optional, in seconds
  instructions: "..."               # Optional: server instructions for clients
}
```

**Stateless Streamable HTTP**

```hocon
mcp.default {
  name: "my-awesome-mcp-server"
  version: "0.1.0"
  transport: "stateless-streamable-http"
  mcpEndpoint: "/mcp/stateless-streamable"   # Optional (default: /mcp)
}
```

- **keepAliveInterval** — Sends periodic keep-alive messages when set to a positive number (seconds). Off by default.
- **instructions** — Shown to clients during initialization. Use it to describe how to use the server.

## 4. Implement tools, prompts, or resources

Add one or more classes with `@Tool`, `@Prompt`, or `@Resource` (and related) annotations. See [Tools]({{< ref "tools" >}}), [Prompts]({{< ref "prompts" >}}), and [Resources]({{< ref "resources" >}}) for examples, or browse the [example project](https://github.com/kliushnichenko/jooby-mcp/tree/main/jooby-mcp-example/src/main/java/io/github/kliushnichenko/jooby/mcp/example).

## 5. Register the MCP module

After building, the processor generates a `DefaultMcpServer` class (or a custom name if configured). Install the module in your Jooby app:

```java
{
    install(new JacksonModule());              // Required for JSON-RPC
    install(AvajeInjectModule.of());           // Or your preferred DI module
    install(new McpModule(new DefaultMcpServer()));
}
```

Your MCP server is now available at the configured endpoint. Next, define [Tools]({{< ref "tools" >}}), [Prompts]({{< ref "prompts" >}}), or [Resources]({{< ref "resources" >}}).
