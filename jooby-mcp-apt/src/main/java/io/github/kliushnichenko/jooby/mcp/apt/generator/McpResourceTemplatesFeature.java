package io.github.kliushnichenko.jooby.mcp.apt.generator;

import com.palantir.javapoet.*;
import io.github.kliushnichenko.jooby.mcp.ResourceUri;
import io.github.kliushnichenko.jooby.mcp.apt.McpServerDescriptor;
import io.github.kliushnichenko.jooby.mcp.apt.resourcetemplates.ResourceTemplateEntry;
import io.github.kliushnichenko.jooby.mcp.apt.util.ClassLiteral;
import io.modelcontextprotocol.spec.McpSchema;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * @author kliushnichenko
 */
public class McpResourceTemplatesFeature extends McpFeature {

    @Override
    void generateFields(TypeSpec.Builder builder) {
        FieldSpec resourcesTmplField = FieldSpec.builder(
                        ParameterizedTypeName.get(List.class, McpSchema.ResourceTemplate.class),
                        "resourceTemplates",
                        Modifier.PRIVATE, Modifier.FINAL)
                .initializer("new $T<>()", ArrayList.class)
                .addJavadoc("List of resource templates.")
                .build();

        var function = ParameterizedTypeName.get(
                ClassName.get(Function.class),
                ParameterizedTypeName.get(Map.class, String.class, Object.class),
                ClassName.get(Object.class)
        );
        FieldSpec resourceTmplReadersField = FieldSpec.builder(
                        ParameterizedTypeName.get(
                                ClassName.get(Map.class),
                                ClassName.get(String.class),
                                function
                        ),
                        "resourceTemplateReaders",
                        Modifier.PRIVATE, Modifier.FINAL)
                .initializer("new $T<>()", HashMap.class)
                .addJavadoc("Map of resource URI template to method invoker.")
                .build();

        builder.addField(resourcesTmplField);
        builder.addField(resourceTmplReadersField);
    }

    @Override
    void generateInitializers(MethodSpec.Builder methodBuilder, McpServerDescriptor descriptor) {
        for (ResourceTemplateEntry tmpl : descriptor.resourceTemplates()) {
            CodeBlock resAnnotations = buildResourceAnnotations(tmpl.annotations());

            CodeBlock.Builder newTemplateBlock = CodeBlock.builder()
                    .add("resourceTemplates.add($T.builder().name($S)",
                            ClassName.get(McpSchema.ResourceTemplate.class),
                            tmpl.name()
                    );

            addIfNotNull(tmpl.title(), newTemplateBlock, ".title($S)");
            addIfNotNull(tmpl.description(), newTemplateBlock, ".description($S)");
            addIfNotNull(tmpl.uriTemplate(), newTemplateBlock, ".uriTemplate($S)");
            addIfNotNull(tmpl.mimeType(), newTemplateBlock, ".mimeType($S)");
            addIfNotNull(resAnnotations, newTemplateBlock, ".annotations($L)");

            newTemplateBlock.add(".build());");
            methodBuilder.addCode(newTemplateBlock.build()).addCode("\n");
        }
        methodBuilder.addCode("\n");

        // fill resource template readers map
        for (ResourceTemplateEntry entry : descriptor.resourceTemplates()) {
            CodeBlock methodCall = buildMethodInvocation(entry.method(), entry.serviceClass());
            var mapEntry = CodeBlock.of("$S, $L", entry.uriTemplate(), methodCall);
            methodBuilder.addCode(CodeBlock.of("resourceTemplateReaders.put($L);\n", mapEntry));
        }
        methodBuilder.addCode("\n");
    }

    protected CodeBlock buildMethodInvocation(ExecutableElement method, TypeElement serviceClass) {
        List<? extends VariableElement> parameters = method.getParameters();

        ClassName serviceClassName = ClassName.get(serviceClass);
        CodeBlock.Builder methodCall = CodeBlock.builder();
        methodCall.add("(args) -> app.require($T.class).$L(", serviceClassName, method.getSimpleName());

        for (int i = 0; i < parameters.size(); i++) {
            if (i > 0) {
                methodCall.add(", ");
            }
            CodeBlock parameterCast;
            if (isResourceUri(parameters.get(i))) {
                parameterCast = CodeBlock.of("new $T((String) args.get($S))", ResourceUri.class, ResourceUri.CTX_KEY);
            } else {
                String parameterName = parameters.get(i).getSimpleName().toString();
                parameterCast = CodeBlock.of("(String) args.get($S)", parameterName);
            }
            methodCall.add("$L", parameterCast);
        }

        methodCall.add(")");
        return methodCall.build();
    }

    private boolean isResourceUri(VariableElement param) {
        String paramType = param.asType().toString();
        return ClassLiteral.RESOURCE_URI.equals(paramType);
    }

    @Override
    void generateInvoker(TypeSpec.Builder builder) {
        MethodSpec invokeMethod = MethodSpec.methodBuilder("readResourceByTemplate")
                .addModifiers(Modifier.PUBLIC)
                .addParameter(String.class, "uriTemplate", Modifier.FINAL)
                .addParameter(ParameterizedTypeName.get(Map.class, String.class, Object.class), "args", Modifier.FINAL)
                .returns(Object.class)
                .addJavadoc("""
                        Reads a resource by URI according to template
                        @param uriTemplate Resource URI template
                        @return resource content
                        """)
                .addCode("""
                        var reader = resourceTemplateReaders.get(uriTemplate);
                        return reader.apply(args);
                        """)
                .build();

        builder.addMethod(invokeMethod);
    }

    @Override
    void generateGetter(TypeSpec.Builder builder) {
        MethodSpec getter = MethodSpec.methodBuilder("getResourceTemplates")
                .addModifiers(Modifier.PUBLIC)
                .returns(ParameterizedTypeName.get(List.class, McpSchema.ResourceTemplate.class))
                .addStatement("return resourceTemplates")
                .build();

        builder.addMethod(getter);
    }

    @Override
    boolean hasItems(McpServerDescriptor descriptor) {
        return !descriptor.resourceTemplates().isEmpty();
    }
}
