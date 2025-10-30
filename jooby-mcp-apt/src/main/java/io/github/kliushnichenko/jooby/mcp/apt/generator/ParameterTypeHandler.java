package io.github.kliushnichenko.jooby.mcp.apt.generator;

import com.palantir.javapoet.CodeBlock;
import io.github.kliushnichenko.jsonschema.generator.TypeUtils;
import io.modelcontextprotocol.json.TypeRef;

import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

/**
 * Utility class for handling parameter type casting in generated code.
 *
 * <p>This class provides methods to generate appropriate type casting expressions
 * for different parameter types when creating method invoker lambdas.</p>
 */
class ParameterTypeHandler {

    private ParameterTypeHandler() {
        throw new UnsupportedOperationException("Utility class should not be instantiated");
    }

    /**
     * Builds a parameter cast expression based on the parameter type.
     *
     * <p>This method generates the appropriate casting code for retrieving
     * parameters from the arguments map and casting them to the correct type.</p>
     *
     * @param param         the parameter element
     * @param parameterName the name of the parameter in the arguments map
     * @return a CodeBlock containing the cast expression
     */
    public static CodeBlock buildParameterCast(VariableElement param, String parameterName) {
        TypeMirror typeMirror = param.asType();
        String paramType = param.asType().toString();

        if (TypeUtils.isIterableType(typeMirror)) {
            return handleIterableTypes(parameterName, typeMirror, paramType);
        }

        if (TypeUtils.isMapType(typeMirror)) {
            return handleMapTypes(parameterName, paramType);
        }

        return switch (paramType) {
            case "byte" -> CodeBlock.of("(byte) args.get($S)", parameterName);
            case "short" -> CodeBlock.of("(short) args.get($S)", parameterName);
            case "int" -> CodeBlock.of("(int) args.get($S)", parameterName);
            case "long" -> CodeBlock.of("(long) args.get($S)", parameterName);
            case "double" -> CodeBlock.of("(double) args.get($S)", parameterName);
            case "float" -> CodeBlock.of("(float) args.get($S)", parameterName);
            case "boolean" -> CodeBlock.of("(boolean) args.get($S)", parameterName);
            case "java.lang.String" -> CodeBlock.of("(String) args.get($S)", parameterName);
            case "java.lang.Integer" -> CodeBlock.of("(Integer) args.get($S)", parameterName);
            case "java.lang.Short" -> CodeBlock.of("(Short) args.get($S)", parameterName);
            case "java.lang.Long" -> CodeBlock.of("(Long) args.get($S)", parameterName);
            case "java.lang.Double" -> CodeBlock.of("(Double) args.get($S)", parameterName);
            case "java.lang.Float" -> CodeBlock.of("(Float) args.get($S)", parameterName);
            case "java.lang.Boolean" -> CodeBlock.of("(Boolean) args.get($S)", parameterName);
            default -> CodeBlock.of("($L) mcpJsonMapper.convertValue(args.get($S), $L.class)",
                    paramType,
                    parameterName,
                    paramType);
        };
    }

    private static CodeBlock handleMapTypes(String parameterName, String paramType) {
        return CodeBlock.of("($L) mcpJsonMapper.convertValue(args.get($S), new $T<$L>() {})",
                paramType,
                parameterName,
                TypeRef.class,
                paramType);
    }

    private static CodeBlock handleIterableTypes(String parameterName, TypeMirror typeMirror, String paramType) {
        TypeMirror componentType = TypeUtils.getCollectionComponentType(typeMirror);
        if (TypeKind.ARRAY == typeMirror.getKind()) {
            return CodeBlock.of("($L) mcpJsonMapper.convertValue(args.get($S), $L[].class)",
                    paramType,
                    parameterName,
                    componentType.toString());
        } else {
            // handle collection
            return CodeBlock.of("($L) mcpJsonMapper.convertValue(args.get($S), new $T<$L>() {})",
                    paramType,
                    parameterName,
                    TypeRef.class,
                    paramType);
        }
    }
}
