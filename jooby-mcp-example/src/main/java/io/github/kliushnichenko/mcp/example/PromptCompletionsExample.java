package io.github.kliushnichenko.mcp.example;

import io.github.kliushnichenko.jooby.mcp.annotation.CompleteArg;
import io.github.kliushnichenko.jooby.mcp.annotation.CompletePrompt;
import io.github.kliushnichenko.jooby.mcp.annotation.Prompt;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.inject.Singleton;

import java.util.List;


@Singleton
public class PromptCompletionsExample {

    private static final List<String> SUPPORTED_LANGUAGES = List.of("Java", "Python", "JavaScript", "Go", "TypeScript");
    private static final List<String> SCRUTINY_LEVELS = List.of("light", "moderate", "high", "extreme");

    @Prompt(name = "code_review", description = "Code Review Prompt")
    public String codeReviewPrompt(String codeSnippet, String language, String scrutinyLevel) {
        return """
                You are a senior software engineer tasked with reviewing the following %s code snippet.
                Apply a %s level of scrutiny during the review:
                   
                %s
                   
                Please provide feedback on:
                1. Code readability and maintainability.
                2. Potential bugs or issues.
                3. Suggestions for improvement.
                 """.formatted(language, scrutinyLevel, codeSnippet);
    }

    @CompletePrompt("code_review")
    public McpSchema.CompleteResult completeCodeReviewLang(@CompleteArg(name = "language") String partialInput) {
        var values = SUPPORTED_LANGUAGES.stream()
                .filter(lang -> lang.toLowerCase().contains(partialInput.toLowerCase()))
                .toList();
        var completion = new McpSchema.CompleteResult.CompleteCompletion(values, values.size(), false);
        return new McpSchema.CompleteResult(completion);
    }

    @CompletePrompt("code_review")
    public List<String> completeScrutinyLevel(@CompleteArg(name = "scrutinyLevel") String input) {
        return SCRUTINY_LEVELS.stream()
                .filter(lang -> lang.toLowerCase().contains(input.toLowerCase()))
                .toList();
    }
}
