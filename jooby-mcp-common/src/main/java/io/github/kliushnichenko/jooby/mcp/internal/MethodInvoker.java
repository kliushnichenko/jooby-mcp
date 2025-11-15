package io.github.kliushnichenko.jooby.mcp.internal;

import io.modelcontextprotocol.server.McpSyncServerExchange;

import java.util.Map;

/**
 * @author kliushnichenko
 */
@FunctionalInterface
public interface MethodInvoker {
    /**
     * Invokes a method with the provided arguments and exchange context.
     *
     * @param args     a map of argument names to values
     * @param exchange the server exchange context
     * @return the result of the method invocation
     */
    Object invoke(final Map<String, Object> args, McpSyncServerExchange exchange);
}
