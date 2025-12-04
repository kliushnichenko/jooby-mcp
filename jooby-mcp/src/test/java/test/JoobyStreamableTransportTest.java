package test;

import transport.StreamableTransportApp;
import io.jooby.StatusCode;
import io.jooby.test.JoobyTest;
import io.modelcontextprotocol.spec.HttpHeaders;
import io.modelcontextprotocol.spec.McpSchema;
import io.restassured.RestAssured;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static io.modelcontextprotocol.spec.McpSchema.JSONRPC_VERSION;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;


@JoobyTest(value = StreamableTransportApp.class, port = 8095)
class JoobyStreamableTransportTest {

    static {
        RestAssured.port = 8095;
    }

    private static final McpSchema.JSONRPCRequest DUMMY_BODY = new McpSchema.JSONRPCRequest(
            JSONRPC_VERSION, "test", "1", null);

    @Nested
    class Get {

        @Test
        public void nonEventStream_shouldThrowError() {
            var result = given()
                    .header("Accept", "application/xml")
                    .get("/mcp")
                    .then()
                    .assertThat()
                    .statusCode(StatusCode.BAD_REQUEST_CODE)
                    .extract().asString();

            assertThat(result).contains("Invalid Accept header. Expected: [text/event-stream]");
        }

        @Test
        public void missingSessionId_shouldThrowError() {
            var result = given().header("Accept", "text/event-stream")
                    .when()
                    .get("/mcp")
                    .then()
                    .assertThat()
                    .statusCode(StatusCode.BAD_REQUEST_CODE)
                    .extract().asString();

            assertThat(result).contains("Session ID required in Mcp-Session-Id header");
        }

        @Test
        public void nonExistingSessionId_shouldThrowError() {
            var result = given()
                    .header(HttpHeaders.MCP_SESSION_ID, "12345")
                    .get("/mcp")
                    .then()
                    .assertThat()
                    .statusCode(StatusCode.NOT_FOUND_CODE)
                    .extract().asString();

            assertThat(result).contains("Session 12345 not found");
        }
    }

    @Nested
    class Post {

        @Test
        public void illegalAcceptHeader_shouldThrowError() {
            var result = given()
                    .header("Accept", "application/xml")
                    .body(DUMMY_BODY)
                    .post("/mcp")
                    .then()
                    .assertThat()
                    .statusCode(StatusCode.BAD_REQUEST_CODE)
                    .extract().asString();

            assertThat(result).contains("Invalid Accept header. Expected: [text/event-stream, application/json]");
        }

        @Test
        public void missingSessionId_shouldThrowError() {
            var result = given().header("Accept", "text/event-stream, application/json")
                    .body(DUMMY_BODY)
                    .when()
                    .post("/mcp")
                    .then()
                    .assertThat()
                    .statusCode(StatusCode.BAD_REQUEST_CODE)
                    .extract().as(McpSchema.JSONRPCResponse.class);

            assertThat(result.error().message()).contains("Session ID required in Mcp-Session-Id header");
        }

        @Test
        public void emptyBody_shouldThrowError() {
            var result = given().header("Accept", "text/event-stream, application/json")
                    .when()
                    .post("/mcp")
                    .then()
                    .assertThat()
                    .statusCode(StatusCode.BAD_REQUEST_CODE)
                    .extract().as(McpSchema.JSONRPCResponse.class);

            assertThat(result.error().message()).contains("Request body is missing");
        }
    }
}
