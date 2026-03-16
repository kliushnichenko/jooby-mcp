---
title: "Prompts"
description: "Expose prompt templates with @Prompt and @PromptArg. Optional prompt completions for suggested argument values."
type: docs
weight: 4
---

Prompts expose template-style inputs to MCP clients (e.g. for LLM prompts). Annotate methods with **@Prompt**. You can optionally add **prompt completions** so clients get suggested values for arguments as the user types.

## Defining a prompt

```java
import io.github.kliushnichenko.jooby.mcp.annotation.Prompt;
import io.github.kliushnichenko.jooby.mcp.annotation.PromptArg;

@Singleton
public class PromptsExample {

    @Prompt(name = "summarizeText", description = "Summarizes the provided text into a specified number of sentences")
    public String summarizeText(
            @PromptArg(name = "text") String text,
            String maxSentences
    ) {
        return String.format("""
                Please provide a clear and concise summary of the following text in no more than %s sentences:
                %s
                """, maxSentences, text);
    }
}
```

- **@Prompt** — Registers the method as a prompt. Name and description can be inferred or set explicitly.
- **@PromptArg** — Describes parameters for the generated schema and client UX.

## Prompt completions

Prompt completions let clients get suggested values for a prompt’s arguments while the user is filling the form. Add a separate method annotated with **@CompletePrompt** that returns suggestions for a given argument and partial input.

```java
import io.github.kliushnichenko.jooby.mcp.annotation.CompleteArg;
import io.github.kliushnichenko.jooby.mcp.annotation.CompletePrompt;
import io.github.kliushnichenko.jooby.mcp.annotation.Prompt;

@Singleton
public class PromptCompletionsExample {

    private static final List<String> SUPPORTED_LANGUAGES =
            List.of("Java", "Python", "JavaScript", "Go", "TypeScript");

    @Prompt(name = "code_review", description = "Code Review Prompt")
    public String codeReviewPrompt(String codeSnippet, String language) {
        return """
                You are a senior software engineer tasked with reviewing the following %s code snippet:
                %s
                Please provide feedback on:
                1. Code readability and maintainability.
                2. Potential bugs or issues.
                3. Suggestions for improvement.
                """.formatted(language, codeSnippet);
    }

    @CompletePrompt("code_review")
    public List<String> completeCodeReviewLang(@CompleteArg(name = "language") String partialInput) {
        return SUPPORTED_LANGUAGES.stream()
                .filter(lang -> lang.toLowerCase().contains(partialInput.toLowerCase()))
                .toList();
    }
}
```

- **@CompletePrompt("code_review")** — Ties this method to the prompt named `code_review`.
- **@CompleteArg(name = "language")** — This method completes the `language` argument. It receives the current partial input and returns a list of suggestions.
