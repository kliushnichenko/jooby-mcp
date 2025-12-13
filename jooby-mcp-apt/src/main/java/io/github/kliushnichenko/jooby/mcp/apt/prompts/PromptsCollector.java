package io.github.kliushnichenko.jooby.mcp.apt.prompts;

import io.github.kliushnichenko.jooby.mcp.annotation.Prompt;
import io.github.kliushnichenko.jooby.mcp.annotation.PromptArg;
import io.github.kliushnichenko.jooby.mcp.apt.BaseMethodCollector;
import lombok.experimental.UtilityClass;

import javax.annotation.processing.Messager;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author kliushnichenko
 */
public class PromptsCollector extends BaseMethodCollector {

    public PromptsCollector(Messager messager, String defaultServerKey) {
        super(messager, defaultServerKey);
    }

    public List<PromptEntry> collectPrompts(RoundEnvironment roundEnv) {
        List<PromptEntry> prompts = new ArrayList<>();
        Set<? extends Element> promptElements = roundEnv.getElementsAnnotatedWith(Prompt.class);

        for (Element element : promptElements) {
            if (isValidMethod(element)) {
                PromptEntry promptEntry = buildPromptEntry((ExecutableElement) element);
                prompts.add(promptEntry);
            }
        }

        return prompts;
    }

    private boolean isValidMethod(Element element) {
        ExecutableElement method = (ExecutableElement) element;
        return isPublicMethod(method);
    }

    private PromptEntry buildPromptEntry(ExecutableElement method) {
        TypeElement serviceClass = (TypeElement) method.getEnclosingElement();
        Prompt promptAnnotation = method.getAnnotation(Prompt.class);

        return new PromptEntry(
                extractPromptName(method, promptAnnotation),
                promptAnnotation.title(),
                promptAnnotation.description(),
                collectPromptArgs(method),
                extractServerKey(method, serviceClass),
                serviceClass,
                method);
    }

    private List<PromptEntry.Arg> collectPromptArgs(ExecutableElement method) {
        List<PromptEntry.Arg> args = new ArrayList<>();
        for (VariableElement methodArg : method.getParameters()) {
            PromptArg argAnnotation = methodArg.getAnnotation(PromptArg.class);
            String name = ArgExtractor.extractName(methodArg, argAnnotation);
            String description = ArgExtractor.extractDescription(argAnnotation);
            boolean required = ArgExtractor.extractRequired(argAnnotation);

            args.add(new PromptEntry.Arg(name, description, required));
        }
        return args;
    }

    private String extractPromptName(ExecutableElement method, Prompt annotation) {
        String name = annotation.name();
        return name.isEmpty() ? method.getSimpleName().toString() : name;
    }

    @UtilityClass
    static class ArgExtractor {

        public static String extractName(VariableElement element, PromptArg arg) {
            if (arg != null && !arg.name().isEmpty()) {
                return arg.name();
            }
            // Fallback to actual parameter name if @PromptArg name is not specified
            return element.getSimpleName().toString();
        }

        public static String extractDescription(PromptArg arg) {
            if (arg != null && !arg.description().isEmpty()) {
                return arg.description();
            }
            return null;
        }

        public static boolean extractRequired(PromptArg arg) {
            return arg == null || arg.required();
        }
    }
}
