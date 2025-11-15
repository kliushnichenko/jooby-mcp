package io.github.kliushnichenko.jooby.mcp.internal;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * @author kliushnichenko
 */
@Getter
@Setter
@Builder
public class ToolSpec {
    private String name;
    private String title;
    private String description;
    private String inputSchema;
    private List<String> requiredArguments;
}
