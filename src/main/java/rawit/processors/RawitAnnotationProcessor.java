package rawit.processors;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import java.util.Set;

/**
 * Annotation processor scaffold for @Curry and @Constructor.
 * <p>
 * This processor validates usage and is ready to generate code via Filer.
 */
@SupportedOptions("curry.debug")
public class RawitAnnotationProcessor
    extends AbstractProcessor {

  private static final String CURRY_ANNOTATION_FQN = "rawit.Curry";
  private static final String CONSTRUCTOR_ANNOTATION_FQN = "rawit.Constructor";

  private Messager messager;

  @Override
  public final synchronized void init(final ProcessingEnvironment processingEnv) {
    super.init(processingEnv);
    this.messager = processingEnv.getMessager();
  }

  @Override
  public final Set<String> getSupportedAnnotationTypes() {
    return Set.of(CURRY_ANNOTATION_FQN, CONSTRUCTOR_ANNOTATION_FQN);
  }

  @Override
  public final SourceVersion getSupportedSourceVersion() {
    return SourceVersion.latestSupported();
  }

  @Override
  public final boolean process(final Set<? extends TypeElement> annotations, final RoundEnvironment roundEnv) {
    if (annotations.isEmpty()) {
      return false; // Nothing to do this round
    }

    for (final TypeElement annotation : annotations) {
      if (!CURRY_ANNOTATION_FQN.equals(annotation.getQualifiedName().toString())) {
        continue; // Not our annotation
      }

      for (final Element annotated : roundEnv.getElementsAnnotatedWith(annotation)) {
        validateUsage(annotated);

        // TODO: Generate curry helpers or modified API using 'filer' if needed.
        // Example outline:
        // - Derive target class/package
        // - Build a source file (JavaFileObject) and write generated code
        // - Use messager.printMessage(Diagnostic.Kind.NOTE, "...") for debug
      }
    }

    // Return false to allow other processors to process @Curry as well, if any.
    return false;
  }

  private void validateUsage(final Element annotated) {
    // Example rule: @Curry is expected on methods (EXECUTABLE)
    if (annotated.getKind() != ElementKind.METHOD) {
      messager.printMessage(Diagnostic.Kind.ERROR, "@Curry can only be applied to methods.", annotated);
      return;
    }

    // Additional example validations can go here:
    // - Method should be non-private
    // - No type parameters or specific parameter constraints
    // - Return type conditions, etc.

    messager.printMessage(Diagnostic.Kind.NOTE, "Processing @Curry on: " + annotated.toString(), annotated);
  }
}
