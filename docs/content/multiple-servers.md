---
title: "Multiple Servers"
description: "Run several MCP servers in one app with @McpServer. Assign tools and prompts to a server by key."
type: docs
weight: 7
---

You can run several MCP servers in one application. Use **@McpServer** to assign tools or prompts to a specific server. The annotation processor generates a dedicated server class per key (e.g. `WeatherMcpServer` for `"weather"`).

## 1. Annotate classes or methods

Apply **@McpServer** at class or method level:

```java
import io.github.kliushnichenko.jooby.mcp.annotation.McpServer;
import io.github.kliushnichenko.jooby.mcp.annotation.Tool;

@Singleton
@McpServer("weather")
public class WeatherService {

    public record Coordinates(double latitude, double longitude) {}

    @Tool(name = "get_weather")
    public String getWeather(Coordinates coordinates) {
        // ...
    }
}
```

The processor generates **WeatherMcpServer** (and keeps **DefaultMcpServer** for the default key).

## 2. Register all server instances

Pass every generated server to **McpModule**:

```java
{
    install(new McpModule(new DefaultMcpServer(), new WeatherMcpServer()));
}
```

## 3. Configure each server in application.conf

Each server key has its own config block:

```hocon
mcp.weather {
  name: "weather-mcp-server"
  version: "0.1.0"
  mcpEndpoint: "/mcp/weather"
}
```

Use the same options as for the default server (transport, endpoints, etc.) as needed.
