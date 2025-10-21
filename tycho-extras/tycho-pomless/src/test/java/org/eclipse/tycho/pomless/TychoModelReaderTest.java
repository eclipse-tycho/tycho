/*******************************************************************************
 * Copyright (c) 2015, 2020 SAP SE and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    SAP SE - initial API and implementation
 *    Christoph Läubrich - adjust to API
 *******************************************************************************/
package org.eclipse.tycho.pomless;

import static org.codehaus.plexus.testing.PlexusExtension.getBasedir;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.apache.maven.model.InputLocation;
import org.apache.maven.model.InputSource;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.building.ModelProcessor;
import org.apache.maven.model.io.ModelParseException;
import org.apache.maven.model.io.ModelReader;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.testing.PlexusTest;
import org.junit.jupiter.api.Test;
import org.sonatype.maven.polyglot.mapping.Mapping;

@PlexusTest
public class TychoModelReaderTest {

    @Inject
    private PlexusContainer container;

    @Test
    public void testReadBundle() throws Exception {
        File buildProperties = new File(getPolyglotTestDir(), "bundle1/" + TychoBundleMapping.META_INF_DIRECTORY);
        Model model = getTychoModelReader(TychoBundleMapping.PACKAGING).read(buildProperties,
                createReaderOptions(buildProperties));
        assertEquals("4.0.0", model.getModelVersion());
        assertEquals("pomless.bundle", model.getArtifactId());
        assertEquals("[bundle] Pomless Bundle", model.getName());
        assertEquals("0.1.0-SNAPSHOT", model.getVersion());
        assertEquals("eclipse-plugin", model.getPackaging());
        assertParent(model.getParent());
        assertLocation("bundle1/META-INF", model.getLocation(""));
    }

    @Test
    public void testReadBundle2() throws Exception {
        File buildProperties = new File(getPolyglotTestDir(), "bundle2/" + TychoBundleMapping.META_INF_DIRECTORY);
        Model model = getTychoModelReader(TychoBundleMapping.PACKAGING).read(buildProperties,
                createReaderOptions(buildProperties));
        assertEquals("4.0.0", model.getModelVersion());
        assertEquals("pomless.bundle", model.getArtifactId());
        assertEquals("[bundle] Pomless Bundle", model.getName());
        assertEquals("0.1.0-SNAPSHOT", model.getVersion());
        assertEquals("eclipse-plugin", model.getPackaging());
        assertParent(model.getParent());
        assertLocation("bundle2/META-INF", model.getLocation(""));
    }

    @Test
    public void testReadBundle3() throws Exception {
        File buildProperties = new File(getPolyglotTestDir(), "bundle3/" + TychoBundleMapping.META_INF_DIRECTORY);
        Model model = getTychoModelReader(TychoBundleMapping.PACKAGING).read(buildProperties,
                createReaderOptions(buildProperties));
        assertEquals("4.0.0", model.getModelVersion());
        assertEquals("pomless.bundle", model.getArtifactId());
        assertEquals("[bundle] Pomless Bundle", model.getName());
        assertEquals("0.1.0-SNAPSHOT", model.getVersion());
        assertEquals("eclipse-plugin", model.getPackaging());
        assertParent(model.getParent());
        assertLocation("bundle3/META-INF", model.getLocation(""));
    }

    @Test
    public void testReadTestBundle() throws Exception {
        File buildProperties = new File(getPolyglotTestDir(), "bundle1.tests/" + TychoBundleMapping.META_INF_DIRECTORY);
        Model model = getTychoModelReader(TychoBundleMapping.PACKAGING).read(buildProperties,
                createReaderOptions(buildProperties));
        assertEquals("pomless.bundle.tests", model.getArtifactId());
        assertEquals("[test-bundle] Pomless Test Bundle", model.getName());
        assertEquals("1.0.1", model.getVersion());
        assertEquals("eclipse-test-plugin", model.getPackaging());
        assertParent(model.getParent());
        assertLocation("bundle1.tests/META-INF", model.getLocation(""));
    }

    @Test
    public void testReadFeature() throws Exception {
        File feature = new File(getPolyglotTestDir(), "feature/feature.xml");
        Model model = getTychoModelReader(TychoFeatureMapping.PACKAGING).read(feature, createReaderOptions(feature));
        assertEquals("pomless.feature", model.getArtifactId());
        assertEquals("[feature] Pomless Feature", model.getName());
        assertEquals("1.0.0-SNAPSHOT", model.getVersion());
        assertEquals("eclipse-feature", model.getPackaging());
        assertParent(model.getParent());
        assertLocation("feature/feature.xml", model.getLocation(""));
    }

