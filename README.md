[![Maven Central](https://img.shields.io/maven-central/v/io.github.kliushnichenko/jooby-mcp.svg)](https://central.sonatype.com/search?q=jooby-mcp)
[![Github](https://github.com/kliushnichenko/jooby-mcp/actions/workflows/maven.yml/badge.svg)](https://github.com/kliushnichenko/jooby-mcp/actions)
[![Documentation](https://img.shields.io/badge/documentation-site-green)](https://kliushnichenko.github.io/jooby-mcp/)
[![Javadoc](https://javadoc.io/badge/io.github.kliushnichenko/jooby-mcp/jooby-mcp.svg)](https://javadoc.io/doc/io.github.kliushnichenko/jooby-mcp/latest)

## jooby-mcp

**jooby-mcp** integrates the [Model Context Protocol (MCP) Java SDK](https://github.com/modelcontextprotocol/java-sdk)
with the [Jooby](https://github.com/jooby-project/jooby) framework. It lets you expose **tools**, **prompts**, and **resources** to MCP clients using declarative, annotation-based APIs with discovery at **build time** via an annotation
processor, so no runtime reflection is required.

⭐ **If you find this project useful, consider giving it a star. It helps others discover it**

🚀 Quickstart, guides, and API reference available in the [docs](https://kliushnichenko.github.io/jooby-mcp/)

### Features

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
- Optional `MCP Inspector` module for local testing
