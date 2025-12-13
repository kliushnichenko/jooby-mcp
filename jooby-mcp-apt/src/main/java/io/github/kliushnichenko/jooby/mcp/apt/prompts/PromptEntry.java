package io.github.kliushnichenko.jooby.mcp.apt.prompts;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import java.util.List;

/**
 * @author kliushnichenko
 */
public record PromptEntry(String name,
                          String title,
                          String description,
                          List<Arg> promptArgs,
                          String serverKey,
                          TypeElement serviceClass,
                          ExecutableElement method) {

    public record Arg(String name, String description, boolean required) {
    }
}