    @Test
    public void testReadFeature2() throws Exception {
        File feature = new File(getPolyglotTestDir(), "feature2/feature.xml");
        Model model = getTychoModelReader(TychoFeatureMapping.PACKAGING).read(feature, createReaderOptions(feature));
        assertEquals("pomless.feature", model.getArtifactId());
        assertEquals("[feature] Pomless Feature 2", model.getName());
        assertEquals("Eclipse.org", model.getOrganization().getName());
        assertEquals("1.0.0-SNAPSHOT", model.getVersion());
        assertEquals("eclipse-feature", model.getPackaging());
        assertParent(model.getParent());
        assertLocation("feature2/feature.xml", model.getLocation(""));
    }

    @Test
    public void testMissingManifestOrFeature() throws Exception {
        File buildDir = new File(getTestResourcesDir(), "modelreader/missingManifestOrFeature/");
        assertNull(getMapping(TychoBundleMapping.PACKAGING).locatePom(buildDir));
        assertNull(getMapping(TychoFeatureMapping.PACKAGING).locatePom(buildDir));
        assertNull(getMapping(TychoRepositoryMapping.PACKAGING).locatePom(buildDir));
        assertNull(getMapping(TychoTargetMapping.PACKAGING).locatePom(buildDir));
    }

    @Test
    public void testIllFormedFeature() throws Exception {
        File featureXml = new File(getTestResourcesDir(), "modelreader/features/illFormed/feature.xml");
        assertThrows(ModelParseException.class, () -> getTychoModelReader(TychoFeatureMapping.PACKAGING)
                .read(featureXml, createReaderOptions(featureXml)));
    }

    @Test
    public void testFeatureWithoutId() throws Exception {
        File featureXml = new File(getTestResourcesDir(), "modelreader/features/missingId/feature.xml");
        ModelParseException e = assertThrows(ModelParseException.class,
                () -> getTychoModelReader(TychoFeatureMapping.PACKAGING).read(featureXml,
                        createReaderOptions(featureXml)));
        assertTrue(e.getMessage().contains("missing or empty 'id' attribute in element 'feature'"));
    }

    @Test
    public void testFeatureWithoutVersion() throws Exception {
        File featureXml = new File(getTestResourcesDir(), "modelreader/features/missingVersion/feature.xml");
        ModelParseException e = assertThrows(ModelParseException.class,
                () -> getTychoModelReader(TychoFeatureMapping.PACKAGING).read(featureXml,
                        createReaderOptions(featureXml)));
        assertTrue(e.getMessage().contains("missing or empty 'version' attribute in element 'feature'"));
    }

    @Test
    public void testBundleWithoutSymbolicName() throws Exception {
        File buildProperties = new File(getTestResourcesDir(),
                "modelreader/plugins/missingBsn/" + TychoBundleMapping.META_INF_DIRECTORY);
        ModelParseException e = assertThrows(ModelParseException.class,
                () -> getTychoModelReader(TychoBundleMapping.PACKAGING).read(buildProperties,
                        createReaderOptions(buildProperties)));
        assertTrue(e.getMessage().contains("Bundle-SymbolicName missing in"));
    }

    @Test
    public void testBundleWithoutVersion() throws Exception {
        File buildProperties = new File(getTestResourcesDir(),
                "modelreader/plugins/missingVersion/" + TychoBundleMapping.META_INF_DIRECTORY);
        ModelParseException e = assertThrows(ModelParseException.class,
                () -> getTychoModelReader(TychoBundleMapping.PACKAGING).read(buildProperties,
                        createReaderOptions(buildProperties)));
        assertTrue(e.getMessage().contains("Bundle-Version missing in"));
    }

    @Test
    public void testNoParent() throws Exception {
        File buildProperties = new File(getTestResourcesDir(),
                "modelreader/noParent/bundle/" + TychoBundleMapping.META_INF_DIRECTORY);
        FileNotFoundException e = assertThrows(FileNotFoundException.class,
                () -> getTychoModelReader(TychoBundleMapping.PACKAGING).read(buildProperties,
                        createReaderOptions(buildProperties)));
        assertTrue(e.getMessage().contains("No parent pom file found in"));
    }

