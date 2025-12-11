package app;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.kliushnichenko.jooby.mcp.McpModule;
import io.jooby.Jooby;
import io.jooby.jackson.JacksonModule;


public class TestApp extends Jooby {

    private final ObjectMapper objectMapper = new ObjectMapper();

    {
        install(new JacksonModule(objectMapper));

        getServices().put(Prompts.class, new Prompts());
        getServices().put(ToolsOutputSchemaTestCases.class, new ToolsOutputSchemaTestCases());
        getServices().put(ToolsAnnotationsTestCases.class, new ToolsAnnotationsTestCases());

        install(new McpModule(new DefaultMcpServer()));
    }

    public static void main(String[] args) {
        runApp(args, TestApp::new);
    }
}
