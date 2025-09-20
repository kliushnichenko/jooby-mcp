package io.github.kliushnichenko.jooby.mcp.apt.generator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.palantir.javapoet.*;
import io.github.kliushnichenko.jooby.mcp.annotation.PromptArg;
import io.github.kliushnichenko.jooby.mcp.annotation.ToolArg;
import io.github.kliushnichenko.jooby.mcp.apt.ArgNameExtractor;
import io.github.kliushnichenko.jooby.mcp.apt.McpServerDescriptor;
import io.github.kliushnichenko.jooby.mcp.apt.prompts.PromptEntry;
import io.github.kliushnichenko.jooby.mcp.apt.tools.ToolEntry;
import io.github.kliushnichenko.jsonschema.generator.JsonSchemaGenerator;
import io.github.kliushnichenko.jsonschema.model.JsonSchemaAnnotationMapper;
import io.github.kliushnichenko.jsonschema.model.JsonSchemaProps;
import io.modelcontextprotocol.spec.McpSchema;

import javax.annotation.processing.Filer;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Map;

public class McpServerGenerator {

    private static final String MCP_SERVER_CLASS_NAME = "McpServer";
    static final String TOOL_INVOKERS_FIELD_NAME = "toolInvokers";
    static final String PROMPT_INVOKERS_FIELD_NAME = "promptInvokers";

    private final Filer filer;
    private final JsonSchemaGenerator schemaGenerator;

    private static final JsonSchemaAnnotationMapper<ToolArg> TOOL_ARG_MAPPER = (ToolArg annotation) -> {
        JsonSchemaProps schemaProps = new JsonSchemaProps();
        schemaProps.setName(annotation.name());
        schemaProps.setDescription(annotation.description());
        schemaProps.setRequired(annotation.required());
//        schemaArg.setDefaultValue(annotation.defaultValue()); //todo: handle default values casting
        return schemaProps;
    };

    private static final JsonSchemaAnnotationMapper<PromptArg> PROMPT_ARG_MAPPER = (PromptArg annotation) -> {
        JsonSchemaProps schemaProps = new JsonSchemaProps();
        schemaProps.setName(annotation.name());
        schemaProps.setDescription(annotation.description());
        schemaProps.setRequired(annotation.required());
//        schemaArg.setDefaultValue(annotation.defaultValue()); //todo: handle default values casting
        return schemaProps;
    };

    private static final Map<Class<? extends Annotation>, JsonSchemaAnnotationMapper<?>> MAPPERS = Map.of(
            ToolArg.class, TOOL_ARG_MAPPER,
            PromptArg.class, PROMPT_ARG_MAPPER
    );

    public McpServerGenerator(Filer filer) {
        this(filer, new JsonSchemaGenerator(MAPPERS));
    }

    /**
     * Creates a new McpServerGenerator with specified dependencies.
     *
     * @param filer           the filer for writing generated files
     * @param schemaGenerator the JSON schema generator to use
     */
    public McpServerGenerator(Filer filer, JsonSchemaGenerator schemaGenerator) {
        this.filer = filer;
        this.schemaGenerator = schemaGenerator;
    }

    public void generateMcpServers(List<McpServerDescriptor> serverDescriptors) throws IOException {
        for (McpServerDescriptor serverDescriptor : serverDescriptors) {
            generateMcpServer(serverDescriptor);
        }
    }

    private void generateMcpServer(McpServerDescriptor serverDescriptor) throws IOException {
        String className = capitalize(serverDescriptor.serverKey()) + MCP_SERVER_CLASS_NAME;

        TypeSpec.Builder mcpServerBuilder = TypeSpec.classBuilder(className)
                .addModifiers(Modifier.PUBLIC)
                .addSuperinterface(
                        ClassName.get("io.github.kliushnichenko.jooby.mcp", "JoobyMcpServer")
                )
                .addJavadoc("Generated Jooby MCP Server. Do not modify manually.");

        // Build all components
        FieldsGenerator.generateFields(mcpServerBuilder);

        addInitMethod(mcpServerBuilder, serverDescriptor);

        InvokersGenerator.generateInvokers(mcpServerBuilder);
        GettersGenerator.addToolsGetter(mcpServerBuilder);
        GettersGenerator.addPromptsGetter(mcpServerBuilder);
        GettersGenerator.addServerKeyGetter(mcpServerBuilder, serverDescriptor.serverKey());

        writeJavaFile(mcpServerBuilder.build(), serverDescriptor.targetPackage());
    }

