package io.github.kliushnichenko.jooby.mcp.apt.generator;

import com.palantir.javapoet.*;
import io.github.kliushnichenko.jooby.mcp.JoobyMcpServer;
import io.github.kliushnichenko.jooby.mcp.apt.McpServerDescriptor;
import io.modelcontextprotocol.json.McpJsonMapper;

import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Modifier;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

/**
 * @author kliushnichenko
 */
public class McpServerGenerator {

    private static final String MCP_SERVER_CLASS_NAME = "McpServer";
    private final Filer filer;
    private final Messager messager;

    private static final List<McpFeature> FEATURES = List.of(
            new McpToolsFeature(),
            new McpPromptsFeature(),
            new McpCompletionsFeature(),
            new McpResourcesFeature(),
            new McpResourceTemplatesFeature()
    );

    public McpServerGenerator(ProcessingEnvironment processingEnv) {
        this.filer = processingEnv.getFiler();
        this.messager = processingEnv.getMessager();
    }

    public void generateMcpServers(List<McpServerDescriptor> serverDescriptors) throws IOException {
        for (McpServerDescriptor serverDescriptor : serverDescriptors) {
            String fileLocation = generateMcpServer(serverDescriptor);
            printSummary(serverDescriptor, fileLocation);
        }
    }

    /**
     * Generates a Jooby MCP server class based on the provided descriptor.
     *
     * @param serverDescriptor the descriptor containing server details
     * @return the file location as a string
     * @throws IOException if an I/O error occurs
     */
    private String generateMcpServer(McpServerDescriptor serverDescriptor) throws IOException {
        String className = capitalize(serverDescriptor.serverKey()) + MCP_SERVER_CLASS_NAME;

        TypeSpec.Builder mcpServerBuilder = TypeSpec.classBuilder(className)
                .addModifiers(Modifier.PUBLIC)
                .addSuperinterface(ClassName.get(JoobyMcpServer.class))
                .addJavadoc("Generated Jooby MCP Server. Do not modify manually.");

        generateFields(mcpServerBuilder);
        generateInitMethod(mcpServerBuilder, serverDescriptor);
        generateInvokers(mcpServerBuilder);
        generateGetters(mcpServerBuilder, serverDescriptor.serverKey());

        return writeJavaFile(mcpServerBuilder.build(), serverDescriptor.targetPackage());
    }

    void generateFields(TypeSpec.Builder builder) {
        FieldSpec joobyApp = FieldSpec.builder(
                        ClassName.get("io.jooby", "Jooby"),
                        "app",
                        Modifier.PRIVATE)
                .build();
        builder.addField(joobyApp);

        FEATURES.forEach(feature -> feature.generateFields(builder));
    }

    /**
     * Adds the init method to the server.
     * This method initializes the tools, prompts, resources and their invokers.
     */
    private void generateInitMethod(TypeSpec.Builder serverBuilder, McpServerDescriptor descriptor) {
        MethodSpec.Builder initMethodBuilder = MethodSpec.methodBuilder("init")
                .addModifiers(Modifier.PUBLIC)
                .addParameter(ClassName.get("io.jooby", "Jooby"), "app", Modifier.FINAL)
                .addParameter(ClassName.get(McpJsonMapper.class), "mcpJsonMapper", Modifier.FINAL)
                .addJavadoc("""
                        Initialize a new server.
                        @param app the Jooby application instance
                        @param mcpJsonMapper json serializer instance
                        """);

        initMethodBuilder.addStatement("this.app = app");

        FEATURES.stream()
                .filter(mcpFeature -> mcpFeature.hasItems(descriptor))
                .forEach(feature -> feature.generateInitializers(initMethodBuilder, descriptor));

        serverBuilder.addMethod(initMethodBuilder.build());
    }

    private void generateInvokers(TypeSpec.Builder builder) {
        FEATURES.forEach(feature -> feature.generateInvoker(builder));
    }

    private void generateGetters(TypeSpec.Builder builder, String serverKey) {
        FEATURES.forEach(feature -> feature.generateGetter(builder));

        MethodSpec getServerKeyMethod = MethodSpec.methodBuilder("getServerKey")
                .addModifiers(Modifier.PUBLIC)
                .returns(String.class)
                .addStatement("return $S", serverKey)
                .build();

        builder.addMethod(getServerKeyMethod);
    }

    /**
     * Writes the generated Java file to the FS.
     *
     * @param serverClass   the TypeSpec representing the server class
     * @param targetPackage the target package name
     * @return the file location as a string
     * @throws IOException if an I/O error occurs
     */
    private String writeJavaFile(TypeSpec serverClass, String targetPackage) throws IOException {
        JavaFile javaFile = JavaFile.builder(targetPackage, serverClass)
                .addFileComment("This file is generated by McpToolProcessor. Do not modify manually.")
                .build();

        javaFile.writeTo(filer);
        return targetPackage + "." + serverClass.name();
    }

    private void printSummary(McpServerDescriptor descriptor, String fileLocation) {
        StringBuilder summary = new StringBuilder(String.format("""
                Generated MCP server key: %s
                Location: %s
                """, descriptor.serverKey(), fileLocation)
        );

        appendIfNotEmpty("Tools", descriptor.tools(), summary);
        appendIfNotEmpty("Prompts", descriptor.prompts(), summary);
        appendIfNotEmpty("Completions", descriptor.completions(), summary);
        appendIfNotEmpty("Resources", descriptor.resources(), summary);
        appendIfNotEmpty("Resource Templates", descriptor.resourceTemplates(), summary);

        messager.printMessage(Diagnostic.Kind.NOTE, summary.toString());
    }

    private void appendIfNotEmpty(String text, Collection<?> items, StringBuilder builder) {
        if (!items.isEmpty()) {
            builder.append(String.format("%s: %d  ", text, items.size()));
        }
    }

    private String capitalize(String str) {
        var lowerCase = str.toLowerCase();
        return Character.toUpperCase(lowerCase.charAt(0)) + lowerCase.substring(1);
    }
}
