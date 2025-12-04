package ext;

import io.jooby.Jooby;
import io.jooby.ServerOptions;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * @author kliushnichenko
 */
@Getter
@Setter
@Accessors(fluent = true)
@AllArgsConstructor
public class JoobyTestConfig {

    private final Class<? extends Jooby> app;
    private ServerOptions serverOptions;
    private String env = "test";
    private Map<String, String> properties = new HashMap<>();

    public JoobyTestConfig(Class<? extends Jooby> app, int port) {
        Objects.requireNonNull(app);
        this.app = app;
        this.serverOptions = new ServerOptions()
                .setPort(port)
                .setIoThreads(1)
                .setWorkerThreads(8);
    }

    public JoobyTestConfig properties(Map<String, String> props) {
        properties.putAll(props);
        return this;
    }

    public Map<String, String> properties() {
        return Collections.unmodifiableMap(properties);
    }
}
