package io.github.kliushnichenko.jooby.mcp.apt.tools;

import io.github.kliushnichenko.jooby.mcp.annotation.OutputSchema;
import io.github.kliushnichenko.jooby.mcp.annotation.Tool;
import io.github.kliushnichenko.jooby.mcp.apt.AnnMirrorUtils;
import io.github.kliushnichenko.jooby.mcp.apt.BaseMethodCollector;
import io.modelcontextprotocol.spec.McpSchema;

import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeMirror;
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

    private final ProcessingEnvironment processingEnv;

    private static final Set<String> RESERVED_RETURN_TYPES = Set.of(
            McpSchema.CallToolResult.class.getCanonicalName(),
            McpSchema.Content.class.getCanonicalName(),
            McpSchema.TextContent.class.getCanonicalName(),
            McpSchema.ImageContent.class.getCanonicalName(),
            McpSchema.AudioContent.class.getCanonicalName(),
            McpSchema.EmbeddedResource.class.getCanonicalName(),
            McpSchema.ResourceLink.class.getCanonicalName(),
            String.class.getCanonicalName()
    );

    public ToolsCollector(ProcessingEnvironment processingEnv, String defaultServerKey) {
        super(processingEnv.getMessager(), defaultServerKey);
        this.processingEnv = processingEnv;
    }

    public List<ToolEntry> collectTools(RoundEnvironment roundEnv) {
        List<ToolEntry> toolEntries = new ArrayList<>();

        Set<? extends Element> toolElements = roundEnv.getElementsAnnotatedWith(Tool.class);

        for (Element element : toolElements) {
            if (isValidMethod(element)) {
                ExecutableElement method = (ExecutableElement) element;
                TypeElement serviceClass = (TypeElement) method.getEnclosingElement();
                Tool toolAnnotation = method.getAnnotation(Tool.class);
                TypeMirror outputType = evalOutputType(method);
                McpSchema.ToolAnnotations toolAnnotations = evalToolAnnotations(method, toolAnnotation);

                toolEntries.add(new ToolEntry(
                        extractToolName(method, toolAnnotation),
                        toNullIfEmpty(toolAnnotation.title()),
                        toNullIfEmpty(toolAnnotation.description()),
                        outputType,
                        toolAnnotations,
                        extractServerKey(method, serviceClass),
                        serviceClass,
                        method)
                );
            }
        }

        return toolEntries;
    }

    private McpSchema.ToolAnnotations evalToolAnnotations(ExecutableElement method, Tool toolAnnotation) {
        McpSchema.ToolAnnotations toolAnnotations = null;
        if (hasNonDefaultToolAnnotations(method)) {
            Tool.Annotations annotations = toolAnnotation.annotations();
            toolAnnotations = new McpSchema.ToolAnnotations(
                    toNullIfEmpty(annotations.title()),
                    annotations.readOnlyHint(),
                    annotations.destructiveHint(),
                    annotations.idempotentHint(),
                    annotations.openWorldHint(),
                    null
            );
        }
        return toolAnnotations;
    }

    private boolean hasNonDefaultToolAnnotations(ExecutableElement method) {
        return AnnMirrorUtils.findAnnotationMirror(method, Tool.class)
                .map(annMirror -> AnnMirrorUtils.hasProperty(annMirror, "annotations"))
                .orElse(false);
    }

    private boolean isValidMethod(Element element) {
        ExecutableElement method = (ExecutableElement) element;
        return isPublicMethod(method);
    }

    private String extractToolName(ExecutableElement method, Tool annotation) {
        String name = annotation.name();
        return name.isEmpty() ? method.getSimpleName().toString() : name;
    }

    private TypeMirror evalOutputType(ExecutableElement method) {
        var isSuppressed = method.getAnnotation(OutputSchema.Suppressed.class) != null;
        if (isSuppressed) {
            return null;
        }

        TypeMirror typeMirror = getOutputTypeMirror(method);

        if (typeMirror != null) {
            return typeMirror;
        } else {
            typeMirror = method.getReturnType();
            if (RESERVED_RETURN_TYPES.contains(typeMirror.toString())) {
                return null;
            }
            return typeMirror;
        }
    }

    private TypeMirror getOutputTypeMirror(ExecutableElement method) {
        OutputSchema.From scalarAnnotation = method.getAnnotation(OutputSchema.From.class);
        if (scalarAnnotation != null) {
            return evalOutputByClass(scalarAnnotation);
        }

        OutputSchema.ArrayOf arrayAnnotation = method.getAnnotation(OutputSchema.ArrayOf.class);
        if (arrayAnnotation != null) {
            return evalOutputArrayByClass(arrayAnnotation);
        }

        OutputSchema.MapOf mapAnnotation = method.getAnnotation(OutputSchema.MapOf.class);
        if (mapAnnotation != null) {
            return evalOutputMapByClass(mapAnnotation);
        }

        return null;
    }

    private TypeMirror evalOutputMapByClass(OutputSchema.MapOf mapAnnotation) {
        TypeMirror typeMirror = null;
        try {
            @SuppressWarnings({"checkstyle:UnusedLocalVariable", "PMD.UnusedLocalVariable"})
            Class<?> clazz = mapAnnotation.value();
        } catch (MirroredTypeException mte) {
            var strTypeMirror = processingEnv.getElementUtils()
                    .getTypeElement("java.lang.String")
                    .asType();
            typeMirror = processingEnv.getTypeUtils()
                    .getDeclaredType(
                            processingEnv.getElementUtils().getTypeElement("java.util.Map"),
                            strTypeMirror,
                            mte.getTypeMirror()
                    );
        }
        return typeMirror;
    }

    private TypeMirror evalOutputArrayByClass(OutputSchema.ArrayOf arrayAnnotation) {
        TypeMirror typeMirror = null;
        try {
            @SuppressWarnings({"checkstyle:UnusedLocalVariable", "PMD.UnusedLocalVariable"})
            Class<?> clazz = arrayAnnotation.value();
        } catch (MirroredTypeException mte) {
            typeMirror = processingEnv.getTypeUtils()
                    .getDeclaredType(
                            processingEnv.getElementUtils().getTypeElement("java.util.List"),
                            mte.getTypeMirror()
                    );
        }
        return typeMirror;
    }

    private TypeMirror evalOutputByClass(OutputSchema.From annotation) {
        TypeMirror typeMirror = null;
        try {
            @SuppressWarnings({"checkstyle:UnusedLocalVariable", "PMD.UnusedLocalVariable"})
            Class<?> clazz = annotation.value();
        } catch (MirroredTypeException mte) {
            typeMirror = mte.getTypeMirror();
        }
        return typeMirror;
    }
}
