package io.github.kliushnichenko.mcp.example;

import io.github.kliushnichenko.jooby.mcp.annotation.McpServer;
import io.github.kliushnichenko.jooby.mcp.annotation.Tool;
import jakarta.inject.Singleton;
import lombok.RequiredArgsConstructor;

@Singleton
@McpServer("weather")
@RequiredArgsConstructor
public class WeatherMcpServer {

    private WeatherService weatherService;

    @Tool(name = "get_weather")
    public String getWeather(double latitude, double longitude) {
        return weatherService.getWeather(latitude, longitude);
    }

    public static class WeatherService {

        public String getWeather(double latitude, double longitude) {
            // Simulate fetching weather data for the given location
            // In a real application, this would involve calling a weather API
            return "The weather in Numenor is sunny with a temperature of 25°C.";
        }
    }
}
