package ext;

import io.jooby.*;
import lombok.Getter;
import org.junit.jupiter.api.extension.Extension;

import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * @author kliushnichenko
 */
@Getter
public class JoobyTestSingleton implements Extension {

    private static JoobyTestSingleton singletonExtension;
    private Jooby app;

    public static JoobyTestSingleton getExtension(JoobyTestConfig config) {
        if (singletonExtension == null) {
            singletonExtension = new JoobyTestSingleton();
            startApp(singletonExtension, config);
        }
        return singletonExtension;
    }

    private static void startApp(JoobyTestSingleton extension, JoobyTestConfig config) {
        config.properties().forEach(System::setProperty);

        System.setProperty("application.env", config.env());
        Server srv = Server.loadServer(config.serverOptions());
        Jooby app = Jooby.createApp(srv, ExecutionMode.DEFAULT, reflectionProvider(config.app()));
        srv.start(app);
        extension.app = app;
    }

    private static Supplier<Jooby> reflectionProvider(Class<? extends Jooby> applicationType) {
        return () -> (Jooby) Stream.of(applicationType.getDeclaredConstructors())
                .filter(it -> it.getParameterCount() == 0)
                .findFirst()
                .map(SneakyThrows.throwingFunction(c -> c.newInstance()))
                .orElseThrow(() -> new IllegalArgumentException("Default constructor for: " + applicationType.getName()));
    }
}
