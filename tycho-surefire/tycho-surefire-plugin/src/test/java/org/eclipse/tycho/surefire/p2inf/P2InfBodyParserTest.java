package org.eclipse.tycho.surefire.p2inf;

import java.io.File;
import java.io.IOException;
import java.util.List;

import junit.framework.TestCase;

import org.apache.maven.plugin.logging.Log;
import org.eclipse.equinox.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.sisu.equinox.launching.BundleStartLevel;
import org.eclipse.tycho.ArtifactDescriptor;
import org.eclipse.tycho.surefire.p2inf.P2InfBodyParser;
import org.eclipse.tycho.surefire.p2inf.P2InfLoader;

public class P2InfBodyParserTest extends TestCase {

    private Log log;
    private P2InfBodyParser bodyParser;
    private P2InfLoader p2InfLoader;

    public void setUp() {
        log = null;
        p2InfLoader = new P2InfLoader(log);
        bodyParser = new P2InfBodyParser(log);
    }

    public void testloadStartLevelsFromInvalidSource() {
        InstallableUnitDescription[] description = p2InfLoader.loadInstallableUnitDescription(null);
        assertNull(description);
    }

    public void testloadStartLevels() throws IOException {
        File file = P2infLoaderTest.getResource(P2infLoaderTest.EXAMPLE_BUNLE_ZIP);
        ArtifactDescriptor descriptor = P2infLoaderTest.createArtifactDescriptor(file, "1.0.0", "eclipse-feature",
                "example.bundle");

        InstallableUnitDescription[] description = p2InfLoader.loadInstallableUnitDescription(descriptor);
        List<BundleStartLevel> startLevels = bodyParser.getStartLevels(description);
        assertNotNull(startLevels);
        assertEquals(2, startLevels.size());

        BundleStartLevel bundle0 = getFromList(startLevels, "org.apache.aries.blueprint");
        assertNotNull(bundle0);
        assertTrue(bundle0.isAutoStart());
        assertEquals(3, bundle0.getLevel());

        BundleStartLevel bundle1 = getFromList(startLevels, "org.apache.aries.proxy");
        assertNotNull(bundle1);
        assertTrue(bundle1.isAutoStart());
        assertEquals(3, bundle1.getLevel());
    }

    private BundleStartLevel getFromList(List<BundleStartLevel> startLevels, String bundleId) {
        for (BundleStartLevel bundle : startLevels) {
            if (bundle.getId().equals(bundleId)) {
                return bundle;
            }
        }

        return null;
    }

    public void testIsStartLevelValid() {
        assertFalse(bodyParser.isStartLevelValid(null));
        assertFalse(bodyParser.isStartLevelValid(""));
        assertFalse(bodyParser.isStartLevelValid("a"));

        for (int i = -1; i < 10; i++) {
            assertTrue(bodyParser.isStartLevelValid(i + ""));
        }
    }

    public void testgetStartLveleParameter() {
        String body = "org.eclipse.equinox.p2.touchpoint.eclipse.setStartLevel(startLevel:XXX); org.eclipse.equinox.p2.touchpoint.eclipse.markStarted(started:true);";

        for (int i = 1; i < 10; i++) {
            String parameter = bodyParser.getP2infParameter(body.replace("XXX", "" + i),
                    P2InfBodyParser.START_LEVEL_REGEX, "\\d+");
            assertNotNull(parameter);
            assertEquals(i + "", parameter);
        }

        assertNull(bodyParser.getP2infParameter(null, P2InfBodyParser.START_LEVEL_REGEX, "\\d+"));
        assertNull(bodyParser.getP2infParameter("", P2InfBodyParser.START_LEVEL_REGEX, "\\d+"));
        assertNull(bodyParser.getP2infParameter("org.eclipse.equinox.p2.touchpoint.eclipse.markStarted(started:true);",
                P2InfBodyParser.START_LEVEL_REGEX, "\\d+"));
        assertNull(bodyParser.getP2infParameter("something.setStartLevel(startLevel:123); ",
                P2InfBodyParser.START_LEVEL_REGEX, "\\d+"));
    }

    public void testgetAutostartParameter() {
        String parameter = getAutostartParameter("org.eclipse.equinox.p2.touchpoint.eclipse.setStartLevel(startLevel:3); org.eclipse.equinox.p2.touchpoint.eclipse.markStarted(started:true);");
        assertNotNull(parameter);
        assertTrue(Boolean.parseBoolean(parameter));

        parameter = getAutostartParameter("org.eclipse.equinox.p2.touchpoint.eclipse.setStartLevel(startLevel:3); org.eclipse.equinox.p2.touchpoint.eclipse.markStarted(started:false);");
        assertNotNull(parameter);
        assertFalse(Boolean.parseBoolean(parameter));

        parameter = getAutostartParameter("org.eclipse.equinox.p2.touchpoint.eclipse.setStartLevel(startLevel:3); org.eclipse.equinox.p2.touchpoint.eclipse.markStarted(started:);");
        assertNull(parameter);

        parameter = getAutostartParameter("org.eclipse.equinox.p2.touchpoint.eclipse.setStartLevel(startLevel:3); org.eclipse.equinox.p2.touchpoint.eclipse.markStarted(started:XYZ);");
        assertNull(parameter);

        parameter = getAutostartParameter("org.eclipse.equinox.p2.touchpoint.eclipse.setStartLevel(startLevel:3); org.eclipse.equinox.p2.touchpoint.eclipse.markStarted(started:XYZ);");
        assertNull(parameter);

        parameter = getAutostartParameter(null);
        assertNull(parameter);

        parameter = getAutostartParameter("");
        assertNull(parameter);
    }

    private String getAutostartParameter(String body) {
        return bodyParser.getP2infParameter(body, P2InfBodyParser.MARKED_STATED_REGEX, "true|false");
    }
}