    /**
     * Adds the init method to the server.
     * This method initializes the tools and their invokers.
     */
    private void addInitMethod(TypeSpec.Builder serverBuilder, McpServerDescriptor descriptor) {
        MethodSpec.Builder initMethodBuilder = MethodSpec.methodBuilder("init")
                .addModifiers(Modifier.PUBLIC)
                .addParameter(ClassName.get("io.jooby", "Jooby"), "app", Modifier.FINAL)
                .addParameter(ClassName.get(ObjectMapper.class), "objectMapper", Modifier.FINAL)
                .addJavadoc("Initialize a new server.")
                .addJavadoc("@param app the Jooby application instance")
                .addJavadoc("@param objectMapper json serializer instance");

        initMethodBuilder.addStatement("this.services = app.getServices()");
        initMethodBuilder.addStatement("this.objectMapper = objectMapper");
        initMethodBuilder.addCode("\n");

        fillToolsMap(initMethodBuilder, descriptor.tools());
        initMethodBuilder.addCode("\n");
        fillPromptsMap(initMethodBuilder, descriptor.prompts());
        initMethodBuilder.addCode("\n");

        fillToolInvokersMap(initMethodBuilder, descriptor.tools());
        initMethodBuilder.addCode("\n");
        fillPromptInvokersMap(initMethodBuilder, descriptor.prompts());

        serverBuilder.addMethod(initMethodBuilder.build());
    }

    private void fillToolsMap(MethodSpec.Builder methodBuilder, List<ToolEntry> tools) {
        for (ToolEntry tool : tools) {
            String jsonSchema = schemaGenerator.generate(tool.method());
            methodBuilder.addStatement(
                    "tools.put($S, new $T($S, $S, $S))",
                    tool.toolName(),
                    ClassName.get(McpSchema.Tool.class),
                    tool.toolName(),
                    tool.toolDescription(),
                    jsonSchema);
        }
    }

    private void fillPromptsMap(MethodSpec.Builder methodBuilder, List<PromptEntry> prompts) {
        for (PromptEntry prompt : prompts) {
            CodeBlock promptArguments = buildPromptArgs(prompt.promptArgs());

            methodBuilder.addStatement(
                    "prompts.put($S, new $T($S, $S, $L))",
                    prompt.promptName(),
                    ClassName.get(McpSchema.Prompt.class),
                    prompt.promptName(),
                    prompt.promptDescription(),
                    promptArguments);
        }
    }

    private CodeBlock buildPromptArgs(List<PromptEntry.Arg> promptArgs) {
        CodeBlock.Builder argsBuilder = CodeBlock.builder();
        argsBuilder.add("$T.of(", ClassName.get(List.class));
        for (int i = 0; i < promptArgs.size(); i++) {
            if (i > 0) {
                argsBuilder.add(", ");
            }
            argsBuilder.add("new $T(", ClassName.get(McpSchema.PromptArgument.class));
            PromptEntry.Arg arg = promptArgs.get(i);
            argsBuilder.add("$S, $S, $L", arg.name(), arg.description(), arg.required());
            argsBuilder.add(")");
        }
        argsBuilder.add(")");
        return argsBuilder.build();
    }

    private void fillToolInvokersMap(MethodSpec.Builder methodBuilder, List<ToolEntry> toolEntries) {
        for (ToolEntry entry : toolEntries) {
            CodeBlock methodCall = buildMethodInvocation(entry.method(), entry.serviceClass(), ToolArg.class);
            var mapEntry = CodeBlock.of("$S, $L", entry.toolName(), methodCall);
            methodBuilder.addCode(CodeBlock.of("toolInvokers.put($L);\n", mapEntry));
        }
    }

    private void fillPromptInvokersMap(MethodSpec.Builder methodBuilder, List<PromptEntry> promptEntries) {
        for (PromptEntry entry : promptEntries) {
            CodeBlock methodCall = buildMethodInvocation(entry.method(), entry.serviceClass(), PromptArg.class);
            var mapEntry = CodeBlock.of("$S, $L", entry.promptName(), methodCall);
            methodBuilder.addCode(CodeBlock.of("promptInvokers.put($L);\n", mapEntry));
        }
    }

    /**
     * Builds a method invocation lambda expression.
     */
    private CodeBlock buildMethodInvocation(ExecutableElement method,
                                            TypeElement serviceClass,
                                            Class<? extends Annotation> annotationClass) {
        List<? extends VariableElement> parameters = method.getParameters();

        ClassName serviceClassName = ClassName.get(serviceClass);
        CodeBlock.Builder methodCall = CodeBlock.builder();
        methodCall.add("(args) -> (services.get($T.class)).$L(", serviceClassName, method.getSimpleName());

        for (int i = 0; i < parameters.size(); i++) {
            if (i > 0) {
                methodCall.add(", ");
            }

            VariableElement param = parameters.get(i);
            String parameterName = ArgNameExtractor.extractName(param, annotationClass);
            CodeBlock parameterCast = ParameterTypeHandler.buildParameterCast(param, parameterName);
            methodCall.add("$L", parameterCast);
        }

        methodCall.add(")");
        return methodCall.build();
    }

    private void writeJavaFile(TypeSpec serverClass, String targetPackage) throws IOException {
        JavaFile javaFile = JavaFile.builder(targetPackage, serverClass)
                .addFileComment("This file is generated by McpToolProcessor. Do not modify manually.")
                .build();

        javaFile.writeTo(filer);
    }

    private String capitalize(String str) {
        str = str.toLowerCase();
        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
    }
} 