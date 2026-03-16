---
title: "Supported Return Types"
description: "Return types supported for tools, prompts, and resources. String, McpSchema types, and POJOs."
type: docs
weight: 10
---

This page lists the return types supported for each kind of handler. Use these so the annotation processor and runtime can serialize and expose results correctly.

## Tools

- `String`
- `McpSchema.CallToolResult`
- `McpSchema.Content`
- `McpSchema.TextContent`
- POJO (serialized to JSON)

## Prompts

- `McpSchema.GetPromptResult`
- `McpSchema.PromptMessage`
- `List<McpSchema.PromptMessage>`
- `McpSchema.Content`
- `String`
- POJO (string representation via `toString()`)

## Resources and Resource Templates

- `McpSchema.ReadResourceResult`
- `McpSchema.ResourceContents`
- `List<McpSchema.ResourceContents>`
- `McpSchema.TextResourceContents`
- `McpSchema.BlobResourceContents`
- POJO (serialized to JSON)

## Completions

- `McpSchema.CompleteResult`
- `McpSchema.CompleteResult.CompleteCompletion`
- `List<String>`
- `String`