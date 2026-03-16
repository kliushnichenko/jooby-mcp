---
title: "Exchange Object"
description: "Use McpSyncServerExchange for Elicitation, Sampling, and Progress. Inject the exchange as a method argument."
type: docs
weight: 6
---

The **McpSyncServerExchange** gives tool (and related) handlers access to the current request and to MCP features such as **Elicitation**, **Sampling**, and **Progress**. Add it as an argument to your method. The framework injects the current exchange.

## Example

```java
public class ElicitationExample {

    @Tool(name = "elicitation_example")
    public String elicitationExample(McpSyncServerExchange exchange) {
        // ...
        exchange.createElicitation(request);
        // ...
    }
}
```

Use the same pattern for Sampling, Progress, and other exchange-based features. For a full example, see the [example project](https://github.com/kliushnichenko/jooby-mcp/blob/1.x/jooby-mcp-example/src/main/java/io/github/kliushnichenko/mcp/example/ElicitationExample.java).

For details on the SDK APIs, see the [MCP Java SDK documentation](https://modelcontextprotocol.io/sdk/java/mcp-server#using-sampling-from-a-server).
