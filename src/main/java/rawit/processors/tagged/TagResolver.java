package rawit.processors.tagged;

import rawit.processors.model.TagInfo;
import rawit.processors.model.TagResolution;

import javax.annotation.processing.Messager;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Resolves the effective tag for an element by inspecting its annotations
 * against the known tag map.
 *
 * <p>If the element carries no recognized tag annotation, the result is
 * {@link TagResolution.Untagged}. If exactly one recognized tag annotation
 * is found, the result is {@link TagResolution.Tagged} with the corresponding
 * {@link TagInfo}. If multiple recognized tag annotations are found, the
 * first one encountered (in declaration order) is used and a duplicate-tag
 * warning is emitted via the {@link Messager}.
 */
public final class TagResolver {

    /**
     * Resolves the effective tag for an element by inspecting its annotations.
     *
     * @param element  the element to inspect
     * @param tagMap   the known tag annotations (FQN → {@link TagInfo})
     * @param messager for emitting duplicate-tag warnings
     * @return the tag resolution result
     */
    public TagResolution resolve(
            final Element element,
            final Map<String, TagInfo> tagMap,
            final Messager messager
    ) {
        final List<TagInfo> matched = new ArrayList<>();

        for (final AnnotationMirror mirror : element.getAnnotationMirrors()) {
            final Element annotationElement = mirror.getAnnotationType().asElement();
            if (annotationElement instanceof TypeElement typeElement) {
                final String fqn = typeElement.getQualifiedName().toString();
                final TagInfo info = tagMap.get(fqn);
                if (info != null) {
                    matched.add(info);
                }
            }
        }

        if (matched.isEmpty()) {
            return new TagResolution.Untagged();
        }

        if (matched.size() > 1) {
            final TagInfo first = matched.get(0);
            final TagInfo second = matched.get(1);
            messager.printMessage(
                    Diagnostic.Kind.WARNING,
                    "multiple tag annotations on element; using @"
                            + simpleName(first) + ", ignoring @" + simpleName(second),
                    element
            );
        }

        return new TagResolution.Tagged(matched.get(0));
    }

    /**
     * Extracts the simple name from a fully qualified annotation name.
     * For example, {@code "com.example.UserId"} becomes {@code "UserId"}.
     */
    private static String simpleName(final TagInfo tag) {
        final String fqn = tag.annotationFqn();
        final int dot = fqn.lastIndexOf('.');
        return dot < 0 ? fqn : fqn.substring(dot + 1);
    }
}
