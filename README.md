# jooby-mcp
This module provides a lightweight wrapper around the official Java MCP SDK, adapted for use with Jooby’s routing, server capabilities and DI.

Unlike some other frameworks, this module does not (yet) offer convenient annotations. Instead, it delivers a minimal abstraction over the official MCP SDK to help structure your code more effectively.

Additionally, it supports running multiple MCP servers within a single Jooby instance.

## Usage

1. Add the dependency to your `pom.xml`: 
    ```xml
    <dependency>
        <groupId>io.github.kliushnichenko</groupId>
        <artifactId>jooby-mcp</artifactId>
        <version>${version}</version>
    </dependency>
   ```

2. Add configuration to `application.conf`
   ```
   mcp {
     name: "my-awesome-mcp-server"     # Required
     version: "0.0.1"                  # Required
     sseEndpoint: "/mcp/sse"           # Optional (default: /mcp/sse)
     messageEndpoint: "/mcp/message"   # Optional (default: /mcp/message)
   }
   ```

3. Implement your tool, prompt or resource, see examples below
   
4. Install module and register your tools, prompts, resources in the application
   ```java
   {
        install(AvajeInjectModule.of()); // or other DI module of your choice, should go before McpModule
        install(new McpModule()
            .tools(Set.of(GetWeatherTool.class))
            .prompts(Set.of(MyAwesomePrompt.class))
        );
   }
   ```
   

### Example Tool

   ```java
   import io.github.kliushnichenko.jooby.mcp.McpSyncTool;
   
   @Singleton
   public class GetWeatherTool implements McpSyncTool {
   
     @Inject
     private final WeatherService weatherService;
   
     @Override
     public McpSchema.Tool specification() {
         var schema = """
                 {
                   "type" : "object",
                   "id" : "urn:jsonschema:Operation",
                   "properties" : {
                     "latitude" : {
                       "type" : "number"
                     },
                     "longitude" : {
                       "type" : "number"
                     }
                   }
                 }
                 """;
   
         return new McpSchema.Tool("get_weather",
                 "Fetches weather by latitude and longitude coordinates",
                 schema);
     }
   
     @Override
     public McpSchema.CallToolResult handler(McpSyncServerExchange exchange, Map<String, Object> args) {
         Double latitude = (Double) args.get("latitude");
         Double longitude = (Double) args.get("longitude");
         var weather = weatherService.getWeather(latitude, longitude);
         return new McpSchema.CallToolResult(weather, false);
     }
   }
   ```