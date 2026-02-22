package io.github.kliushnichenko.jooby.mcp.example;

import io.github.kliushnichenko.jooby.mcp.McpModule;
import io.github.kliushnichenko.mcp.inspector.McpInspectorModule;
import io.jooby.Jooby;
import io.jooby.avaje.inject.AvajeInjectModule;
import io.jooby.handler.AccessLogHandler;
import io.jooby.jackson.JacksonModule;

/**
 * @author kliushnichenko
 */
public class App extends Jooby {

    {
        use(new AccessLogHandler());
        install(new JacksonModule());
        install(AvajeInjectModule.of());
        install(new McpModule(new ExampleMcpServer(), new WeatherMcpServer()));
        install(new McpInspectorModule().path("/"));


    }

    public static void main(String[] args) {
        runApp(args, App::new);
    }
}
