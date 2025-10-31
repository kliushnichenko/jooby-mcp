package io.github.kliushnichenko.jooby.mcp.apt;

import io.github.kliushnichenko.jooby.mcp.annotation.PromptArg;
import io.github.kliushnichenko.jooby.mcp.annotation.ToolArg;
import lombok.experimental.UtilityClass;

import javax.lang.model.element.VariableElement;
import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.function.Function;

@UtilityClass
public final class ArgNameExtractor {

    private static final Map<Class<?>, Function<VariableElement, String>> EXTRACTORS = Map.of(
            ToolArg.class, argument -> {
                ToolArg toolArg = argument.getAnnotation(ToolArg.class);
                if (toolArg != null && !toolArg.name().isEmpty()) {
                    return toolArg.name();
                }
                return argument.getSimpleName().toString();
            },
            PromptArg.class, argument -> {
                PromptArg promptArg = argument.getAnnotation(PromptArg.class);
                if (promptArg != null && !promptArg.name().isEmpty()) {
                    return promptArg.name();
                }
                return argument.getSimpleName().toString();
            }
    );

    public static String extractName(VariableElement argument, Class<? extends Annotation> annotationClass) {
        return EXTRACTORS.getOrDefault(annotationClass, var -> var.getSimpleName().toString())
                .apply(argument);
    }
}
