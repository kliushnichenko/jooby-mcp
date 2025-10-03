package io.github.kliushnichenko.jooby.mcp.apt.generator;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.TypeSpec;
import io.github.kliushnichenko.jooby.mcp.apt.ArgNameExtractor;
import io.github.kliushnichenko.jooby.mcp.apt.McpServerDescriptor;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import java.lang.annotation.Annotation;
import java.util.List;

abstract class McpFeature {

    abstract void generateFields(TypeSpec.Builder builder);

    abstract void generateInitializers(MethodSpec.Builder builder, McpServerDescriptor descriptor);

    abstract void generateInvoker(TypeSpec.Builder builder);

    abstract void generateGetter(TypeSpec.Builder builder);

    /**
     * Builds a method invocation lambda expression.
     */
    protected CodeBlock buildMethodInvocation(ExecutableElement method,
                                            TypeElement serviceClass,
                                            Class<? extends Annotation> annotationClass) {
        List<? extends VariableElement> parameters = method.getParameters();

        ClassName serviceClassName = ClassName.get(serviceClass);
        CodeBlock.Builder methodCall = CodeBlock.builder();
        methodCall.add("(args) -> app.require($T.class).$L(", serviceClassName, method.getSimpleName());

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
}
