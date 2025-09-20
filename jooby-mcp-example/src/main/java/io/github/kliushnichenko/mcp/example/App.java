package io.github.kliushnichenko.mcp.example;

import io.github.kliushnichenko.jooby.mcp.McpModule;
import io.github.kliushnichenko.mcp.example.mcp.CalculatorMcpServer;
import io.github.kliushnichenko.mcp.example.mcp.WeatherMcpServer;
import io.jooby.Jooby;
import io.jooby.jackson.JacksonModule;

public class App extends Jooby {

    {
        // Install Jackson for JSON processing
        install(new JacksonModule());

        getServices().put(CalculatorService.class, new CalculatorService());
        getServices().put(WeatherService.class, new WeatherService());

        // Create the generated ToolRegistry to demonstrate the annotation processor
        install(new McpModule(new CalculatorMcpServer())
                .server(new WeatherMcpServer()));
    }

    public static void main(String[] args) {
        runApp(args, App::new);
    }
} 