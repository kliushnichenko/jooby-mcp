package io.github.kliushnichenko.jooby.mcp.apt.generator;

import com.palantir.javapoet.*;
import io.github.kliushnichenko.jooby.mcp.annotation.ToolArg;
import io.github.kliushnichenko.jooby.mcp.apt.McpServerDescriptor;
import io.github.kliushnichenko.jooby.mcp.apt.tools.ToolEntry;
import io.github.kliushnichenko.jooby.mcp.internal.MethodInvoker;
import io.github.kliushnichenko.jooby.mcp.internal.ToolSpec;
import io.github.kliushnichenko.jsonschema.generator.JsonSchemaGenerator;
import io.github.kliushnichenko.jsonschema.model.JsonSchemaObj;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.server.McpSyncServerExchange;

import javax.lang.model.element.Modifier;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.github.kliushnichenko.jooby.mcp.apt.generator.AnnotationMappers.MAPPERS;

/**
 * @author kliushnichenko
 */
class McpToolsFeature extends McpFeature {

    private final JsonSchemaGenerator schemaGenerator = new JsonSchemaGenerator(MAPPERS);

    @Override
    public void generateFields(TypeSpec.Builder builder) {
        FieldSpec objectMapper = FieldSpec.builder(
                        ClassName.get(McpJsonMapper.class),
                        "mcpJsonMapper",
                        Modifier.PRIVATE)
                .build();

        FieldSpec toolsField = FieldSpec.builder(
                        ParameterizedTypeName.get(Map.class, String.class, ToolSpec.class),
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
                        "toolInvokers",
                        Modifier.PRIVATE, Modifier.FINAL)
                .initializer("new $T<>()", HashMap.class)
                .addJavadoc("Map of tool names to method invokers.")
                .build();

        builder.addField(objectMapper);
        builder.addField(toolsField);
        builder.addField(toolInvokersField);
    }

    @Override
    @SuppressWarnings("PMD.NcssCount")
    public void generateInitializers(MethodSpec.Builder methodBuilder, McpServerDescriptor descriptor) {
        methodBuilder.addStatement("this.mcpJsonMapper = mcpJsonMapper");
        methodBuilder.addCode("\n");

        // fill tools map
        for (ToolEntry tool : descriptor.tools()) {
            JsonSchemaObj jsonSchemaObj = schemaGenerator.generateAsObject(tool.method(), IGNORE_TYPES);
            String inputSchema = JsonSchemaGenerator.serializeSchemaObj(jsonSchemaObj);

            String outputSchema = null;
            if (tool.outputType() != null) {
                outputSchema = schemaGenerator.generate(tool.outputType());
            }
            CodeBlock requiredArgs = buildRequiredArguments(jsonSchemaObj.getRequired());

            CodeBlock.Builder newToolBlock = CodeBlock.builder()
                    .add("tools.put($S, $T.builder().name($S)",
                            tool.toolName(),
                            ClassName.get(ToolSpec.class),
                            tool.toolName()
                    );

            addIfNotNull(tool.toolTitle(), newToolBlock, ".title($S)");
            addIfNotNull(tool.toolDescription(), newToolBlock, ".description($S)");
            addIfNotNull(inputSchema, newToolBlock, ".inputSchema($S)");
            addIfNotNull(outputSchema, newToolBlock, ".outputSchema($S)");
            addIfNotNull(requiredArgs, newToolBlock, ".requiredArguments($L)");

            newToolBlock.add(".build());");
            methodBuilder.addCode(newToolBlock.build()).addCode("\n");
        }
        methodBuilder.addCode("\n");

        populateInvokersMap(methodBuilder, descriptor);
    }

    private void populateInvokersMap(MethodSpec.Builder methodBuilder, McpServerDescriptor descriptor) {
        for (ToolEntry entry : descriptor.tools()) {
            CodeBlock methodCall = buildMethodInvocation(entry.method(), entry.serviceClass(), ToolArg.class);
            var mapEntry = CodeBlock.of("$S, $L", entry.toolName(), methodCall);
            methodBuilder.addCode(CodeBlock.of("toolInvokers.put($L);\n", mapEntry));
        }
        methodBuilder.addCode("\n");
    }

    private CodeBlock buildRequiredArguments(List<String> requiredArgs) {
        CodeBlock.Builder codeBlock = CodeBlock.builder();
        codeBlock.add("$T.of(", List.class);
        for (int i = 0; i < requiredArgs.size(); i++) {
            if (i > 0) {
                codeBlock.add(", ");
            }
            codeBlock.add("$S", requiredArgs.get(i));
        }
        codeBlock.add(")");
        return codeBlock.build();
    }

    @Override
    public void generateInvoker(TypeSpec.Builder builder) {
        MethodSpec invokeMethod = MethodSpec.methodBuilder("invokeTool")
                .addModifiers(Modifier.PUBLIC)
                .addParameter(String.class, "toolName", Modifier.FINAL)
                .addParameter(ParameterizedTypeName.get(Map.class, String.class, Object.class), "args", Modifier.FINAL)
                .addParameter(McpSyncServerExchange.class, "exchange", Modifier.FINAL)
                .returns(Object.class)
                .addJavadoc("""
                        Invokes a tool by name with the provided arguments.
                        @param toolName the name of the tool to invoke
                        @param args the arguments to pass to the tool
                        @return the result of the tool invocation
                        """)
                .addStatement("$T invoker = toolInvokers.get(toolName)", ClassName.get(MethodInvoker.class))
                .addStatement("return invoker.invoke(args, exchange)")
                .build();

        builder.addMethod(invokeMethod);
    }

    @Override
    public void generateGetter(TypeSpec.Builder builder) {
        MethodSpec getter = MethodSpec.methodBuilder("getTools")
                .addModifiers(Modifier.PUBLIC)
                .returns(ParameterizedTypeName.get(Map.class, String.class, ToolSpec.class))
                .addStatement("return tools")
                .build();

        builder.addMethod(getter);
    }

    @Override
    boolean hasItems(McpServerDescriptor descriptor) {
        return !descriptor.tools().isEmpty();
    }
}
