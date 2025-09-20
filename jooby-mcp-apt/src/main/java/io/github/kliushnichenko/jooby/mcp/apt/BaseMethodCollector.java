package io.github.kliushnichenko.jooby.mcp.apt;

import io.github.kliushnichenko.jooby.mcp.annotation.McpServer;

import javax.annotation.processing.Messager;
import javax.lang.model.element.*;
import javax.tools.Diagnostic;

public class BaseMethodCollector {

    protected String defaultServerKey;
    protected final Messager messager;
    protected Class<?> methodAnnotation;
    protected String annotationName;

    public BaseMethodCollector(Messager messager,
                               Class<?> methodAnnotation,
                               String defaultServerKey) {
        this.messager = messager;
        this.methodAnnotation = methodAnnotation;
        this.defaultServerKey = defaultServerKey;
        this.annotationName = "@" + methodAnnotation.getSimpleName();
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

    protected boolean isValidMethod(Element element) {
        ExecutableElement method = (ExecutableElement) element;
        if (!method.getModifiers().contains(Modifier.PUBLIC)) {
            reportError(annotationName + " annotated methods must be public", element);
            return false;
        }

        return true;
    }

    protected void reportError(String message, Element element) {
        messager.printMessage(Diagnostic.Kind.ERROR, message, element);
    }
}
