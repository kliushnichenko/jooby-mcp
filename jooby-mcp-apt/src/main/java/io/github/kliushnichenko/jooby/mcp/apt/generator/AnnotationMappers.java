package io.github.kliushnichenko.jooby.mcp.apt.generator;

import io.github.kliushnichenko.jooby.mcp.annotation.PromptArg;
import io.github.kliushnichenko.jooby.mcp.annotation.ToolArg;
import io.github.kliushnichenko.jsonschema.model.JsonSchemaAnnotationMapper;
import io.github.kliushnichenko.jsonschema.model.JsonSchemaProps;

import java.lang.annotation.Annotation;
import java.util.Map;

/**
 * @author kliushnichenko
 */
class AnnotationMappers {

    private static final JsonSchemaAnnotationMapper<ToolArg> TOOL_ARG_MAPPER = (ToolArg annotation) -> {
        JsonSchemaProps schemaProps = new JsonSchemaProps();
        schemaProps.setName(annotation.name());
        schemaProps.setDescription(annotation.description());
        schemaProps.setRequired(annotation.required());
//        schemaArg.setDefaultValue(annotation.defaultValue()); //todo: handle default values casting
        return schemaProps;
    };

    static final Map<Class<? extends Annotation>, JsonSchemaAnnotationMapper<?>> MAPPERS = Map.of(
            ToolArg.class, TOOL_ARG_MAPPER
    );
}
