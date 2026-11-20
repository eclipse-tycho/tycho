package tycho.demo.apt.processor;

import java.io.IOException;
import java.io.Writer;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.StandardLocation;

/**
 * Collects all elements annotated with {@code @Documentation} and writes them
 * to {@code doc/documentation.yml} in the class output folder, so the file ends
 * up in the packaged bundle.
 * <p>
 * The annotation is matched by name only, so this processor jar has no
 * dependency on the bundle that defines the annotation.
 */
@SupportedAnnotationTypes("tycho.demo.apt.annotations.Documentation")
public class DocumentationProcessor extends AbstractProcessor {

	private final Map<String, String> entries = new TreeMap<>();

	@Override
	public SourceVersion getSupportedSourceVersion() {
		return SourceVersion.latestSupported();
	}

	@Override
	public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
		for (TypeElement annotation : annotations) {
			for (Element element : roundEnv.getElementsAnnotatedWith(annotation)) {
				for (AnnotationMirror mirror : element.getAnnotationMirrors()) {
					if (mirror.getAnnotationType().asElement().equals(annotation)) {
						entries.put(getName(element), getValue(mirror));
					}
				}
			}
		}
		if (roundEnv.processingOver() && !entries.isEmpty()) {
			write();
		}
		return false;
	}

	private static String getName(Element element) {
		if (element instanceof TypeElement type) {
			return type.getQualifiedName().toString();
		}
		return ((TypeElement) element.getEnclosingElement()).getQualifiedName() + "#" + element.getSimpleName();
	}

	private static String getValue(AnnotationMirror mirror) {
		for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : mirror.getElementValues()
				.entrySet()) {
			if (entry.getKey().getSimpleName().contentEquals("value")) {
				return String.valueOf(entry.getValue().getValue());
			}
		}
		return "";
	}

	private void write() {
		try {
			FileObject file = processingEnv.getFiler().createResource(StandardLocation.CLASS_OUTPUT, "",
					"doc/documentation.yml");
			try (Writer writer = file.openWriter()) {
				for (Map.Entry<String, String> entry : entries.entrySet()) {
					writer.write("- element: \"" + entry.getKey() + "\"\n");
					writer.write("  description: \"" + entry.getValue().replace("\"", "\\\"") + "\"\n");
				}
			}
		} catch (IOException e) {
			processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Writing documentation failed: " + e);
		}
	}
}
