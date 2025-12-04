package test;

import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

/**
 * @author kliushnichenko
 */
public class ToolsOutputSchemaTest extends BaseTest {

    private static final Map<String, Object> PET_OUTPUT_SCHEMA = Map.of(
            "type", "object",
            "properties", Map.of(
                    "name", Map.of(
                            "type", "string"
                    )
            ),
            "required", List.of("name"),
            "additionalProperties", false
    );

    private static final Map<String, Object> PETS_ARRAY_OUTPUT_SCHEMA = Map.of(
            "type", "array",
            "items", PET_OUTPUT_SCHEMA
    );

    private static final Map<String, Object> PETS_MAP_OUTPUT_SCHEMA = Map.of(
            "type", "object",
            "additionalProperties", PET_OUTPUT_SCHEMA
    );

    @Test
    void outputSchema_returnTypeIsPet_outputSchemaShouldReflectPet() {
        var tool = findTool("test_default_output_scheme_generation");

        assertThat(tool.outputSchema())
                .usingRecursiveComparison()
                .isEqualTo(PET_OUTPUT_SCHEMA);
    }

    @Test
    void outputSchema_suppressOutputType_outputSchemaShouldBeNull() {
        var tool = findTool("test_suppressing_default_output_scheme_generation");
        assertThat(tool.outputSchema()).isNull();
    }

    @Test
    void outputSchema_returnTypeIsReserved_outputSchemaShouldBeNull() {
        var tool = findTool("test_reserved_type_for_output_scheme");
        assertThat(tool.outputSchema()).isNull();
    }

    @Test
    void outputSchema_returnTypeIsInAnnotation_outputSchemaShouldReflectPet() {
        var tool = findTool("test_output_scheme_from_scalar_annotation");
        assertThat(tool.outputSchema())
                .usingRecursiveComparison()
                .isEqualTo(PET_OUTPUT_SCHEMA);
    }

    @Test
    void outputSchema_returnTypeIsArrayAnnotation_outputSchemaShouldReflectPetsArray() {
        var tool = findTool("test_output_scheme_from_array_annotation");
        assertThat(tool.outputSchema())
                .usingRecursiveComparison()
                .isEqualTo(PETS_ARRAY_OUTPUT_SCHEMA);
    }

    @Test
    void outputSchema_returnTypeIsListOfPets_outputSchemaShouldReflectPetsArray() {
        var tool = findTool("test_output_scheme_for_list");
        assertThat(tool.outputSchema())
                .usingRecursiveComparison()
                .isEqualTo(PETS_ARRAY_OUTPUT_SCHEMA);
    }

    @Test
    void outputSchema_returnTypeIsMapAnnotation_outputSchemaShouldReflectPetsMap() {
        var tool = findTool("test_output_scheme_from_map_annotation");
        assertThat(tool.outputSchema())
                .usingRecursiveComparison()
                .isEqualTo(PETS_MAP_OUTPUT_SCHEMA);
    }

    private McpSchema.Tool findTool(String name) {
        return mcpClient.listTools().tools().stream()
                .filter(tool -> tool.name().equals(name))
                .findFirst()
                .orElseThrow();
    }
}