    @Test
    public void testFindParent() throws Exception {
        File location = new File(getTestResourcesDir(),
                "modelreader/grandparentInheritance/bundle/" + TychoBundleMapping.META_INF_DIRECTORY);
        Model model = getTychoModelReader(TychoBundleMapping.PACKAGING).read(location, createReaderOptions(location));
        assertNotNull(model);
        Parent parentReference = model.getParent();
        assertEquals("bundle-parent", parentReference.getArtifactId());
        assertEquals("grandparent.groupid", parentReference.getGroupId());
        assertEquals("1.2.3", parentReference.getVersion());
    }

    @Test
    public void testReadProduct() throws ModelParseException, IOException, ComponentLookupException {
        File product = new File(getPolyglotTestDir(), "product/myproduct.product");
        Model model = getTychoModelReader(TychoRepositoryMapping.PACKAGING).read(product, createReaderOptions(product));
        assertEquals("4.0.0", model.getModelVersion());
        assertEquals("pomless.product.validproduct", model.getArtifactId());
        assertEquals("0.0.2-SNAPSHOT", model.getVersion());
        assertEquals("eclipse-repository", model.getPackaging());
        assertParent(model.getParent());
    }

    public ModelReader getTychoModelReader(String packaging) throws ComponentLookupException {
        return getMapping(packaging).getReader();
    }

    private Mapping getMapping(String packaging) throws ComponentLookupException {
        return container.lookup(Mapping.class, packaging);
    }

    @Test
    public void testProductWithoutUid() throws IOException, ComponentLookupException {
        File product = new File(getTestResourcesDir(), "modelreader/products/missingUid/myproduct.product");
        ModelParseException e = assertThrows(ModelParseException.class,
                () -> getTychoModelReader(TychoRepositoryMapping.PACKAGING).read(product,
                        createReaderOptions(product)));
        assertTrue(e.getMessage().contains("missing or empty 'uid' attribute in element 'product'"));
    }

    @Test
    public void testProductWithoutVersion() throws IOException, ComponentLookupException {
        File product = new File(getTestResourcesDir(), "modelreader/products/missingVersion/myproduct.product");
        getTychoModelReader(TychoRepositoryMapping.PACKAGING).read(product, createReaderOptions(product));
    }

    @Test()
    public void testIllFormedProduct() throws Exception {
        File product = new File(getTestResourcesDir(), "modelreader/products/illFormed/myproduct.product");
        assertThrows(ModelParseException.class, () -> getTychoModelReader(TychoRepositoryMapping.PACKAGING)
                .read(product, createReaderOptions(product)));
    }

    @Test
    public void testReadUpdateSite() throws ModelParseException, IOException, ComponentLookupException {
        File updatesite = new File(getTestResourcesDir(), "modelreader/updatesites/site/category.xml");
        Model model = getTychoModelReader(TychoRepositoryMapping.PACKAGING).read(updatesite,
                createReaderOptions(updatesite));
        assertEquals("4.0.0", model.getModelVersion());
        assertEquals("site.eclipse-repository", model.getArtifactId());
        assertEquals("0.0.1-SNAPSHOT", model.getVersion());
        assertEquals("eclipse-repository", model.getPackaging());
        assertParent(model.getParent());
    }

    private void assertParent(Parent parent) {
        assertNotNull(parent);
        assertEquals("testParent.groupId", parent.getGroupId());
        assertEquals("testparent", parent.getArtifactId());
        assertEquals("0.0.1-SNAPSHOT", parent.getVersion());
    }

    private void assertLocation(String expectedLocation, InputLocation location) {
        assertNotNull(location);
        assertEquals(0, location.getLineNumber());
        assertEquals(0, location.getColumnNumber());
        InputSource source = location.getSource();
        assertNotNull(source);
        assertEquals(new File(getPolyglotTestDir(), expectedLocation).toString(), source.getLocation());
        assertNotNull(source.getModelId());
        assertTrue(source.getModelId().matches("^testParent.groupId:.*:.*"));
    }

    private Map<String, String> createReaderOptions(File buildProperties) {
        Map<String, String> options = new HashMap<>();
        options.put(ModelProcessor.SOURCE, buildProperties.getAbsolutePath());
        return options;
    }

    private File getPolyglotTestDir() {
        return new File(getTestResourcesDir(), "testpomless/");
    }

    private File getTestResourcesDir() {
        return new File(getBasedir(), "src/test/resources/");
    }

}
