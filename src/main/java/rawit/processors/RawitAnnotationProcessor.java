package rawit.processors;

import rawit.processors.validation.ElementValidator;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
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
  private ElementValidator elementValidator;

  @Override
  public final synchronized void init(final ProcessingEnvironment processingEnv) {
    super.init(processingEnv);
    this.messager = processingEnv.getMessager();
    this.elementValidator = new ElementValidator();
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
      final String fqn = annotation.getQualifiedName().toString();
      if (!CURRY_ANNOTATION_FQN.equals(fqn) && !CONSTRUCTOR_ANNOTATION_FQN.equals(fqn)) {
        continue;
      }

      for (final Element annotated : roundEnv.getElementsAnnotatedWith(annotation)) {
        elementValidator.validate(annotated, messager);
      }
    }

    // Return false to allow other processors to process @Curry/@Constructor as well, if any.
    return false;
  }
}
