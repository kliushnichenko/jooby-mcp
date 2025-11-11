package io.github.kliushnichenko.jooby.mcp.apt.generator;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.TypeSpec;
import io.github.kliushnichenko.jooby.mcp.apt.ArgNameExtractor;
import io.github.kliushnichenko.jooby.mcp.apt.McpServerDescriptor;
import io.github.kliushnichenko.jooby.mcp.apt.resources.ResourceEntry;
import io.modelcontextprotocol.spec.McpSchema;

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

    abstract boolean hasItems(McpServerDescriptor descriptor);

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

    protected static CodeBlock buildResourceAnnotations(ResourceEntry.Annotations annotations) {
        if (annotations != null) {
            CodeBlock.Builder audienceList = CodeBlock.builder().add("List.of(");
            var roles = annotations.audience();
            for (int i = 0; i < roles.length; i++) {
                if (i > 0) {
                    audienceList.add(", ");
                }
                audienceList.add("$T.$L", McpSchema.Role.class, roles[i].name());
            }
            audienceList.add(")");

            return CodeBlock.of("new $T($L, $L, $S)",
                    ClassName.get(McpSchema.Annotations.class),
                    audienceList.build(),
                    annotations.priority(),
                    annotations.lastModified()
            );
        }
        return null;
    }

    protected void addIfNotNull(Object value, CodeBlock.Builder codeBuilder, String format) {
        if (value != null) {
            codeBuilder.add(format, value);
        }
    }

    protected void addIfPositive(Number value, CodeBlock.Builder codeBuilder, String format) {
        if (value != null && value.doubleValue() >= 0) {
            codeBuilder.add(format, value);
        }
    }
}
