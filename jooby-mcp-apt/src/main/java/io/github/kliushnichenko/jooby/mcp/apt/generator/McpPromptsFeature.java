package io.github.kliushnichenko.jooby.mcp.apt.generator;

import com.palantir.javapoet.*;
import io.github.kliushnichenko.jooby.mcp.annotation.PromptArg;
import io.github.kliushnichenko.jooby.mcp.apt.McpServerDescriptor;
import io.github.kliushnichenko.jooby.mcp.apt.prompts.PromptEntry;
import io.github.kliushnichenko.jooby.mcp.internal.MethodInvoker;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;

import javax.lang.model.element.Modifier;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author kliushnichenko
 */
public class McpPromptsFeature extends McpFeature {

    @Override
    public void generateFields(TypeSpec.Builder builder) {
        FieldSpec promptsField = FieldSpec.builder(
                        ParameterizedTypeName.get(Map.class, String.class, McpSchema.Prompt.class),
                        "prompts",
                        Modifier.PRIVATE, Modifier.FINAL)
                .initializer("new $T<>()", HashMap.class)
                .addJavadoc("Map of prompt names to its specification.")
                .build();

        FieldSpec promptInvokersField = FieldSpec.builder(
                        ParameterizedTypeName.get(
                                ClassName.get(Map.class),
                                ClassName.get(String.class),
                                ClassName.get(MethodInvoker.class)
                        ),
                        "promptInvokers",
                        Modifier.PRIVATE, Modifier.FINAL)
                .initializer("new $T<>()", HashMap.class)
                .addJavadoc("Map of prompt names to method invokers.")
                .build();

        builder.addField(promptsField);
        builder.addField(promptInvokersField);
    }

    @Override
    public void generateInitializers(MethodSpec.Builder builder, McpServerDescriptor descriptor) {
        // fill prompts map
        for (PromptEntry prompt : descriptor.prompts()) {
            CodeBlock promptArguments = buildPromptArgs(prompt.promptArgs());

            builder.addStatement(
                    "prompts.put($S, new $T($S, $S, $L))",
                    prompt.promptName(),
                    ClassName.get(McpSchema.Prompt.class),
                    prompt.promptName(),
                    prompt.promptDescription(),
                    promptArguments);
        }
        builder.addCode("\n");

        // fill prompt invokers map
        for (PromptEntry entry : descriptor.prompts()) {
            CodeBlock methodCall = buildMethodInvocation(entry.method(), entry.serviceClass(), PromptArg.class);
            var mapEntry = CodeBlock.of("$S, $L", entry.promptName(), methodCall);
            builder.addCode(CodeBlock.of("promptInvokers.put($L);\n", mapEntry));
        }
    }

    @Override
    public void generateInvoker(TypeSpec.Builder builder) {
        MethodSpec invokeMethod = MethodSpec.methodBuilder("invokePrompt")
                .addModifiers(Modifier.PUBLIC)
                .addParameter(String.class, "promptName", Modifier.FINAL)
                .addParameter(ParameterizedTypeName.get(Map.class, String.class, Object.class), "args", Modifier.FINAL)
                .addParameter(McpSyncServerExchange.class, "exchange", Modifier.FINAL)
                .returns(Object.class)
                .addJavadoc("""
                         Invokes a prompt by name with the provided arguments.
                         @param promptName the name of the prompt to invoke
                         @param args the arguments to pass to the prompt
                         @return the result of the prompt invocation
                        """)
                .addStatement("$T invoker = promptInvokers.get(promptName)", ClassName.get(MethodInvoker.class))
                .addStatement("return invoker.invoke(args, exchange)")
                .build();

        builder.addMethod(invokeMethod);
    }

    @Override
    public void generateGetter(TypeSpec.Builder builder) {
        MethodSpec getter = MethodSpec.methodBuilder("getPrompts")
                .addModifiers(Modifier.PUBLIC)
                .returns(ParameterizedTypeName.get(Map.class, String.class, McpSchema.Prompt.class))
                .addStatement("return prompts")
                .build();

        builder.addMethod(getter);
    }

    @Override
    boolean hasItems(McpServerDescriptor descriptor) {
        return !descriptor.prompts().isEmpty();
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
}
