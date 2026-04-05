package rawit;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import rawit.processors.RawitAnnotationProcessor;

import javax.lang.model.SourceVersion;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Validates Java 17 compatibility requirements.
 *
 * Validates: Requirements 1.3, 4.2, 5.1, 5.2, 5.3
 */
class Java17CompatibilityTest {

    private static Document pom;
    private static String readmeContent;

    @BeforeAll
    static void setup() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(false);
        factory.setFeature("http://javax.xml.XMLConstants/feature/secure-processing", true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        DocumentBuilder builder = factory.newDocumentBuilder();
        pom = builder.parse(new File("pom.xml"));
        pom.getDocumentElement().normalize();

        readmeContent = Files.readString(new File("README.md").toPath());
    }

    private String propertyValue(String propertyName) {
        NodeList propertiesNodes = pom.getElementsByTagName("properties");
        for (int i = 0; i < propertiesNodes.getLength(); i++) {
            Element propertiesEl = (Element) propertiesNodes.item(i);
            NodeList children = propertiesEl.getChildNodes();
            for (int j = 0; j < children.getLength(); j++) {
                if (children.item(j) instanceof Element el && el.getTagName().equals(propertyName)) {
                    return el.getTextContent().trim();
                }
            }
        }
        return null;
    }

    // Validates: Requirement 1.3
    @Test
    void pomJavaVersionPropertyIs17() {
        assertEquals("17", propertyValue("java.version"),
                "pom.xml <java.version> must be 17");
    }

    // Validates: Requirement 4.2
    @Test
    void getSupportedSourceVersionReturnsLatestSupported() {
        RawitAnnotationProcessor processor = new RawitAnnotationProcessor();
        assertEquals(SourceVersion.latestSupported(), processor.getSupportedSourceVersion(),
                "getSupportedSourceVersion() must return SourceVersion.latestSupported()");
    }

    // Validates: Requirements 5.1, 5.2
    @Test
    void readmeContainsJava17AsMinimumRequirement() {
        assertTrue(readmeContent.contains("Java 17"),
                "README.md must contain 'Java 17' as the minimum requirement");
    }

    // Validates: Requirement 5.3
    @Test
    void readmeDoesNotContainJava21AsMinimumRequirement() {
        // "Java 21" must not appear as a minimum requirement.
        // We check that the README does not contain "Java 21" at all,
        // since any such reference would imply it as a minimum.
        assertFalse(readmeContent.contains("Java 21"),
                "README.md must not contain 'Java 21' as a minimum requirement");
    }
}
