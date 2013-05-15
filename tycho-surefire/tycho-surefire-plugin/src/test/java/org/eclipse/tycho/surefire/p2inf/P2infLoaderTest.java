package org.eclipse.tycho.surefire.p2inf;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Set;

import junit.framework.TestCase;

import org.apache.maven.plugin.logging.Log;
import org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.tycho.ArtifactDescriptor;
import org.eclipse.tycho.ArtifactKey;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.surefire.p2inf.P2InfLoader;

public class P2infLoaderTest extends TestCase {

    public static final String RESOURCES_DIR = "p2infs";
    public static final String EXAMPLE_BUNLE_ZIP = RESOURCES_DIR + "\\example.bundle.zip";
    public static final String EMPTY_EXAMPLE_BUNLE_ZIP = RESOURCES_DIR + "\\epmpty.example.bundle.zip";
    private Log log;
    private P2InfLoader p2InfLoader;

    public void setUp() {
        log = null;
        p2InfLoader = new P2InfLoader(log);
    }

    public void testLoadP2inf() {
        InstallableUnitDescription[] description = p2InfLoader.loadInstallableUnitDescription(null);
        assertNull(description);

        File file = new File("not-existing-file");
        ArtifactDescriptor descriptor = createArtifactDescriptor(file, "1.0.0", "eclipse-feature", "example.bundle");
        description = p2InfLoader.loadInstallableUnitDescription(descriptor);
        assertNull(description);
    }

    public void testLoadP2infFromFolder() throws IOException {
        File file = getResource(RESOURCES_DIR);
        ArtifactDescriptor descriptor = createArtifactDescriptor(file, "1.0.0", "eclipse-feature", "example.bundle");
        InstallableUnitDescription[] description = p2InfLoader.loadInstallableUnitDescription(descriptor);

        assertNotNull(description);
        assertEquals(2, description.length);
    }

    public void testLoadP2infFromZip() throws IOException {
        final File file = getResource(EXAMPLE_BUNLE_ZIP);
        ArtifactDescriptor descriptor = createArtifactDescriptor(file, "1.0.0", "eclipse-feature", "example.bundle");
        InstallableUnitDescription[] description = p2InfLoader.loadInstallableUnitDescription(descriptor);

        assertNotNull(description);
        assertEquals(2, description.length);
    }

    public void testLoadP2infFromEmptyZip() throws IOException {
        final File file = getResource(EMPTY_EXAMPLE_BUNLE_ZIP);
        ArtifactDescriptor descriptor = createArtifactDescriptor(file, "1.0.0", "eclipse-feature", "example.bundle");
        InstallableUnitDescription[] description = p2InfLoader.loadInstallableUnitDescription(descriptor);

        assertNull(description);
    }

    public static File getResource(String fileName) throws IOException {
        final Enumeration<URL> urls = ClassLoader.getSystemClassLoader().getResources("");
        if (!urls.hasMoreElements()) {
            return null;
        }

        String resources = urls.nextElement().getFile();
        return fileName != null ? new File(resources, fileName) : new File(resources);
    }

    public static ArtifactDescriptor createArtifactDescriptor(final File file, final String version, final String type,
            final String id) {
        return new ArtifactDescriptor() {

            public ReactorProject getMavenProject() {
                return null;
            }

            public File getLocation() {
                return file;
            }

            public ArtifactKey getKey() {
                return new ArtifactKey() {

                    public String getVersion() {
                        return version;
                    }

                    public String getType() {
                        return type;
                    }

                    public String getId() {
                        return id;
                    }
                };
            }

            public Set<Object> getInstallableUnits() {
                return null;
            }

            public String getClassifier() {
                return null;
            }
        };
    }
}
