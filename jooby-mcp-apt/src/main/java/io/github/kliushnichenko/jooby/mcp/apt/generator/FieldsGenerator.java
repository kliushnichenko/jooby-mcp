package io.github.kliushnichenko.jooby.mcp.apt.generator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.FieldSpec;
import com.palantir.javapoet.ParameterizedTypeName;
import com.palantir.javapoet.TypeSpec;
import io.github.kliushnichenko.jooby.mcp.internal.MethodInvoker;
import io.modelcontextprotocol.spec.McpSchema;

import javax.lang.model.element.Modifier;
import java.util.HashMap;
import java.util.Map;

import static io.github.kliushnichenko.jooby.mcp.apt.generator.McpServerGenerator.PROMPT_INVOKERS_FIELD_NAME;
import static io.github.kliushnichenko.jooby.mcp.apt.generator.McpServerGenerator.TOOL_INVOKERS_FIELD_NAME;

class FieldsGenerator {

    public static void generateFields(TypeSpec.Builder builder) {
        FieldSpec joobyServices = FieldSpec.builder(
                        ClassName.get("io.jooby", "ServiceRegistry"),
                        "services",
                        Modifier.PRIVATE)
                .build();

        FieldSpec objectMapper = FieldSpec.builder(
                        ClassName.get(ObjectMapper.class),
                        "objectMapper",
                        Modifier.PRIVATE)
                .build();

        FieldSpec toolsField = FieldSpec.builder(
                        ParameterizedTypeName.get(Map.class, String.class, McpSchema.Tool.class),
                        "tools",
                        Modifier.PRIVATE, Modifier.FINAL)
                .initializer("new $T<>()", HashMap.class)
                .addJavadoc("Map of tool names to its specification.")
                .build();

        FieldSpec promptsField = FieldSpec.builder(
                        ParameterizedTypeName.get(Map.class, String.class, McpSchema.Prompt.class),
                        "prompts",
                        Modifier.PRIVATE, Modifier.FINAL)
                .initializer("new $T<>()", HashMap.class)
                .addJavadoc("Map of prompt names to its specification.")
                .build();

        // Invokers field
        FieldSpec toolInvokersField = FieldSpec.builder(
                        ParameterizedTypeName.get(
                                ClassName.get(Map.class),
                                ClassName.get(String.class),
                                ClassName.get(MethodInvoker.class)
                        ),
                        TOOL_INVOKERS_FIELD_NAME,
                        Modifier.PRIVATE, Modifier.FINAL)
                .initializer("new $T<>()", HashMap.class)
                .addJavadoc("Map of tool names to method invokers.")
                .build();

        FieldSpec promptInvokersField = FieldSpec.builder(
                        ParameterizedTypeName.get(
                                ClassName.get(Map.class),
                                ClassName.get(String.class),
                                ClassName.get(MethodInvoker.class)
                        ),
                        PROMPT_INVOKERS_FIELD_NAME,
                        Modifier.PRIVATE, Modifier.FINAL)
                .initializer("new $T<>()", HashMap.class)
                .addJavadoc("Map of prompt names to method invokers.")
                .build();

        builder.addField(joobyServices);
        builder.addField(objectMapper);
        builder.addField(toolsField);
        builder.addField(promptsField);
        builder.addField(toolInvokersField);
        builder.addField(promptInvokersField);
    }
}
