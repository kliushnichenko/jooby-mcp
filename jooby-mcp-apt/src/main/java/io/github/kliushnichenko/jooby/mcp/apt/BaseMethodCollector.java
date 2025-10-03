package io.github.kliushnichenko.jooby.mcp.apt;

import io.github.kliushnichenko.jooby.mcp.annotation.McpServer;

import javax.annotation.processing.Messager;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;

public class BaseMethodCollector {

    protected String defaultServerKey;
    protected final Messager messager;

    public BaseMethodCollector(Messager messager, String defaultServerKey) {
        this.messager = messager;
        this.defaultServerKey = defaultServerKey;
    }

    protected String extractServerKey(ExecutableElement method, TypeElement serviceClass) {
        McpServer mcpServer = serviceClass.getAnnotation(McpServer.class);

        if (mcpServer == null) {
            // Check if the method itself has the McpServer annotation
            mcpServer = method.getAnnotation(McpServer.class);
        }

        if (mcpServer == null) {
            return defaultServerKey;
        } else {
            String serverName = mcpServer.value();
            if (serverName.isEmpty()) {
                return defaultServerKey;
            } else {
                return serverName;
            }
        }
    }

    protected boolean isPublicMethod(ExecutableElement method) {
        if (!method.getModifiers().contains(Modifier.PUBLIC)) {
            reportError(method.getSimpleName().toString() + " method must be public", method);
            return false;
        }

        return true;
    }

    protected void reportError(String message, Element element) {
        messager.printMessage(Diagnostic.Kind.ERROR, message, element);
    }
}
