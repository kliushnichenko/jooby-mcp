package io.github.kliushnichenko.jooby.mcp.apt.generator;

import com.palantir.javapoet.*;
import io.github.kliushnichenko.jooby.mcp.apt.McpServerDescriptor;
import io.github.kliushnichenko.jooby.mcp.apt.completions.CompletionEntry;
import io.modelcontextprotocol.spec.McpSchema;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class McpCompletionsFeature extends McpFeature {

    @Override
    void generateFields(TypeSpec.Builder builder) {
        FieldSpec completionsField = FieldSpec.builder(
                        ParameterizedTypeName.get(List.class, McpSchema.CompleteReference.class),
                        "completions",
                        Modifier.PRIVATE, Modifier.FINAL)
                .initializer("new $T<>()", ArrayList.class)
                .addJavadoc("List of completions reference objects.")
                .build();

        FieldSpec completionInvokersField = FieldSpec.builder(
                        ParameterizedTypeName.get(
                                ClassName.get(Map.class),
                                ClassName.get(String.class),
                                ParameterizedTypeName.get(
                                        ClassName.get(Function.class),
                                        ClassName.get(String.class),
                                        ClassName.get(Object.class))
                        ),
                        "completionInvokers",
                        Modifier.PRIVATE, Modifier.FINAL)
                .initializer("new $T<>()", HashMap.class)
                .addJavadoc("Map of completion key(a composition of <identifier>_<argumentName>) to method invoker.")
                .build();

        builder.addField(completionsField);
        builder.addField(completionInvokersField);
    }

    @Override
    void generateInitializers(MethodSpec.Builder builder, McpServerDescriptor descriptor) {
        for (CompletionEntry completion : descriptor.completions()) {
            builder.addStatement(
                    "completions.add(new $T($S))",
                    completion.type().getClassName(),
                    completion.identifier()
            );
        }
        builder.addCode("\n");

        // fill completion invokers map
        for (CompletionEntry entry : descriptor.completions()) {
            CodeBlock methodCall = buildMethodInvocation(entry.method(), entry.serviceClass());
            String completionKey = entry.identifier() + "_" + entry.argumentName();
            var mapEntry = CodeBlock.of("$S, $L", completionKey, methodCall);
            builder.addCode(CodeBlock.of("completionInvokers.put($L);\n", mapEntry));
        }
        builder.addCode("\n");
    }

    protected CodeBlock buildMethodInvocation(ExecutableElement method, TypeElement serviceClass) {
        ClassName serviceClassName = ClassName.get(serviceClass);
        CodeBlock.Builder methodCall = CodeBlock.builder();
        methodCall.add("(input) -> app.require($T.class).$L(input)",
                serviceClassName,
                method.getSimpleName());

        return methodCall.build();
    }

    @Override
    void generateInvoker(TypeSpec.Builder builder) {
        MethodSpec invokeMethod = MethodSpec.methodBuilder("invokeCompletion")
                .addModifiers(Modifier.PUBLIC)
                .addParameter(String.class, "identifier", Modifier.FINAL)
                .addParameter(String.class, "argumentName", Modifier.FINAL)
                .addParameter(String.class, "input", Modifier.FINAL)
                .returns(Object.class)
                .addJavadoc("""
                        Invokes a completion by identifier(prompt or resource name) and argumentName with the provided
                        argument value.
                        @param identifier prompt or resource template name
                        @param argumentName the name of an argument in prompt or resource template
                        @param input incoming argument value
                        @return the result of the completion invocation
                        """)
                .addCode("""
                        var completionKey = identifier + '_' + argumentName;
                        var invoker = completionInvokers.get(completionKey);
                        if (invoker == null) {
                            return List.of();
                        }
                        return invoker.apply(input);
                        """)
                .build();

        builder.addMethod(invokeMethod);
    }

    @Override
    void generateGetter(TypeSpec.Builder builder) {
        MethodSpec getter = MethodSpec.methodBuilder("getCompletions")
                .addModifiers(Modifier.PUBLIC)
                .returns(ParameterizedTypeName.get(List.class, McpSchema.CompleteReference.class))
                .addStatement("return completions")
                .build();

        builder.addMethod(getter);
    }
}
