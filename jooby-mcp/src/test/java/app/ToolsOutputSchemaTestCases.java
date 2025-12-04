package app;

import io.github.kliushnichenko.jooby.mcp.annotation.OutputSchema;
import io.github.kliushnichenko.jooby.mcp.annotation.Tool;
import io.modelcontextprotocol.spec.McpSchema;

import java.util.List;

/**
 * @author kliushnichenko
 */
public class ToolsOutputSchemaTestCases {

    @Tool(name = "test_default_output_scheme_generation")
    public Pet findPet() {
        return new Pet("Buddy");
    }

    @Tool(name = "test_suppressing_default_output_scheme_generation")
    @OutputSchema.Suppressed
    public Pet findPetSuppressed() {
        return new Pet("Buddy");
    }

    @Tool(name = "test_reserved_type_for_output_scheme")
    public McpSchema.CallToolResult getPetName() {
        return buildTextResult("Buddy", false);
    }

    @Tool(name = "test_output_scheme_from_scalar_annotation")
    @OutputSchema.From(Pet.class)
    public McpSchema.CallToolResult lookupPet() {
        String pet = """
                {
                    "name": "Buddy"
                }
                """;
        return buildTextResult(pet, false);
    }

    @Tool(name = "test_output_scheme_from_array_annotation")
    @OutputSchema.ArrayOf(Pet.class)
    public McpSchema.CallToolResult lookupPetsArray() {
        String pet = """
                [{
                    "name": "Buddy"
                }]
                """;
        return buildTextResult(pet, false);
    }

    @Tool(name = "test_output_scheme_for_list")
    public List<Pet> findPets() {
        return List.of(new Pet("Buddy"));
    }

    @Tool(name = "test_output_scheme_from_map_annotation")
    @OutputSchema.MapOf(Pet.class)
    public McpSchema.CallToolResult lookupPetsMap() {
        String pet = """
                {
                    "1": {
                        "name": "Buddy"
                    }
                }
                """;
        return buildTextResult(pet, false);
    }

    private McpSchema.CallToolResult buildTextResult(String text, boolean isError) {
        return McpSchema.CallToolResult.builder()
                .addTextContent(text)
                .isError(isError)
                .build();
    }
}
