package io.github.kliushnichenko.mcp.example;

import io.github.kliushnichenko.jooby.mcp.McpModule;
import io.github.kliushnichenko.mcp.example.mcp.CalculatorMcpServer;
import io.github.kliushnichenko.mcp.example.mcp.WeatherMcpServer;
import io.jooby.Jooby;
import io.jooby.jackson.JacksonModule;

public class App extends Jooby {

    {
        install(new JacksonModule());

        // Simple, for the sake of example, DI less services registration, but DI is supported as well
        getServices().put(CalculatorService.class, new CalculatorService());
        getServices().put(WeatherService.class, new WeatherService());

        install(new McpModule(new CalculatorMcpServer())
                .server(new WeatherMcpServer()));
    }

    public static void main(String[] args) {
        runApp(args, App::new);
    }
} 