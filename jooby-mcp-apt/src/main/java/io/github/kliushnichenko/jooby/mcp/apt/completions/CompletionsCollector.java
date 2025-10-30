package io.github.kliushnichenko.jooby.mcp.apt.completions;

import io.github.kliushnichenko.jooby.mcp.annotation.CompleteArg;
import io.github.kliushnichenko.jooby.mcp.annotation.CompletePrompt;
import io.github.kliushnichenko.jooby.mcp.apt.BaseMethodCollector;
import io.github.kliushnichenko.jooby.mcp.apt.util.ClassLiteral;

import javax.annotation.processing.Messager;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class CompletionsCollector extends BaseMethodCollector {

    private final Validator validator = new Validator();

    public CompletionsCollector(Messager messager, String defaultServerKey) {
        super(messager, defaultServerKey);
    }

    public List<CompletionEntry> collectCompletions(RoundEnvironment roundEnv, List<String> definedPrompts) {
        List<CompletionEntry> completions = new ArrayList<>();
        Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(CompletePrompt.class
                /*, CompleteResourceTemplate.class*/);

        for (Element element : elements) {
            if (validator.isValidMethod(element)) {
                CompletionEntry entry = buildCompletionEntry((ExecutableElement) element);

                if (validator.isValidPromptRef(entry.identifier(), definedPrompts, element)) {
                    completions.add(entry);
                }
            }
        }

        return completions;
    }

    private CompletionEntry buildCompletionEntry(ExecutableElement method) {
        TypeElement serviceClass = (TypeElement) method.getEnclosingElement();
        CompletePrompt promptAnnotation = method.getAnnotation(CompletePrompt.class);
        VariableElement param = method.getParameters().get(0);
        CompleteArg argAnnotation = param.getAnnotation(CompleteArg.class);

        String argName = param.getSimpleName().toString();
        if (argAnnotation != null) {
            if (!argAnnotation.name().isEmpty()) {
                argName = argAnnotation.name();
            }
        }

        return new CompletionEntry(
                promptAnnotation.value(),
                argName,
                "ref/prompt", // todo: for resources 'ref/resource'
                extractServerKey(method, serviceClass),
                serviceClass,
                method);
    }

    class Validator {

        private static final List<String> ALLOWED_RETURN_TYPES = List.of(
                "io.modelcontextprotocol.spec.McpSchema.CompleteResult",
                "io.modelcontextprotocol.spec.McpSchema.CompleteResult$CompleteCompletion",
                "java.util.List<java.lang.String>",
                ClassLiteral.STRING
        );

        boolean isValidMethod(Element element) {
            ExecutableElement method = (ExecutableElement) element;
            if (!isPublicMethod(method)) {
                return false;
            }

            if (!isValidReturnType(method)) {
                return false;
            }

            return isValidArgument(method);
        }

        private boolean isValidReturnType(ExecutableElement method) {
            var returnType = method.getReturnType().toString();
            if (!ALLOWED_RETURN_TYPES.contains(returnType)) {
                var msg = String.format("Invalid return type: %s. Supported return types are: %s",
                        returnType, ALLOWED_RETURN_TYPES);
                reportError(msg, method);
                return false;
            }
            return true;
        }

        private boolean isValidArgument(ExecutableElement method) {
            var methodName = method.getSimpleName().toString();
            var params = method.getParameters();
            var paramsCount = params.size();

            if (paramsCount != 1) {
                var msg = String.format("Method '%s' must have exactly one argument", methodName);
                reportError(msg, method);
                return false;
            }

            String paramType = params.get(0).asType().toString();

            if (!ClassLiteral.STRING.equals(paramType)) {
                var msg = String.format("Method '%s' must have a single String argument, but found: %s",
                        methodName, paramType);
                reportError(msg, method);
                return false;
            }
            return true;
        }

        boolean isValidPromptRef(String promptRef, List<String> definedPrompts, Element element) {
            if (!definedPrompts.contains(promptRef)) {
                var msg = String.format("No such prompt found '%s' at method '%s'. Please verify the prompt reference.",
                        promptRef, element.getSimpleName().toString());
                reportError(msg, element);
                return false;
            }
            return true;
        }
    }
}
