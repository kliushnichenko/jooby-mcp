---
title: "Introduction"
description: "jooby-mcp integrates the MCP Java SDK with Jooby. Expose tools, prompts, and resources via annotations with build-time discovery."
type: docs
weight: 1
---

<p style="display: flex; flex-wrap: nowrap; gap: 8px; align-items: center; margin-bottom: 1rem;">
  <a target="_blank" href="https://central.sonatype.com/search?q=jooby-mcp"><img src="https://img.shields.io/maven-central/v/io.github.kliushnichenko/jooby-mcp.svg" alt="Maven Central" loading="lazy" decoding="async"></a>
  <a target="_blank" href="https://mvnrepository.com/artifact/io.github.kliushnichenko/jooby-mcp"><img src="https://badges.mvnrepository.com/badge/io.github.kliushnichenko/jooby-mcp/badge.svg?label=MvnRepository&color=green" alt="MvnRepository" loading="lazy" decoding="async"></a>
  <a><img src="https://badge.mcpx.dev" alt="MCP" loading="lazy" decoding="async"></a>
  <a target="_blank" href="https://javadoc.io/doc/io.github.kliushnichenko/jooby-mcp/latest"><img src="https://javadoc.io/badge/io.github.kliushnichenko/jooby-mcp/jooby-mcp.svg" alt="Javadoc" loading="lazy" decoding="async"></a>
</p>

## What is jooby-mcp?

**jooby-mcp** integrates the [Model Context Protocol (MCP) Java SDK](https://github.com/modelcontextprotocol/java-sdk)
with the [Jooby](https://github.com/jooby-project/jooby) framework. It lets you expose **tools**, **prompts**, and **resources** to MCP clients using declarative, annotation-based APIs with discovery at **build time** via an annotation
processor, so no runtime reflection is required.

## Prerequisites

- A [Jooby](https://jooby.io) application (3.x or 4.x).
- Basic familiarity with MCP
  concepts ([tools](https://modelcontextprotocol.io/specification/2025-11-25/server/tools), [prompts](https://modelcontextprotocol.io/specification/2025-11-25/server/prompts), [resources](https://modelcontextprotocol.io/specification/2025-11-25/server/resources))
  is helpful but not required.

## Version compatibility

| Jooby | jooby-mcp |
|-------|-----------|
| 3.x   | 1.x       |
| 4.x   | 2.x       |

## Features

**Transport**

- SSE, Streamable HTTP, and Stateless Streamable HTTP
- Multiple MCP servers in one application

**MCP capabilities**

- Tools, Prompts, Resources
- Resource templates with URI patterns and completions
- Prompt completions and resource-template completions

**Quality & tooling**

- Required argument validation and build-time checks for method signatures and return types
- Elicitation, Sampling, and Progress via the exchange object
- Optional [MCP Inspector]({{< ref "mcp-inspector" >}}) module for local testing

## In this documentation

| Section                                                                                                    | Description                                                                |
|------------------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------|
| [Quick Start]({{< ref "quick-start" >}})                                                                   | Add the dependency, configure the server, and run your first MCP endpoint. |
| [Tools]({{< ref "tools" >}}), [Prompts]({{< ref "prompts" >}}), [Resources]({{< ref "resources" >}})       | Core MCP capabilities and annotations.                                     |
| [Exchange object]({{< ref "exchange-object" >}})                                                           | Elicitation, Sampling, and Progress.                                       |
| [Multiple servers]({{< ref "multiple-servers" >}}), [Customizing server]({{< ref "customizing-server" >}}) | Run several servers and change the default key or package.                 |
| [MCP Inspector]({{< ref "mcp-inspector" >}})                                                               | Local testing in the browser.                                              |
| [Supported return types]({{< ref "appendix-return-types" >}})                                              | Reference for tools, prompts, and resources.                               |

**Next:** [Quick Start]({{< ref "quick-start" >}}) — add the dependency and run your first MCP server.
