# jooby-mcp

This module provides a lightweight wrapper over the official Java MCP [SDK](https://github.com/modelcontextprotocol/java-sdk), adapted for use with Jooby’s routing, server
capabilities and DI.

The module provides declarative(annotation-based) registration of tools, prompts and resources.
Annotations discovery is done at build-time using APT, so no reflection is used at runtime.
Hence, you will need to add an annotation processor in addition to the module dependency.

Features:

- [X] SSE transport
- [X] Multiple servers support
- [X] Tools
- [X] Prompts
- [ ] Resources
- [ ] Completions
- [ ] HTTP Streamable transport

Table of Contents:
- [Quick Start](#quick-start)
- [Example Tools & Prompts](#example-tools--prompts)
- [Multiple Servers Support](#multiple-servers-support)
- [Customizing Default Server Name and Package](#customizing-default-server-name-and-package)
- [Supported return types in Tools](#supported-return-types-in-tools)
- [Supported return types in Prompts](#supported-return-types-in-prompts)

## Quick Start

1. Add the dependency to your `pom.xml`:
    ```xml
    <dependency>
        <groupId>io.github.kliushnichenko</groupId>
        <artifactId>jooby-mcp</artifactId>
        <version>${jooby.mcp.version}</version>
    </dependency>
   ```
2. Add annotation processor
   ```xml
   <plugin>
       <groupId>org.apache.maven.plugins</groupId>
       <artifactId>maven-compiler-plugin</artifactId>
       <configuration>
           <annotationProcessorPaths>
               <path>
                   <groupId>io.github.kliushnichenko</groupId>
                   <artifactId>jooby-mcp-processor</artifactId>
                   <version>${jooby.mcp.version}</version>
               </path>
           </annotationProcessorPaths>
       </configuration>
   </plugin>
   ```

3. Add configuration to `application.conf`
   ```
   mcp.default {                       # `default` is the server key, can be customized over compiler arguments
     name: "my-awesome-mcp-server"     # Required
     version: "0.1.0"                  # Required
     sseEndpoint: "/mcp/sse"           # Optional (default: /mcp/sse)
     messageEndpoint: "/mcp/message"   # Optional (default: /mcp/message)
   }
   ```

4. Implement your tool, prompt or resource, see examples below

5. Install the module. After compilation, you can observe generated `DefaultMcpServer` class. Now register its instance
   in the module:
   ```java
   {
      install(new JacksonModule()); // some JSON encode/decoder is required
      install(AvajeInjectModule.of()); // or other DI module of your choice
      install(new McpModule(new DefaultMcpServer());
   }
   ```

### Example Tools & Prompts

   ```java
   import io.github.kliushnichenko.jooby.mcp.annotation.Tool;
   import io.github.kliushnichenko.jooby.mcp.annotation.ToolArg;
   import io.github.kliushnichenko.jooby.mcp.annotation.Prompt;
   
   @Singleton
   public class Service {
   
       @Tool(name = "add", description = "Adds two numbers together")
       public String add(
               @ToolArg(name = "first", description = "First number to add") int a,
               @ToolArg(name = "second", description = "Second number to add") int b
       ) {
           int result = a + b;
           return String.valueOf(result);
       }
   
       @Tool
       public String subtract(int a, int b) {
           int result = a - b;
           return String.valueOf(result);
       }
   
       @Prompt(name = "summarizeText", description = "Summarizes the provided text into a specified number of sentences")
       public String summarizeText(@PromptArg(name = "text") String text, String maxSentences) {
           return String.format("""
                   Please provide a clear and concise summary of the following text in no more than %s sentences:
                   %s
                   """, maxSentences, text);
       }
   } 
   ```

### Multiple Servers Support

Use `@McpServer` annotation to assign a tool or prompt to a specific server. Annotation can be applied at the class or
method level.

   ```java
   import io.github.kliushnichenko.jooby.mcp.annotation.McpServer;

   @Singleton
   @McpServer("weather")
   public class WeatherService {
   
       public record Coordinates(double latitude, double longitude) {
       }
   
       @Tool(name = "get_weather")
       public String getWeather(Coordinates coordinates) {
            ...
       }
   }
   ```

As a result, additional `WeatherMcpServer` class will be generated. Register it in the module:

   ```java
   {
        ...
        install(new McpModule(new DefaultMcpServer().server(new WeatherMcpServer())));
        }
   ```

The weather MCP server should have its own configuration section in `application.conf`:

   ```
   mcp.weather {
     name: "weather-mcp-server"
     version: "0.1.0"
     sseEndpoint: "/weather-mcp/sse"
     messageEndpoint: "/weather-mcp/message"
   }
   ```

### Customizing Default Server Name and Package

You can customize the default server name and the package where the generated server classes will be placed by providing
compiler arguments. For example, to set the default server name to `CalculatorMcpServer` and the package
to `com.acme.corp.mcp`, you can add the following configuration to your `pom.xml`:

   ```xml
   <plugin>
       <groupId>org.apache.maven.plugins</groupId>
       <artifactId>maven-compiler-plugin</artifactId>
       <configuration>
           <annotationProcessorPaths>
               <path>
                   <groupId>io.github.kliushnichenko</groupId>
                   <artifactId>jooby-mcp-processor</artifactId>
                   <version>${version}</version>
               </path>
           </annotationProcessorPaths>
           <compilerArgs>
               <arg>-Amcp.default.server.key=calculator</arg>
               <arg>-Amcp.target.package=com.acme.corp.mcp</arg>
           </compilerArgs>
       </configuration>
   </plugin>
   ```

Mind, that `mcp.default.server.key` should match the configuration section in `application.conf`:

   ```
   mcp.calculator {
     name: "calculator-mcp-server"
     version: "0.1.0"
     sseEndpoint: "/calculator-mcp/sse"
     messageEndpoint: "/calculator-mcp/message"
   }
   ```

#### Supported return types in Tools

- `String`
- `McpSchema.CallToolResult`
- `McpSchema.Content`
- POJO (will be serialized to JSON)

#### Supported return types in Prompts

- `McpSchema.GetPromptResult`
- `McpSchema.PromptMessage`
- `McpSchema.Content`
- `String`
- POJO (`toString()` method will be invoked to get the string representation)