package io.github.kliushnichenko.mcp.example;

import io.github.kliushnichenko.jooby.mcp.annotation.McpServer;
import io.github.kliushnichenko.jooby.mcp.annotation.Tool;

@McpServer("weather")
public class WeatherService {

    public record Coordinates(double latitude, double longitude) {}

    @Tool(name = "get_weather")
    public String getWeather(Coordinates coordinates) {
        // Simulate fetching weather data for the given location
        // In a real application, this would involve calling a weather API
        return "The weather in Numenor is sunny with a temperature of 25°C.";
    }
}
