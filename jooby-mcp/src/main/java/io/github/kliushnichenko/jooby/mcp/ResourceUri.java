package io.github.kliushnichenko.jooby.mcp;

public record ResourceUri(String uri) {
    public static final String CTX_KEY = "__resourceUri";
}
