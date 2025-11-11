package io.github.kliushnichenko.jooby.mcp.apt.resources;

import io.github.kliushnichenko.jooby.mcp.annotation.Resource;
import io.github.kliushnichenko.jooby.mcp.apt.AnnMirrorUtils;
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
 * @author kliushnichenko
 */
public class ResourcesCollector extends BaseMethodCollector {

    public ResourcesCollector(Messager messager, String defaultServerKey) {
        super(messager, defaultServerKey);
    }

    public List<ResourceEntry> collectResources(RoundEnvironment roundEnv) {
        List<ResourceEntry> resources = new ArrayList<>();
        Set<? extends Element> elems = roundEnv.getElementsAnnotatedWith(Resource.class);

        for (Element method : elems) {
            if (isValidMethod(method)) {
                ResourceEntry resourceEntry = buildResourceEntry((ExecutableElement) method);
                resources.add(resourceEntry);
            }
        }

        return resources;
    }

    private ResourceEntry buildResourceEntry(ExecutableElement method) {
        TypeElement serviceClass = (TypeElement) method.getEnclosingElement();
        Resource annotation = method.getAnnotation(Resource.class);

        ResourceEntry.Annotations resourceEntryAnns = null;
        if (hasNonDefaultResourceAnnotations(method)) {
            Resource.Annotations annotations = annotation.annotations();
            resourceEntryAnns = new ResourceEntry.Annotations(
                    annotations.audience(),
                    annotations.priority(),
                    toNullIfEmpty(annotations.lastModified())
            );
        }

        return new ResourceEntry(
                extractResourceName(method, annotation),
                toNullIfEmpty(annotation.title()),
                toNullIfEmpty(annotation.description()),
                annotation.uri(),
                toNullIfEmpty(annotation.mimeType()),
                annotation.size(),
                resourceEntryAnns,
                extractServerKey(method, serviceClass),
                serviceClass,
                method
        );
    }

    private boolean hasNonDefaultResourceAnnotations(ExecutableElement method) {
        return AnnMirrorUtils.findAnnotationMirror(method, Resource.class)
                .map(annMirror -> AnnMirrorUtils.hasProperty(annMirror, "annotations"))
                .orElse(false);
    }

    private boolean isValidMethod(Element element) {
        ExecutableElement method = (ExecutableElement) element;

        if (!method.getParameters().isEmpty()) {
            reportError("Resource method must not have arguments", method);
            return false;
        }

        return isPublicMethod(method);
    }

    private String extractResourceName(ExecutableElement method, Resource annotation) {
        String name = annotation.name();
        return name.isEmpty() ? method.getSimpleName().toString() : name;
    }
}
