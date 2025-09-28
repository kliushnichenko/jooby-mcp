package io.github.kliushnichenko.jooby.mcp.internal;

import java.util.List;

public class ToolSpec {
    private String name;
    private String title;
    private String description;
    private String inputSchema;
    private List<String> requiredArguments;

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final ToolSpec toolSpec;

        public Builder() {
            this.toolSpec = new ToolSpec();
        }

        public Builder name(String name) {
            toolSpec.setName(name);
            return this;
        }

        public Builder title(String title) {
            toolSpec.setTitle(title);
            return this;
        }

        public Builder description(String description) {
            toolSpec.setDescription(description);
            return this;
        }

        public Builder inputSchema(String inputSchema) {
            toolSpec.setInputSchema(inputSchema);
            return this;
        }

        public Builder requiredArguments(List<String> requiredArguments) {
            toolSpec.setRequiredArguments(requiredArguments);
            return this;
        }

        public ToolSpec build() {
            return toolSpec;
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getInputSchema() {
        return inputSchema;
    }

    public void setInputSchema(String inputSchema) {
        this.inputSchema = inputSchema;
    }

    public List<String> getRequiredArguments() {
        return requiredArguments;
    }

    public void setRequiredArguments(List<String> requiredArguments) {
        this.requiredArguments = requiredArguments;
    }
}
