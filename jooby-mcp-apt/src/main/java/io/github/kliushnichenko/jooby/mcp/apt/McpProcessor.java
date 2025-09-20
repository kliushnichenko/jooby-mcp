package io.github.kliushnichenko.jooby.mcp.apt;

import com.google.auto.service.AutoService;
import io.github.kliushnichenko.jooby.mcp.annotation.McpServer;
import io.github.kliushnichenko.jooby.mcp.annotation.Prompt;
import io.github.kliushnichenko.jooby.mcp.annotation.Tool;
import io.github.kliushnichenko.jooby.mcp.apt.generator.McpServerGenerator;
import io.github.kliushnichenko.jooby.mcp.apt.prompts.PromptEntry;
import io.github.kliushnichenko.jooby.mcp.apt.prompts.PromptsCollector;
import io.github.kliushnichenko.jooby.mcp.apt.tools.ToolEntry;
import io.github.kliushnichenko.jooby.mcp.apt.tools.ToolsCollector;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import static io.github.kliushnichenko.jooby.mcp.apt.McpProcessor.OPTION_DEFAULT_SERVER_KEY;
import static io.github.kliushnichenko.jooby.mcp.apt.McpProcessor.OPTION_TARGET_PACKAGE;

@AutoService(Processor.class)
@SupportedAnnotationTypes({
        "io.github.kliushnichenko.jooby.mcp.annotation.Tool",
        "io.github.kliushnichenko.jooby.mcp.annotation.Prompt"
})
@SupportedOptions({OPTION_DEFAULT_SERVER_KEY, OPTION_TARGET_PACKAGE})
@SupportedSourceVersion(SourceVersion.RELEASE_17)
public class McpProcessor extends AbstractProcessor {

    private static final Pattern SERVER_KEY_PATTERN = Pattern.compile("^[A-Za-z][A-Za-z0-9]*$");

    static final String OPTION_DEFAULT_SERVER_KEY = "mcp.default.server.key";
    static final String OPTION_TARGET_PACKAGE = "mcp.target.package";
    private static final String DEFAULT_SERVER_KEY = "default";

    private Elements elementUtils;
    private Messager messager;

    private ToolsCollector toolsCollector;
    private PromptsCollector promptsCollector;
    private McpServerGenerator mcpServerGenerator;

    private Set<String> serverKeys;
    private List<ToolEntry> tools = new ArrayList<>();
    private List<PromptEntry> prompts = new ArrayList<>();

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.elementUtils = processingEnv.getElementUtils();
        this.messager = processingEnv.getMessager();

        String defaultServerKey = processingEnv.getOptions()
                .getOrDefault(OPTION_DEFAULT_SERVER_KEY, DEFAULT_SERVER_KEY);
        boolean serverKeyIsValid = verifyServerKey(defaultServerKey);

        if (!serverKeyIsValid) {
            throw new RuntimeException("Illegal value at '" + OPTION_DEFAULT_SERVER_KEY +
                                       "' compilerArg. Server key must start with a letter and contain " +
                                       "only letters and digits");
        }

        this.serverKeys = new HashSet<>(List.of(defaultServerKey));
        this.toolsCollector = new ToolsCollector(messager, defaultServerKey);
        this.promptsCollector = new PromptsCollector(messager, defaultServerKey);
        this.mcpServerGenerator = new McpServerGenerator(processingEnv.getFiler());
    }

    private boolean verifyServerKey(String defaultServerKey) {
        if (defaultServerKey == null || defaultServerKey.isBlank()) {
            reportError("Default server key cannot be empty. Please provide a valid server key");
            return false;
        } else if (!SERVER_KEY_PATTERN.matcher(defaultServerKey).matches()) {
            reportError("Illegal default server key. Server key must start with a letter " +
                        "and contain only letters and digits");
            return false;
        }

        return true;
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (roundEnv.processingOver()) {
            return false;
        }

        try {
            if (annotations.isEmpty()) {
                return false;
            }

            String defaultTargetPackage = evalTargetPackage(roundEnv);

            log("Start McpProcessor");
            serverKeys.addAll(collectServers(roundEnv));

            tools = toolsCollector.collectTools(roundEnv);
            prompts = promptsCollector.collectPrompts(roundEnv);

            List<McpServerDescriptor> descriptors = buildServerDescriptors(defaultTargetPackage);
            mcpServerGenerator.generateMcpServers(descriptors);
            log("End McpProcessor execution");

            return true;
        } catch (IOException e) {
            reportError("Failed to generate Mcp Server: " + e.getMessage());
            return false;
        } catch (Exception e) {
            reportError("Unexpected error during McpProcessor execution: " + e.getMessage());
            return false;
        }
    }

    private String evalTargetPackage(RoundEnvironment roundEnv) {
        String targetPackage = processingEnv.getOptions()
                .getOrDefault(OPTION_TARGET_PACKAGE, "");

        if (targetPackage.isBlank()) {
            // evaluate default target package
            Element classElement = roundEnv.getElementsAnnotatedWithAny(Set.of(Tool.class, Prompt.class))
                    .iterator()
                    .next()
                    .getEnclosingElement();

            PackageElement pkg = elementUtils.getPackageOf(classElement);
            targetPackage = pkg.isUnnamed() ? "" : pkg.getQualifiedName().toString();
        }

        return targetPackage;
    }

    private List<McpServerDescriptor> buildServerDescriptors(String defaultTargetPackage) {
        List<McpServerDescriptor> descriptors = new ArrayList<>();
        for (String serverKey : serverKeys) {
            descriptors.add(new McpServerDescriptor(
                    serverKey,
                    defaultTargetPackage,
                    tools.stream().filter(tool -> tool.serverKey().equals(serverKey)).toList(),
                    prompts.stream().filter(prompt -> prompt.serverKey().equals(serverKey)).toList()
            ));
        }
        return descriptors;
    }

    private Set<String> collectServers(RoundEnvironment roundEnv) {
        Set<? extends Element> serverElements = roundEnv.getElementsAnnotatedWith(McpServer.class);
        Set<String> servers = new HashSet<>();
        for (Element element : serverElements) {
            McpServer annotation = element.getAnnotation(McpServer.class);
            servers.add(annotation.value());
        }
        return servers;
    }

    private void reportError(String message) {
        messager.printMessage(Diagnostic.Kind.ERROR, message);
    }

    private void log(String message) {
        messager.printMessage(Diagnostic.Kind.NOTE, message);
    }
} 