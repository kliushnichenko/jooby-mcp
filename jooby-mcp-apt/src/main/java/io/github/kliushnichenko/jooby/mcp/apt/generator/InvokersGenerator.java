package io.github.kliushnichenko.jooby.mcp.apt.generator;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterizedTypeName;
import com.palantir.javapoet.TypeSpec;
import io.github.kliushnichenko.jooby.mcp.internal.MethodInvoker;

import javax.lang.model.element.Modifier;
import java.util.Map;

import static io.github.kliushnichenko.jooby.mcp.apt.generator.McpServerGenerator.PROMPT_INVOKERS_FIELD_NAME;
import static io.github.kliushnichenko.jooby.mcp.apt.generator.McpServerGenerator.TOOL_INVOKERS_FIELD_NAME;

class InvokersGenerator {

    static void generateInvokers(TypeSpec.Builder builder) {
        addInvokeToolMethod(builder);
        addInvokePromptMethod(builder);
    }

    private static void addInvokeToolMethod(TypeSpec.Builder builder) {
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

    private static void addInvokePromptMethod(TypeSpec.Builder builder) {
        MethodSpec invokeMethod = MethodSpec.methodBuilder("invokePrompt")
                .addModifiers(Modifier.PUBLIC)
                .addParameter(String.class, "promptName", Modifier.FINAL)
                .addParameter(ParameterizedTypeName.get(Map.class, String.class, Object.class), "args", Modifier.FINAL)
                .returns(Object.class)
                .addJavadoc("Invokes a prompt by name with the provided arguments.\n")
                .addJavadoc("@param promptName the name of the prompt to invoke\n")
                .addJavadoc("@param args the arguments to pass to the prompt\n")
                .addJavadoc("@return the result of the prompt invocation\n")
                .addJavadoc("@throws IllegalArgumentException if the tool is not found")
                .addStatement("$T invoker = $L.get(promptName)",
                        ClassName.get(MethodInvoker.class), PROMPT_INVOKERS_FIELD_NAME)
                .addStatement("return invoker.invoke(args)")
                .build();

        builder.addMethod(invokeMethod);
    }
}
