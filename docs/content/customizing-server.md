---
title: "Customizing Server Name and Package"
description: "Change the default MCP server key and generated class package via compiler arguments."
type: docs
weight: 8
---

You can change the default MCP server key and the package of the generated server class via **compiler arguments**. That lets you align generated class names and config keys with your project (e.g. `CalculatorMcpServer` in `com.acme.corp.mcp`).

## Maven configuration

Add the arguments under the maven-compiler-plugin (next to the annotation processor path):

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

- **mcp.default.server.key** — Config key for the default server (e.g. `calculator` → `mcp.calculator` in config and a generated class name like `CalculatorMcpServer`).
- **mcp.target.package** — Package for generated server classes.

## application.conf

The server key in config must match the compiler argument:

```hocon
mcp.calculator {
  name: "calculator-mcp-server"
  version: "0.1.0"
  mcpEndpoint: "/mcp/calculator"
}
```