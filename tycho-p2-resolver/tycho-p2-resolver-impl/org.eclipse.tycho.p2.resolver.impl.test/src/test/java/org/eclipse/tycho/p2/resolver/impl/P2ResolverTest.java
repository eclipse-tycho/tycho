/*******************************************************************************
 * Copyright (c) 2008, 2011 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.resolver.impl;

import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.tycho.p2.impl.publisher.DefaultDependencyMetadataGenerator;
import org.eclipse.tycho.p2.impl.publisher.SourcesBundleDependencyMetadataGenerator;
import org.eclipse.tycho.p2.impl.resolver.DuplicateReactorIUsException;
import org.eclipse.tycho.p2.impl.resolver.P2ResolverImpl;
import org.eclipse.tycho.p2.impl.test.ArtifactMock;
import org.eclipse.tycho.p2.impl.test.NullMavenLogger;
import org.eclipse.tycho.p2.impl.test.P2RepositoryCacheImpl;
import org.eclipse.tycho.p2.metadata.DependencyMetadataGenerator;
import org.eclipse.tycho.p2.resolver.facade.P2ResolutionResult;
import org.eclipse.tycho.p2.resolver.facade.P2ResolutionResult.Entry;
import org.eclipse.tycho.p2.resolver.facade.P2Resolver;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class P2ResolverTest {

    private DependencyMetadataGenerator generator = new DefaultDependencyMetadataGenerator();

    private P2ResolverImpl impl;

    static void addMavenProject(P2ResolverImpl impl, File basedir, String packaging, String id) throws IOException {
        String version = "1.0.0-SNAPSHOT";

        impl.addMavenArtifact(new ArtifactMock(basedir.getCanonicalFile(), id, id, version, packaging));
    }

    static File getLocalRepositoryLocation() throws IOException {
        return new File("target/localrepo").getCanonicalFile();
    }

    static List<Map<String, String>> getEnvironments() {
        ArrayList<Map<String, String>> environments = new ArrayList<Map<String, String>>();

        Map<String, String> properties = new LinkedHashMap<String, String>();
        properties.put("osgi.os", "linux");
        properties.put("osgi.ws", "gtk");
        properties.put("osgi.arch", "x86_64");

        // TODO does not belong here
        properties.put("org.eclipse.update.install.features", "true");

        environments.add(properties);

        return environments;
    }

    static void delete(File dir) {
        if (dir == null || !dir.exists()) {
            return;
        }

        if (dir.isDirectory()) {
            File[] members = dir.listFiles();
            if (members != null) {
                for (File member : members) {
                    delete(member);
                }
            }
        }

        Assert.assertTrue("Delete " + dir.getAbsolutePath(), dir.delete());
    }

    @Before
    public void initDefaultResolver() throws Exception {
        impl = new P2ResolverImpl();
        impl.setLogger(new NullMavenLogger());
        impl.setRepositoryCache(new P2RepositoryCacheImpl());
        impl.setLocalRepositoryLocation(getLocalRepositoryLocation());
        impl.setEnvironments(getEnvironments());
    }

    public void stopResolver() {
        impl.stop();
    }

    @Test
    public void basic() throws Exception {
        impl.addP2Repository(new File("resources/repositories/e342").getCanonicalFile().toURI());

        File bundle = new File("resources/resolver/bundle01").getCanonicalFile();
        String groupId = "org.eclipse.tycho.p2.impl.resolver.test.bundle01";
        String artifactId = "org.eclipse.tycho.p2.impl.resolver.test.bundle01";
        String version = "1.0.0-SNAPSHOT";

        ArtifactMock a = new ArtifactMock(bundle, groupId, artifactId, version, P2Resolver.TYPE_ECLIPSE_PLUGIN);
        a.setDependencyMetadata(generator.generateMetadata(a, getEnvironments()));

        impl.addReactorArtifact(a);

        List<P2ResolutionResult> results = impl.resolveProject(bundle);

        Assert.assertEquals(1, results.size());
        P2ResolutionResult result = results.get(0);

        Assert.assertEquals(2, result.getArtifacts().size());
        Assert.assertEquals(1, result.getNonReactorUnits().size());
    }

    @Test
    public void siteConflictingDependenciesResolver() throws IOException {
        impl.addP2Repository(new File("resources/repositories/e342").getCanonicalFile().toURI());

        File[] projects = new File[] { new File("resources/siteresolver/bundle342").getCanonicalFile(),
                new File("resources/siteresolver/bundle352").getCanonicalFile(),
                new File("resources/siteresolver/feature342").getCanonicalFile(),
                new File("resources/siteresolver/feature352").getCanonicalFile(),
                new File("resources/siteresolver/site").getCanonicalFile() };

        addMavenProject(impl, projects[0], P2Resolver.TYPE_ECLIPSE_PLUGIN, "bundle342");
        addMavenProject(impl, projects[1], P2Resolver.TYPE_ECLIPSE_PLUGIN, "bundle352");
        addMavenProject(impl, projects[2], P2Resolver.TYPE_ECLIPSE_FEATURE, "feature342");
        addMavenProject(impl, projects[3], P2Resolver.TYPE_ECLIPSE_FEATURE, "feature352");

        File basedir = projects[4];
        addMavenProject(impl, basedir, P2Resolver.TYPE_ECLIPSE_UPDATE_SITE, "site");

        P2ResolutionResult result = impl.collectProjectDependencies(basedir);

        Assert.assertEquals(projects.length, result.getArtifacts().size());
        for (File project : projects) {
            assertContainLocation(result, project);
        }

        // conflicting dependency mode only collects included artifacts - the referenced non-reactor unit
        // org.eclipse.osgi is not included
        Assert.assertEquals(0, result.getNonReactorUnits().size());
    }

    private static void assertContainLocation(P2ResolutionResult result, File location) {
        for (P2ResolutionResult.Entry entry : result.getArtifacts()) {
            if (entry.getLocation().equals(location)) {
                return;
            }
        }
        Assert.fail();
    }

    @Test
    public void duplicateInstallableUnit() throws Exception {
        File projectLocation = new File("resources/duplicate-iu/featureA").getCanonicalFile();

        ArtifactMock a1 = new ArtifactMock(projectLocation, "groupId", "featureA", "1.0.0-SNAPSHOT",
                P2Resolver.TYPE_ECLIPSE_FEATURE);
        a1.setDependencyMetadata(generator.generateMetadata(a1, getEnvironments()));

        ArtifactMock a2 = new ArtifactMock(new File("resources/duplicate-iu/featureA2").getCanonicalFile(), "groupId",
                "featureA2", "1.0.0-SNAPSHOT", P2Resolver.TYPE_ECLIPSE_FEATURE);
        a2.setDependencyMetadata(generator.generateMetadata(a2, getEnvironments()));

        impl.addReactorArtifact(a1);
        impl.addReactorArtifact(a2);

        try {
            impl.resolveProject(projectLocation);
            fail();
        } catch (DuplicateReactorIUsException e) {
            // TODO proper assertion
        }
    }

    @Test
    public void featureInstallableUnits() throws Exception {
        File bundle = new File("resources/resolver/feature01").getCanonicalFile();
        String groupId = "org.eclipse.tycho.p2.impl.resolver.test.feature01";
        String artifactId = "org.eclipse.tycho.p2.impl.resolver.test.feature01";
        String version = "1.0.0-SNAPSHOT";

        impl.addMavenArtifact(new ArtifactMock(bundle, groupId, artifactId, version, P2Resolver.TYPE_ECLIPSE_FEATURE));

        List<P2ResolutionResult> results = impl.resolveProject(bundle);

        Assert.assertEquals(1, results.size());
        P2ResolutionResult result = results.get(0);

        Assert.assertEquals(1, result.getArtifacts().size());
        Assert.assertEquals(1, result.getArtifacts().iterator().next().getInstallableUnits().size());
        Assert.assertEquals(0, result.getNonReactorUnits().size());
    }

    @Test
    public void sourceBundle() throws Exception {
        File feature = new File("resources/sourcebundles/feature01").getCanonicalFile();
        String featureId = "org.eclipse.tycho.p2.impl.resolver.test.feature01";
        String featureVersion = "1.0.0-SNAPSHOT";

        ArtifactMock f = new ArtifactMock(feature, featureId, featureId, featureVersion,
                P2Resolver.TYPE_ECLIPSE_FEATURE);
        f.setDependencyMetadata(generator.generateMetadata(f, getEnvironments()));
        impl.addReactorArtifact(f);

        File bundle = new File("resources/sourcebundles/bundle01").getCanonicalFile();
        String bundleId = "org.eclipse.tycho.p2.impl.resolver.test.bundle01";
        String bundleVersion = "1.0.0-SNAPSHOT";
        ArtifactMock b = new ArtifactMock(bundle, bundleId, bundleId, bundleVersion, P2Resolver.TYPE_ECLIPSE_PLUGIN);
        b.setDependencyMetadata(generator.generateMetadata(b, getEnvironments()));
        impl.addReactorArtifact(b);

        ArtifactMock sb = new ArtifactMock(bundle, bundleId, bundleId, bundleVersion, P2Resolver.TYPE_ECLIPSE_PLUGIN,
                "sources");
        sb.setDependencyMetadata(new SourcesBundleDependencyMetadataGenerator().generateMetadata(sb, getEnvironments()));
        impl.addReactorArtifact(sb);

        List<P2ResolutionResult> results = impl.resolveProject(feature);

        Assert.assertEquals(1, results.size());
        P2ResolutionResult result = results.get(0);

        Assert.assertEquals(3, result.getArtifacts().size());
        List<P2ResolutionResult.Entry> entries = new ArrayList<P2ResolutionResult.Entry>(result.getArtifacts());
        Collections.sort(entries, new Comparator<Entry>() {

            public int compare(Entry entry1, Entry entry2) {
                return entry1.getId().compareTo(entry2.getId());
            }
        });
        Assert.assertEquals("org.eclipse.tycho.p2.impl.resolver.test.bundle01", entries.get(0).getId());
        Assert.assertEquals("org.eclipse.tycho.p2.impl.resolver.test.bundle01.source", entries.get(1).getId());
        Assert.assertEquals("org.eclipse.tycho.p2.impl.resolver.test.feature01", entries.get(2).getId());
        Assert.assertEquals(bundle, entries.get(0).getLocation());
        Assert.assertEquals(bundle, entries.get(1).getLocation());
        Assert.assertEquals("sources", entries.get(1).getClassifier());
    }

    @Test
    public void eclipseRepository() throws Exception {
        impl.addP2Repository(new File("resources/repositories/e342").getCanonicalFile().toURI());
        // launchers currently cannot be disabled (see TYCHO-511/TYCHO-512)
        impl.addP2Repository(new File("resources/repositories/launchers").getCanonicalFile().toURI());

        File projectDir = new File("resources/resolver/repository").getCanonicalFile();
        String groupId = "org.eclipse.tycho.p2.impl.resolver.test.repository";
        String artifactId = "org.eclipse.tycho.p2.impl.resolver.test.repository";
        String version = "1.0.0-SNAPSHOT";

        addMavenProject(impl, new File("resources/resolver/bundle01"), P2Resolver.TYPE_ECLIPSE_PLUGIN, "bundle01");

        ArtifactMock module = new ArtifactMock(projectDir, groupId, artifactId, version,
                P2Resolver.TYPE_ECLIPSE_REPOSITORY);
        module.setDependencyMetadata(generator.generateMetadata(module, getEnvironments()));

        impl.addReactorArtifact(module);

        List<P2ResolutionResult> results = impl.resolveProject(projectDir);

        Assert.assertEquals(1, results.size());
        P2ResolutionResult result = results.get(0);

        Assert.assertEquals(3, result.getArtifacts().size()); // the product, bundle01, and the one dependency of bundle01
        Assert.assertEquals(4, result.getNonReactorUnits().size());

        assertContainsUnit("org.eclipse.osgi", result.getNonReactorUnits());
        assertContainsUnit("org.eclipse.equinox.launcher", result.getNonReactorUnits());
        assertContainsUnit("org.eclipse.equinox.launcher.gtk.linux.x86_64", result.getNonReactorUnits());
        assertContainsUnit("org.eclipse.equinox.executable.feature.group", result.getNonReactorUnits());
    }

    @Test
    public void bundleUsesSWT() throws Exception {
        impl.addP2Repository(new File("resources/repositories/e361").getCanonicalFile().toURI());

        File bundle = new File("resources/resolver/bundleUsesSWT").getCanonicalFile();
        String groupId = "org.eclipse.tycho.p2.impl.resolver.test.bundleUsesSWT";
        String artifactId = "org.eclipse.tycho.p2.impl.resolver.test.bundleUsesSWT";
        String version = "1.0.0-SNAPSHOT";

        ArtifactMock a = new ArtifactMock(bundle, groupId, artifactId, version, P2Resolver.TYPE_ECLIPSE_PLUGIN);
        a.setDependencyMetadata(generator.generateMetadata(a, getEnvironments()));

        impl.addReactorArtifact(a);

        List<P2ResolutionResult> results = impl.resolveProject(bundle);

        Assert.assertEquals(1, results.size());
        P2ResolutionResult result = results.get(0);

        Assert.assertEquals(3, result.getArtifacts().size());
        Assert.assertEquals(2, result.getNonReactorUnits().size());

        assertContainsUnit("org.eclipse.swt", result.getNonReactorUnits());
        assertContainsUnit("org.eclipse.swt.gtk.linux.x86_64", result.getNonReactorUnits());
    }

    private static void assertContainsUnit(String unitID, Set<?> units) {
        Assert.assertFalse("Unit " + unitID + " not found", getInstallableUnits(unitID, units).isEmpty());
    }

    private static List<IInstallableUnit> getInstallableUnits(String unitID, Set<?> units) {
        List<IInstallableUnit> result = new ArrayList<IInstallableUnit>();
        for (Object unitObject : units) {
            IInstallableUnit unit = (IInstallableUnit) unitObject;
            if (unitID.equals(unit.getId())) {
                result.add(unit);
            }
        }
        return result;
    }

    @Test
    public void reactorVsExternal() throws Exception {
        impl.addP2Repository(new File("resources/reactor-vs-external/extrepo").getCanonicalFile().toURI());

        ArtifactMock bundle01 = new ArtifactMock(new File("resources/reactor-vs-external/bundle01").getCanonicalFile(),
                "groupId", "org.sonatype.tycho.p2.impl.resolver.test.bundle01", "1.0.0.qualifier",
                P2Resolver.TYPE_ECLIPSE_PLUGIN);
        bundle01.setDependencyMetadata(generator.generateMetadata(bundle01, getEnvironments()));
        impl.addReactorArtifact(bundle01);

        ArtifactMock feature01 = new ArtifactMock(
                new File("resources/reactor-vs-external/feature01").getCanonicalFile(), "groupId",
                "org.sonatype.tycho.p2.impl.resolver.test.feature01", "1.0.0.qualifier",
                P2Resolver.TYPE_ECLIPSE_FEATURE);
        feature01.setDependencyMetadata(generator.generateMetadata(feature01, getEnvironments()));
        impl.addReactorArtifact(feature01);

        List<P2ResolutionResult> results = impl.resolveProject(feature01.getLocation());

        Assert.assertEquals(1, results.size());
        P2ResolutionResult result = results.get(0);

        Assert.assertEquals(2, result.getArtifacts().size());
        Assert.assertEquals(0, result.getNonReactorUnits().size());

        for (Entry entry : result.getArtifacts()) {
            Assert.assertEquals("1.0.0.qualifier", entry.getVersion());
        }
    }
}
