---
title: "MCP Inspector"
description: "Embed the MCP Inspector UI in your Jooby app for local testing. Direct connection mode."
type: docs
weight: 9
---

The **McpInspectorModule** embeds the [MCP Inspector](https://github.com/modelcontextprotocol/inspector) UI in your Jooby app so you can test your MCP server in the browser. It uses **Direct** connection mode only and is intended for local development. **McpModule** must be installed first.

## Setup

Add the dependency:

```xml
<dependency>
    <groupId>io.github.kliushnichenko</groupId>
    <artifactId>jooby-mcp-inspector</artifactId>
    <version>${jooby.mcp.version}</version>
</dependency>
```

Install the module next to **McpModule**:

```java
{
    install(new McpInspectorModule());
}
```

By default the inspector is served at **/mcp-inspector** and tries to auto-connect to the MCP server when the page loads.

## Configuration

You can change the path and disable auto-connect:

```java
install(new McpInspectorModule()
        .path("/custom-inspector-path")
        .autoConnect(false));
```

## Multiple servers

When more than one MCP server is registered, choose which one the inspector connects to by default:

```java
install(new McpInspectorModule()
        .defaultMcpServer("weather-mcp-server"));   // name from application.conf
```
