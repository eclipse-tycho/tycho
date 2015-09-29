package org.eclipse.tycho.versions.bundle.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.tycho.versions.bundle.MutableManifestElement;
import org.junit.Test;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;

public class MutableManifestElementTest {

    @Test
    public void testParseHeader() throws BundleException {
        String importPackage = "org.bundle1.package1;version=\"1.0.0\",\n"
                + " org.bundle1.package2;version=\"2.0.0\";resolution:=optional";
        List<MutableManifestElement> parsed = MutableManifestElement.parseHeader(Constants.IMPORT_PACKAGE,
                importPackage);

        MutableManifestElement package1 = parsed.get(0);
        assertEquals("org.bundle1.package1", package1.getValue());
        assertEquals("1.0.0", package1.getAttribute("version"));

        MutableManifestElement package2 = parsed.get(1);
        assertEquals("org.bundle1.package2", package2.getValue());
        assertEquals("2.0.0", package2.getAttribute("version"));

    }

    @Test
    public void testGetAttribute() {
        MutableManifestElement requireBundle = new MutableManifestElement("bundle1",
                Collections.singletonMap(Constants.BUNDLE_VERSION_ATTRIBUTE, "1.0.0"),
                Collections.singletonMap(Constants.VISIBILITY_DIRECTIVE, Constants.VISIBILITY_REEXPORT));

        assertEquals("1.0.0", requireBundle.getAttribute(Constants.BUNDLE_VERSION_ATTRIBUTE));

        assertNull(requireBundle.getAttribute(Constants.VERSION_ATTRIBUTE));
    }

    @Test
    public void testSetAttribute() {
        MutableManifestElement requireBundle = new MutableManifestElement("bundle1",
                Collections.singletonMap(Constants.BUNDLE_VERSION_ATTRIBUTE, "1.0.0"),
                Collections.<String, String> emptyMap());

        requireBundle.setAttribute(Constants.BUNDLE_VERSION_ATTRIBUTE, "2.0.0");

        assertEquals("bundle1;bundle-version=\"2.0.0\"", requireBundle.write());
    }

    @Test
    public void testWriteValueOnly() {
        MutableManifestElement bundleVersion = new MutableManifestElement("2.0.0.qualifier",
                Collections.<String, String> emptyMap(), Collections.<String, String> emptyMap());

        assertEquals("2.0.0.qualifier", bundleVersion.write());
    }

    @Test
    public void testWriteAttirbuteAndDirective() {
        MutableManifestElement requireBundle = new MutableManifestElement("bundle1",
                Collections.singletonMap(Constants.BUNDLE_VERSION_ATTRIBUTE, "1.0.0"),
                Collections.singletonMap(Constants.VISIBILITY_DIRECTIVE, Constants.VISIBILITY_REEXPORT));

        assertEquals("bundle1;bundle-version=\"1.0.0\";visibility:=reexport", requireBundle.write());
    }

    @Test
    public void testWriteNotWrappingUsesDirective() {
        MutableManifestElement exportPackage = new MutableManifestElement("com.package2",
                Collections.singletonMap(Constants.VERSION_ATTRIBUTE, "1.1.0"),
                Collections.singletonMap(Constants.USES_DIRECTIVE, "org.eclipse.whatever1, org.eclipse.whatever2"));

        // When uses directive contains less than 3 elements it is not wrapped
        assertEquals("com.package2;version=\"1.1.0\";uses:=\"org.eclipse.whatever1,org.eclipse.whatever2\"",
                exportPackage.write());
    }

    @Test
    public void testWriteWrappingUsesDirective() {
        MutableManifestElement exportPackage = new MutableManifestElement("com.package2",
                Collections.singletonMap(Constants.VERSION_ATTRIBUTE, "1.1.0"),
                Collections.singletonMap(Constants.USES_DIRECTIVE,
                        "org.eclipse.whatever1, org.eclipse.whatever2, org.eclipse.whatever3, org.eclipse.whatever4"));

        // When uses directive contains 3 or more elements it is wrapped
        assertEquals(
                "com.package2;version=\"1.1.0\";\n"
                        + "  uses:=\"org.eclipse.whatever1,\n   org.eclipse.whatever2,\n   org.eclipse.whatever3,\n   org.eclipse.whatever4\"",
                exportPackage.write());
    }

    @Test
    public void testWriteWrappingUsesAndXFriendsDirective() {

        Map<String, String> directivesMap = new HashMap<>();
        directivesMap.put(Constants.USES_DIRECTIVE, "org.eclipse.whatever1, org.eclipse.whatever2");
        directivesMap.put("x-friends", "bundle.x, bundle.y");
        MutableManifestElement exportPackage = new MutableManifestElement("com.package2",
                Collections.singletonMap(Constants.VERSION_ATTRIBUTE, "1.1.0"), directivesMap);

        // When uses directive contains 3 or more elements it is wrapped
        assertEquals("com.package2;version=\"1.1.0\";\n" + "  x-friends:=\"bundle.x,\n   bundle.y\";\n"
                + "  uses:=\"org.eclipse.whatever1,\n   org.eclipse.whatever2\"", exportPackage.write());
    }

}
