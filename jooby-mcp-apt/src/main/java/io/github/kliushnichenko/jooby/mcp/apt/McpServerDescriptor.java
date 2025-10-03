package io.github.kliushnichenko.jooby.mcp.apt;

import io.github.kliushnichenko.jooby.mcp.apt.completions.CompletionEntry;
import io.github.kliushnichenko.jooby.mcp.apt.prompts.PromptEntry;
import io.github.kliushnichenko.jooby.mcp.apt.tools.ToolEntry;

import java.util.List;

public record McpServerDescriptor(String serverKey,
                                  String targetPackage,
                                  List<ToolEntry> tools,
                                  List<PromptEntry> prompts,
                                  List<CompletionEntry> completions) {
}
