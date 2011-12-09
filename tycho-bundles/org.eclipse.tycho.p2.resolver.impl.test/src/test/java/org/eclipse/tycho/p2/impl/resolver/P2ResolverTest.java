/*******************************************************************************
 * Copyright (c) 2008, 2011 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *    SAP AG - apply DRY principle
 *******************************************************************************/
package org.eclipse.tycho.p2.impl.resolver;

import static org.eclipse.tycho.p2.resolver.facade.P2Resolver.TYPE_ECLIPSE_FEATURE;
import static org.eclipse.tycho.p2.resolver.facade.P2Resolver.TYPE_ECLIPSE_PLUGIN;
import static org.eclipse.tycho.p2.resolver.facade.P2Resolver.TYPE_ECLIPSE_REPOSITORY;
import static org.eclipse.tycho.p2.resolver.facade.P2Resolver.TYPE_ECLIPSE_UPDATE_SITE;
import static org.eclipse.tycho.test.util.ResourceUtil.resourceFile;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.tycho.core.facade.MavenLogger;
import org.eclipse.tycho.p2.impl.publisher.SourcesBundleDependencyMetadataGenerator;
import org.eclipse.tycho.p2.impl.test.ArtifactMock;
import org.eclipse.tycho.p2.impl.test.MavenLoggerStub;
import org.eclipse.tycho.p2.resolver.facade.P2ResolutionResult;
import org.eclipse.tycho.p2.resolver.facade.P2ResolutionResult.Entry;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class P2ResolverTest extends P2ResolverTestBase {

    static File getLocalRepositoryLocation() throws IOException {
        return new File("target/localrepo").getCanonicalFile();
    }

    @Before
    public void initDefaultResolver() throws Exception {
        org.eclipse.equinox.internal.p2.core.helpers.Tracing.DEBUG_PLANNER_PROJECTOR = true;
        MavenLogger logger = new MavenLoggerStub();
        P2ResolverFactoryImpl p2ResolverFactory = createP2ResolverFactory(false);
        context = p2ResolverFactory.createResolutionContext(null, false);
        impl = new P2ResolverImpl(logger);
        impl.setEnvironments(getEnvironments());
    }

    @Test
    public void basic() throws Exception {
        context.addP2Repository(resourceFile("repositories/e342").toURI());

        File bundle = resourceFile("resolver/bundle01");
        String artifactId = "org.eclipse.tycho.p2.impl.resolver.test.bundle01";
        addReactorProject(bundle, TYPE_ECLIPSE_PLUGIN, artifactId);

        List<P2ResolutionResult> results = impl.resolveProject(context, bundle);

        Assert.assertEquals(1, results.size());
        P2ResolutionResult result = results.get(0);

        Assert.assertEquals(2, result.getArtifacts().size());
        Assert.assertEquals(1, result.getNonReactorUnits().size());
    }

    @Test
    public void siteConflictingDependenciesResolver() throws IOException {
        context.addP2Repository(resourceFile("repositories/e342").toURI());

        File[] projects = new File[] { resourceFile("siteresolver/bundle342"), //
                resourceFile("siteresolver/bundle352"), //
                resourceFile("siteresolver/feature342"), // 
                resourceFile("siteresolver/feature352"), // 
                resourceFile("siteresolver/site") };

        addContextProject(projects[0], TYPE_ECLIPSE_PLUGIN);
        addContextProject(projects[1], TYPE_ECLIPSE_PLUGIN);
        addContextProject(projects[2], TYPE_ECLIPSE_FEATURE);
        addContextProject(projects[3], TYPE_ECLIPSE_FEATURE);

        File siteProject = projects[4];
        addReactorProject(siteProject, TYPE_ECLIPSE_UPDATE_SITE, "site");

        P2ResolutionResult result = impl.collectProjectDependencies(context, siteProject);

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
        File projectLocation = resourceFile("duplicate-iu/featureA");

        addReactorProject(projectLocation, TYPE_ECLIPSE_FEATURE, "featureA");
        addReactorProject(resourceFile("duplicate-iu/featureA2"), TYPE_ECLIPSE_FEATURE, "featureA2");

        try {
            impl.resolveProject(context, projectLocation);
            fail();
        } catch (DuplicateReactorIUsException e) {
            // TODO proper assertion
        }
    }

    @Test
    public void featureInstallableUnits() throws Exception {
        File feature = resourceFile("resolver/feature01");
        String artifactId = "org.eclipse.tycho.p2.impl.resolver.test.feature01";
        addReactorProject(feature, TYPE_ECLIPSE_FEATURE, artifactId);

        List<P2ResolutionResult> results = impl.resolveProject(context, feature);

        Assert.assertEquals(1, results.size());
        P2ResolutionResult result = results.get(0);

        Assert.assertEquals(1, result.getArtifacts().size());
        Assert.assertEquals(1, result.getArtifacts().iterator().next().getInstallableUnits().size());
        Assert.assertEquals(0, result.getNonReactorUnits().size());
    }

    @Test
    public void sourceBundle() throws Exception {
        File feature = resourceFile("sourcebundles/feature01");
        String featureId = "org.eclipse.tycho.p2.impl.resolver.test.feature01";
        addReactorProject(feature, TYPE_ECLIPSE_FEATURE, featureId);

        File bundle = resourceFile("sourcebundles/bundle01");
        String bundleId = "org.eclipse.tycho.p2.impl.resolver.test.bundle01";
        String bundleVersion = "1.0.0-SNAPSHOT";
        addReactorProject(bundle, TYPE_ECLIPSE_PLUGIN, bundleId);

        ArtifactMock sb = new ArtifactMock(bundle, bundleId, bundleId, bundleVersion, TYPE_ECLIPSE_PLUGIN, "sources");
        sb.setDependencyMetadata(new SourcesBundleDependencyMetadataGenerator().generateMetadata(sb, getEnvironments(),
                null));
        context.addReactorArtifact(sb);

        List<P2ResolutionResult> results = impl.resolveProject(context, feature);

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
        context.addP2Repository(resourceFile("repositories/e342").toURI());
        // launchers currently cannot be disabled (see TYCHO-511/TYCHO-512)
        context.addP2Repository(resourceFile("repositories/launchers").toURI());

        File projectDir = resourceFile("resolver/repository");
        String artifactId = "org.eclipse.tycho.p2.impl.resolver.test.repository";
        addReactorProject(projectDir, TYPE_ECLIPSE_REPOSITORY, artifactId);

        addContextProject(resourceFile("resolver/bundle01"), TYPE_ECLIPSE_PLUGIN);

        List<P2ResolutionResult> results = impl.resolveProject(context, projectDir);

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
        context.addP2Repository(resourceFile("repositories/e361").toURI());

        File bundle = resourceFile("resolver/bundleUsesSWT");
        String artifactId = "org.eclipse.tycho.p2.impl.resolver.test.bundleUsesSWT";
        addReactorProject(bundle, TYPE_ECLIPSE_PLUGIN, artifactId);

        List<P2ResolutionResult> results = impl.resolveProject(context, bundle);

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
        context.addP2Repository(resourceFile("reactor-vs-external/extrepo").toURI());

        addReactorProject(resourceFile("reactor-vs-external/bundle01"), TYPE_ECLIPSE_PLUGIN,
                "org.sonatype.tycho.p2.impl.resolver.test.bundle01");

        File featureProject = resourceFile("reactor-vs-external/feature01");
        addReactorProject(featureProject, TYPE_ECLIPSE_FEATURE, "org.sonatype.tycho.p2.impl.resolver.test.feature01");

        List<P2ResolutionResult> results = impl.resolveProject(context, featureProject);

        Assert.assertEquals(1, results.size());
        P2ResolutionResult result = results.get(0);

        Assert.assertEquals(2, result.getArtifacts().size());
        Assert.assertEquals(0, result.getNonReactorUnits().size());

        for (Entry entry : result.getArtifacts()) {
            Assert.assertEquals("1.0.0.qualifier", entry.getVersion());
        }
    }

    @Test
    public void resolutionRestrictedEE() throws Exception {
        P2ResolverFactoryImpl p2ResolverFactory = createP2ResolverFactory(false);
        context = p2ResolverFactory.createResolutionContext("CDC-1.0/Foundation-1.0", false);

        context.addP2Repository(resourceFile("repositories/javax.xml").toURI());

        File bundle = resourceFile("resolver/bundle.bree");
        String artifactId = "bundle.bree";
        addReactorProject(bundle, TYPE_ECLIPSE_PLUGIN, artifactId);

        List<P2ResolutionResult> results = impl.resolveProject(context, bundle);

        Assert.assertEquals(1, results.size());
        P2ResolutionResult result = results.get(0);

        Assert.assertEquals(2, result.getArtifacts().size());

        Assert.assertEquals(1, result.getNonReactorUnits().size());
        assertContainsUnit("javax.xml", result.getNonReactorUnits());
    }

    @Test
    public void resolutionEE() throws Exception {
        P2ResolverFactoryImpl p2ResolverFactory = createP2ResolverFactory(false);
        context = p2ResolverFactory.createResolutionContext("J2SE-1.5", false);

        context.addP2Repository(resourceFile("repositories/javax.xml").toURI());

        File bundle = resourceFile("resolver/bundle.bree");
        String artifactId = "bundle.bree";
        addReactorProject(bundle, TYPE_ECLIPSE_PLUGIN, artifactId);

        List<P2ResolutionResult> results = impl.resolveProject(context, bundle);

        Assert.assertEquals(1, results.size());
        P2ResolutionResult result = results.get(0);

        Assert.assertEquals(1, result.getArtifacts().size());

        Assert.assertEquals(0, result.getNonReactorUnits().size());
    }

    @Test
    public void resolutionNoEE() throws Exception {
        context.addP2Repository(resourceFile("repositories/javax.xml").toURI());

        File bundle = resourceFile("resolver/bundle.nobree");
        String artifactId = "bundle.nobree";
        addReactorProject(bundle, TYPE_ECLIPSE_PLUGIN, artifactId);

        List<P2ResolutionResult> results = impl.resolveProject(context, bundle);

        Assert.assertEquals(1, results.size());
        P2ResolutionResult result = results.get(0);

        Assert.assertEquals(1, result.getArtifacts().size());

        Assert.assertEquals(0, result.getNonReactorUnits().size());
    }
}
