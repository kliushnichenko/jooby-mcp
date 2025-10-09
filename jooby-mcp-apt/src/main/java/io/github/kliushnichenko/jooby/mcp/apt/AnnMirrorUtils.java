package io.github.kliushnichenko.jooby.mcp.apt;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import java.util.Optional;

public class AnnMirrorUtils {

    public static Optional<AnnotationMirror> findAnnotationMirror(Element element, Class<?> annotationClass) {
        String annotationClassName = annotationClass.getName();
        for (AnnotationMirror mirror : element.getAnnotationMirrors()) {
            if (mirror.getAnnotationType().toString().equals(annotationClassName)) {
                return Optional.of(mirror);
            }
        }
        return Optional.empty();
    }

    public static boolean hasProperty(AnnotationMirror mirror, String propertyName) {
        return mirror.getElementValues()
                .keySet()
                .stream()
                .anyMatch(k -> k.getSimpleName().toString().equals(propertyName));
    }
}
