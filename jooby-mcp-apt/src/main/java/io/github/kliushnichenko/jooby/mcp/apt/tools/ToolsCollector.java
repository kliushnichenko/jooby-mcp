package io.github.kliushnichenko.jooby.mcp.apt.tools;

import io.github.kliushnichenko.jooby.mcp.annotation.Tool;
import io.github.kliushnichenko.jooby.mcp.apt.BaseMethodCollector;

import javax.annotation.processing.Messager;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Responsible for collecting and validating @Tool annotated methods.
 *
 * <p>This class handles the discovery phase of annotation processing, finding all
 * methods annotated with @Tool.</p>
 *
 * @author kliushnichenko
 */
public class ToolsCollector extends BaseMethodCollector {

    public ToolsCollector(Messager messager, String defaultServerKey) {
        super(messager, defaultServerKey);
    }

    public List<ToolEntry> collectTools(RoundEnvironment roundEnv) {
        List<ToolEntry> toolEntries = new ArrayList<>();

        Set<? extends Element> toolElements = roundEnv.getElementsAnnotatedWith(Tool.class);

        for (Element element : toolElements) {
            if (isValidMethod(element)) {
                ExecutableElement method = (ExecutableElement) element;
                TypeElement serviceClass = (TypeElement) method.getEnclosingElement();
                Tool toolAnnotation = method.getAnnotation(Tool.class);

                String toolName = extractToolName(method, toolAnnotation);
                String toolDescription = toolAnnotation.description();
                String serverKey = extractServerKey(method, serviceClass);

                toolEntries.add(new ToolEntry(toolName, toolDescription, serverKey, serviceClass, method));
            }
        }

        return toolEntries;
    }

    private boolean isValidMethod(Element element) {
        ExecutableElement method = (ExecutableElement) element;
        return isPublicMethod(method);
    }

    private String extractToolName(ExecutableElement method, Tool annotation) {
        String name = annotation.name();
        return name.isEmpty() ? method.getSimpleName().toString() : name;
    }
}
