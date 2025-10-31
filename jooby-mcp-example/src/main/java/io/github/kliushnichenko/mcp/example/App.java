package io.github.kliushnichenko.mcp.example;

import io.github.kliushnichenko.jooby.mcp.McpModule;
import io.github.kliushnichenko.mcp.example.mcp.CalculatorMcpServer;
import io.github.kliushnichenko.mcp.example.mcp.WeatherMcpServer;
import io.jooby.Jooby;
import io.jooby.avaje.inject.AvajeInjectModule;
import io.jooby.handler.AccessLogHandler;

public class App extends Jooby {

    {
        use(new AccessLogHandler());
//        install(new JacksonModule());
        install(AvajeInjectModule.of());
        install(new McpModule(new CalculatorMcpServer(), new WeatherMcpServer()));
    }

    public static void main(String[] args) {
        runApp(args, App::new);
    }
}
