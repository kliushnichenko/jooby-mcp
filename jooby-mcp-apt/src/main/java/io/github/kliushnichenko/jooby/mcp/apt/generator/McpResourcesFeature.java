package io.github.kliushnichenko.jooby.mcp.apt.generator;

import com.palantir.javapoet.*;
import io.github.kliushnichenko.jooby.mcp.apt.McpServerDescriptor;
import io.github.kliushnichenko.jooby.mcp.apt.resources.ResourceEntry;
import io.modelcontextprotocol.spec.McpSchema;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class McpResourcesFeature extends McpFeature {
    @Override
    void generateFields(TypeSpec.Builder builder) {
        FieldSpec resourcesField = FieldSpec.builder(
                        ParameterizedTypeName.get(List.class, McpSchema.Resource.class),
                        "resources",
                        Modifier.PRIVATE, Modifier.FINAL)
                .initializer("new $T<>()", ArrayList.class)
                .addJavadoc("List of resources.")
                .build();

        FieldSpec resourceReadersField = FieldSpec.builder(
                        ParameterizedTypeName.get(
                                ClassName.get(Map.class),
                                ClassName.get(String.class),
                                ParameterizedTypeName.get(
                                        ClassName.get(Supplier.class),
                                        ClassName.get(Object.class))
                        ),
                        "resourceReaders",
                        Modifier.PRIVATE, Modifier.FINAL)
                .initializer("new $T<>()", HashMap.class)
                .addJavadoc("Map of resource URI to method invoker.")
                .build();

        builder.addField(resourcesField);
        builder.addField(resourceReadersField);
    }

    @Override
    void generateInitializers(MethodSpec.Builder methodBuilder, McpServerDescriptor descriptor) {
        for (ResourceEntry resource : descriptor.resources()) {
            CodeBlock resAnnotations = buildResourceAnnotations(resource.annotations());

            CodeBlock.Builder newResourceBlock = CodeBlock.builder()
                    .add("resources.add($T.builder().name($S)",
                            ClassName.get(McpSchema.Resource.class),
                            resource.name()
                    );

            addIfNotNull(resource.title(), newResourceBlock, ".title($S)");
            addIfNotNull(resource.description(), newResourceBlock, ".description($S)");
            addIfNotNull(resource.uri(), newResourceBlock, ".uri($S)");
            addIfNotNull(resource.mimeType(), newResourceBlock, ".mimeType($S)");
            addIfPositive(resource.size(), newResourceBlock, ".size(Long.valueOf($L))");
            addIfNotNull(resAnnotations, newResourceBlock, ".annotations($L)");

            newResourceBlock.add(".build());");
            methodBuilder.addCode(newResourceBlock.build()).addCode("\n");
        }
        methodBuilder.addCode("\n");

        // fill resource readers map
        for (ResourceEntry entry : descriptor.resources()) {
            CodeBlock methodCall = buildMethodInvocation(entry.method(), entry.serviceClass());
            var mapEntry = CodeBlock.of("$S, $L", entry.uri(), methodCall);
            methodBuilder.addCode(CodeBlock.of("resourceReaders.put($L);\n", mapEntry));
        }
        methodBuilder.addCode("\n");
    }

    private static CodeBlock buildResourceAnnotations(ResourceEntry.Annotations annotations) {
        if (annotations != null) {
            CodeBlock.Builder audienceList = CodeBlock.builder().add("List.of(");
            var roles = annotations.audience();
            for (int i = 0; i < roles.length; i++) {
                if (i > 0) {
                    audienceList.add(", ");
                }
                audienceList.add("$T.$L", McpSchema.Role.class, roles[i].name());
            }
            audienceList.add(")");

            return CodeBlock.of("new $T($L, $L, $S)",
                    ClassName.get(McpSchema.Annotations.class),
                    audienceList.build(),
                    annotations.priority(),
                    annotations.lastModified()
            );
        }
        return null;
    }

    protected CodeBlock buildMethodInvocation(ExecutableElement method, TypeElement serviceClass) {
        return CodeBlock.of("() -> app.require($T.class).$L()", ClassName.get(serviceClass), method.getSimpleName());
    }

    @Override
    void generateInvoker(TypeSpec.Builder builder) {
        MethodSpec invokeMethod = MethodSpec.methodBuilder("readResource")
                .addModifiers(Modifier.PUBLIC)
                .addParameter(String.class, "uri", Modifier.FINAL)
                .returns(Object.class)
                .addJavadoc("""
                        Reads a resource by URI
                        @param uri Resource URI
                        @return resource content
                        """)
                .addCode("""
                        var reader = resourceReaders.get(uri);
                        return reader.get();
                        """)
                .build();

        builder.addMethod(invokeMethod);
    }

    @Override
    void generateGetter(TypeSpec.Builder builder) {
        MethodSpec getter = MethodSpec.methodBuilder("getResources")
                .addModifiers(Modifier.PUBLIC)
                .returns(ParameterizedTypeName.get(List.class, McpSchema.Resource.class))
                .addStatement("return resources")
                .build();

        builder.addMethod(getter);
    }
}
