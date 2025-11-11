package io.github.kliushnichenko.jooby.mcp.apt.completions;

import io.github.kliushnichenko.jooby.mcp.annotation.CompleteArg;
import io.github.kliushnichenko.jooby.mcp.annotation.CompletePrompt;
import io.github.kliushnichenko.jooby.mcp.annotation.CompleteResourceTemplate;
import io.github.kliushnichenko.jooby.mcp.apt.BaseMethodCollector;
import io.github.kliushnichenko.jooby.mcp.apt.resourcetemplates.ResourceTemplateEntry;
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

/**
 * @author kliushnichenko
 */
public class CompletionsCollector extends BaseMethodCollector {

    private final Validator validator = new Validator();

    public CompletionsCollector(Messager messager, String defaultServerKey) {
        super(messager, defaultServerKey);
    }

    public List<CompletionEntry> collectCompletions(RoundEnvironment roundEnv,
                                                    List<String> definedPrompts,
                                                    List<ResourceTemplateEntry> resourceTemplates) {
        List<CompletionEntry> completions = new ArrayList<>();
        Set<? extends Element> elements = roundEnv.getElementsAnnotatedWithAny(
                Set.of(CompletePrompt.class, CompleteResourceTemplate.class)
        );

        for (Element element : elements) {
            var method = (ExecutableElement) element;

            if (validator.isValidMethod(method)) {
                try {
                    CompletionEntry entry = buildCompletionEntry(method, definedPrompts, resourceTemplates);
                    completions.add(entry);
                } catch (InvalidCompletionReferenceException ex) {
                    reportError(ex.getMessage(), method);
                }
            }
        }

        return completions;
    }

    private CompletionEntry buildCompletionEntry(ExecutableElement method,
                                                 List<String> definedPrompts,
                                                 List<ResourceTemplateEntry> resourceTemplates) {
        TypeElement serviceClass = (TypeElement) method.getEnclosingElement();
        VariableElement param = method.getParameters().get(0);
        CompleteArg argAnnotation = param.getAnnotation(CompleteArg.class);

        String argName = param.getSimpleName().toString();
        if (argAnnotation != null && !argAnnotation.name().isEmpty()) {
            argName = argAnnotation.name();
        }

        return new CompletionEntry(
                resolveCompletionReference(method, definedPrompts, resourceTemplates),
                argName,
                resolveCompletionType(method),
                extractServerKey(method, serviceClass),
                serviceClass,
                method);
    }

    private String resolveCompletionReference(ExecutableElement method,
                                              List<String> definedPrompts,
                                              List<ResourceTemplateEntry> resourceTemplates) {
        CompletePrompt promptAnnotation = method.getAnnotation(CompletePrompt.class);
        if (promptAnnotation != null) {
            var promptRef = promptAnnotation.value();
            if (!definedPrompts.contains(promptRef)) {
                var msg = String.format("No such prompt found '%s' at method '%s'. Please verify the prompt reference.",
                        promptRef, method.getSimpleName().toString());
                throw new InvalidCompletionReferenceException(msg);
            }

            return promptRef;
        } else {
            var refName = method.getAnnotation(CompleteResourceTemplate.class).value();
            var templateOptional = resourceTemplates.stream()
                    .filter(templateEntry -> refName.equals(templateEntry.name()))
                    .findFirst();

            if (templateOptional.isEmpty()) {
                var msg = String.format("No such resource found '%s' at method '%s'. " +
                                        "Please verify resource template reference.",
                        refName, method.getSimpleName().toString());
                throw new InvalidCompletionReferenceException(msg);
            }

            return templateOptional.get().uriTemplate();
        }
    }

    private CompletionEntry.Type resolveCompletionType(ExecutableElement method) {
        CompletePrompt promptAnnotation = method.getAnnotation(CompletePrompt.class);
        if (promptAnnotation != null) {
            return CompletionEntry.Type.PROMPT;
        } else {
            return CompletionEntry.Type.RESOURCE;
        }
    }

    class Validator {

        private static final int MAX_ARGUMENTS = 1;

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

            if (paramsCount != MAX_ARGUMENTS) {
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
    }
}
