package io.github.kliushnichenko.jooby.mcp.internal;

import java.util.Map;

@FunctionalInterface
public interface MethodInvoker {
    /**
     * Invokes a method with the provided arguments.
     *
     * @param args a map of argument names to values
     * @return the result of the method invocation
     */
    Object invoke(final Map<String, Object> args);
}
