package io.github.kliushnichenko.jooby.mcp.apt.generator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.palantir.javapoet.*;
import io.github.kliushnichenko.jooby.mcp.annotation.ToolArg;
import io.github.kliushnichenko.jooby.mcp.apt.McpServerDescriptor;
import io.github.kliushnichenko.jooby.mcp.apt.tools.ToolEntry;
import io.github.kliushnichenko.jooby.mcp.internal.MethodInvoker;
import io.github.kliushnichenko.jsonschema.generator.JsonSchemaGenerator;
import io.modelcontextprotocol.json.jackson.JacksonMcpJsonMapper;
import io.modelcontextprotocol.spec.McpSchema;

import javax.lang.model.element.Modifier;
import java.util.HashMap;
import java.util.Map;

import static io.github.kliushnichenko.jooby.mcp.apt.generator.AnnotationMappers.MAPPERS;

class McpToolsFeature extends McpFeature {

    private static final String TOOL_INVOKERS_FIELD_NAME = "toolInvokers";
    private final JsonSchemaGenerator schemaGenerator = new JsonSchemaGenerator(MAPPERS); // todo: looks like only TOOL_ARG_MAPPER is used

    @Override
    public void generateFields(TypeSpec.Builder builder) {
        FieldSpec objectMapper = FieldSpec.builder(
                        ClassName.get(ObjectMapper.class),
                        "objectMapper",
                        Modifier.PRIVATE)
                .build();

        FieldSpec mcpJsonMapper = FieldSpec.builder(
                        ClassName.get(JacksonMcpJsonMapper.class),
                        "mcpJsonMapper",
                        Modifier.PRIVATE)
                .build();

        FieldSpec toolsField = FieldSpec.builder(
                        ParameterizedTypeName.get(Map.class, String.class, McpSchema.Tool.class),
                        "tools",
                        Modifier.PRIVATE, Modifier.FINAL)
                .initializer("new $T<>()", HashMap.class)
                .addJavadoc("Map of tool names to its specification.")
                .build();

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

        builder.addField(objectMapper);
        builder.addField(mcpJsonMapper);
        builder.addField(toolsField);
        builder.addField(toolInvokersField);
    }

    @Override
    public void generateInitializers(MethodSpec.Builder builder, McpServerDescriptor descriptor) {
        builder.addStatement("this.objectMapper = objectMapper");
        builder.addStatement("this.mcpJsonMapper = new $T(objectMapper)", ClassName.get(JacksonMcpJsonMapper.class));
        builder.addCode("\n");

        // fill tools map
        for (ToolEntry tool : descriptor.tools()) {
            String jsonSchema = schemaGenerator.generate(tool.method());
            builder.addStatement(
                    "tools.put($S, $T.builder().name($S).title($S).description($S).inputSchema(mcpJsonMapper, $S).build())",
                    tool.toolName(),
                    ClassName.get(McpSchema.Tool.class),
                    tool.toolName(),
                    tool.toolName(), // todo: add title support in annotations
                    tool.toolDescription(),
                    jsonSchema);
        }
        builder.addCode("\n");

        // fill tool invokers map
        for (ToolEntry entry : descriptor.tools()) {
            CodeBlock methodCall = buildMethodInvocation(entry.method(), entry.serviceClass(), ToolArg.class);
            var mapEntry = CodeBlock.of("$S, $L", entry.toolName(), methodCall);
            builder.addCode(CodeBlock.of("toolInvokers.put($L);\n", mapEntry));
        }
        builder.addCode("\n");
    }

    @Override
    public void generateInvoker(TypeSpec.Builder builder) {
        MethodSpec invokeMethod = MethodSpec.methodBuilder("invokeTool")
                .addModifiers(Modifier.PUBLIC)
                .addParameter(String.class, "toolName", Modifier.FINAL)
                .addParameter(ParameterizedTypeName.get(Map.class, String.class, Object.class), "args", Modifier.FINAL)
                .returns(Object.class)
                .addJavadoc("Invokes a tool by name with the provided arguments.\n")
                .addJavadoc("@param toolName the name of the tool to invoke\n")
                .addJavadoc("@param args the arguments to pass to the tool\n")
                .addJavadoc("@return the result of the tool invocation\n")
                .addStatement("$T invoker = $L.get(toolName)",
                        ClassName.get(MethodInvoker.class), TOOL_INVOKERS_FIELD_NAME)
                .addStatement("return invoker.invoke(args)")
                .build();

        builder.addMethod(invokeMethod);
    }

    @Override
    public void generateGetter(TypeSpec.Builder builder) {
        MethodSpec getSchemasMethod = MethodSpec.methodBuilder("getTools")
                .addModifiers(Modifier.PUBLIC)
                .returns(ParameterizedTypeName.get(Map.class, String.class, McpSchema.Tool.class))
                .addStatement("return tools")
                .build();

        builder.addMethod(getSchemasMethod);
    }
}
