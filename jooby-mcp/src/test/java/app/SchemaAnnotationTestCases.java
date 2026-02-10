package app;

import io.github.kliushnichenko.jooby.mcp.annotation.Tool;
import io.swagger.v3.oas.annotations.media.Schema;

public class SchemaAnnotationTestCases {

    @Tool(name = "test_nullable_field")
    public EntityWithNullable petWithNullable() {
        return new EntityWithNullable("Rex");
    }

    public static class EntityWithNullable {
        public String firstName;

        @Schema(nullable = true)
        public String middleName;

        public EntityWithNullable(String name) {
            this.firstName = name;
        }
    }

}
