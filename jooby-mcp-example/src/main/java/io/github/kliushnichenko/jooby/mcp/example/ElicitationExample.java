package io.github.kliushnichenko.jooby.mcp.example;

import io.github.kliushnichenko.jooby.mcp.annotation.Tool;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.inject.Singleton;

import java.util.Map;

/**
 * @author kliushnichenko
 */
@Singleton
public class ElicitationExample {

    @Tool(name = "elicitation_example", description = "Request the username over elicitation")
    public String requestUsername(McpSyncServerExchange exchange) {
        if (exchange.getClientCapabilities().elicitation() == null) {
            return "Client does not support elicitation capabilities";
        }

        McpSchema.ElicitRequest request = McpSchema.ElicitRequest.builder()
                .message("Please provide your GitHub username")
                .requestedSchema(
                        Map.of("type", "object", "properties", Map.of("name", Map.of("type", "string"))))
                .build();

        McpSchema.ElicitResult result = exchange.createElicitation(request);

        if (McpSchema.ElicitResult.Action.ACCEPT == result.action()) {
            String username = result.content().get("name").toString();
            return "Your GitHub username is: " + username;
        } else {
            return "You " + result.action().name() + " elicitation request";
        }
    }
}
