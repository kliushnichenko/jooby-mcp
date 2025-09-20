package io.github.kliushnichenko.jooby.mcp.apt.generator;

import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterizedTypeName;
import com.palantir.javapoet.TypeSpec;
import io.modelcontextprotocol.spec.McpSchema;

import javax.lang.model.element.Modifier;
import java.util.Map;

class GettersGenerator {

    static void addToolsGetter(TypeSpec.Builder builder) {
        MethodSpec getSchemasMethod = MethodSpec.methodBuilder("getTools")
                .addModifiers(Modifier.PUBLIC)
                .returns(ParameterizedTypeName.get(Map.class, String.class, McpSchema.Tool.class))
                .addStatement("return tools")
                .build();

        builder.addMethod(getSchemasMethod);
    }

    static void addPromptsGetter(TypeSpec.Builder builder) {
        MethodSpec getSchemasMethod = MethodSpec.methodBuilder("getPrompts")
                .addModifiers(Modifier.PUBLIC)
                .returns(ParameterizedTypeName.get(Map.class, String.class, McpSchema.Prompt.class))
                .addStatement("return prompts")
                .build();

        builder.addMethod(getSchemasMethod);
    }

    static void addServerKeyGetter(TypeSpec.Builder builder, String serverKey) {
        MethodSpec getServerKeyMethod = MethodSpec.methodBuilder("getServerKey")
                .addModifiers(Modifier.PUBLIC)
                .returns(String.class)
                .addStatement("return $S", serverKey)
                .build();

        builder.addMethod(getServerKeyMethod);
    }
}
