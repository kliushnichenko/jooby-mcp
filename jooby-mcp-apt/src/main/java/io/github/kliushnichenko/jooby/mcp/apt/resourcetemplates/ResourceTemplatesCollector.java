package io.github.kliushnichenko.jooby.mcp.apt.resourcetemplates;

import io.github.kliushnichenko.jooby.mcp.annotation.Resource;
import io.github.kliushnichenko.jooby.mcp.annotation.ResourceTemplate;
import io.github.kliushnichenko.jooby.mcp.apt.AnnMirrorUtils;
import io.github.kliushnichenko.jooby.mcp.apt.BaseMethodCollector;
import io.github.kliushnichenko.jooby.mcp.apt.resources.ResourceEntry;
import io.github.kliushnichenko.jooby.mcp.apt.util.ClassLiteral;
import io.modelcontextprotocol.util.DefaultMcpUriTemplateManager;

import javax.annotation.processing.Messager;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ResourceTemplatesCollector extends BaseMethodCollector {

    private final Validator validator = new Validator();

    public ResourceTemplatesCollector(Messager messager, String defaultServerKey) {
        super(messager, defaultServerKey);
    }

    public List<ResourceTemplateEntry> collectResourceTemplates(RoundEnvironment roundEnv) {
        List<ResourceTemplateEntry> templates = new ArrayList<>();
        Set<? extends Element> elems = roundEnv.getElementsAnnotatedWith(ResourceTemplate.class);

        for (Element method : elems) {
            ResourceTemplateEntry entry = buildResourceTemplateEntry((ExecutableElement) method);
            if (validator.isValidMethod(method, entry)) {
                templates.add(entry);
            }
        }

        return templates;
    }

    private ResourceTemplateEntry buildResourceTemplateEntry(ExecutableElement method) {
        TypeElement serviceClass = (TypeElement) method.getEnclosingElement();
        ResourceTemplate annotation = method.getAnnotation(ResourceTemplate.class);

        ResourceEntry.Annotations resourceEntryAnns = null;
        if (hasNonDefaultResourceAnnotations(method)) {
            Resource.Annotations annotations = annotation.annotations();
            resourceEntryAnns = new ResourceEntry.Annotations(
                    annotations.audience(),
                    annotations.priority(),
                    toNullIfEmpty(annotations.lastModified())
            );
        }

        return new ResourceTemplateEntry(
                extractResourceTemplateName(method, annotation),
                toNullIfEmpty(annotation.title()),
                toNullIfEmpty(annotation.description()),
                annotation.uriTemplate(),
                toNullIfEmpty(annotation.mimeType()),
                resourceEntryAnns,
                extractServerKey(method, serviceClass),
                serviceClass,
                method
        );
    }

    private boolean hasNonDefaultResourceAnnotations(ExecutableElement method) {
        return AnnMirrorUtils.findAnnotationMirror(method, ResourceTemplate.class)
                .map(annMirror -> AnnMirrorUtils.hasProperty(annMirror, "annotations"))
                .orElse(false);
    }

    private String extractResourceTemplateName(ExecutableElement method, ResourceTemplate annotation) {
        String name = annotation.name();
        return name.isEmpty() ? method.getSimpleName().toString() : name;
    }

    class Validator {

        boolean isValidMethod(Element element, ResourceTemplateEntry templateEntry) {
            ExecutableElement method = (ExecutableElement) element;
            DefaultMcpUriTemplateManager manager = new DefaultMcpUriTemplateManager(templateEntry.uriTemplate());
            List<String> templateVars = manager.getVariableNames();

            return hasTemplateVars(method, templateEntry, templateVars) &&
                   argsAreValid(method, templateEntry, templateVars) &&
                   isPublicMethod(method);
        }

        private boolean hasTemplateVars(ExecutableElement method,
                                        ResourceTemplateEntry templateEntry,
                                        List<String> templateVars) {
            if (templateVars.isEmpty()) {
                var msg = String.format(
                        "URI template '%s' must contain at least one variable.",
                        templateEntry.uriTemplate()
                );
                reportError(msg, method);
                return false;
            }
            return true;
        }

        private boolean argsAreValid(ExecutableElement method,
                                     ResourceTemplateEntry templateEntry,
                                     List<String> templateVars) {
            boolean isValid = true;
            for (VariableElement param : method.getParameters()) {
                var paramTypeMirror = param.asType();
                if (!isValidArgType(paramTypeMirror)) {
                    var msg = String.format(
                            "Method has invalid parameter type: %s. Only String and ResourceUri are allowed.",
                            param.asType().toString()
                    );
                    reportError(msg, method);
                    isValid = false;
                    break;
                }

                if (!isResourceUri(paramTypeMirror) && !paramIsPresentInTemplate(param, templateVars)) {
                    var msg = String.format(
                            "Method parameter '%s' does not match any variable in URI template '%s'.",
                            param.getSimpleName().toString(), templateEntry.uriTemplate()
                    );
                    reportError(msg, method);
                    isValid = false;
                    break;
                }
            }
            return isValid;
        }

        private boolean paramIsPresentInTemplate(VariableElement param, List<String> templateVars) {
            return templateVars.contains(param.getSimpleName().toString());
        }

        private boolean isResourceUri(TypeMirror typeMirror) {
            return ClassLiteral.RESOURCE_URI.equals(typeMirror.toString());
        }

        private boolean isValidArgType(TypeMirror typeMirror) {
            return ClassLiteral.STRING.equals(typeMirror.toString()) || isResourceUri(typeMirror);
        }
    }
}
