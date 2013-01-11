/*******************************************************************************
 * Copyright (c) 2008, 2012 Sonatype Inc. and others.
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

import static org.eclipse.tycho.ArtifactKey.TYPE_ECLIPSE_FEATURE;
import static org.eclipse.tycho.ArtifactKey.TYPE_ECLIPSE_PLUGIN;
import static org.eclipse.tycho.ArtifactKey.TYPE_ECLIPSE_REPOSITORY;
import static org.eclipse.tycho.ArtifactKey.TYPE_ECLIPSE_UPDATE_SITE;
import static org.eclipse.tycho.p2.impl.test.ResourceUtil.resourceFile;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
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
import org.eclipse.tycho.core.facade.TargetEnvironment;
import org.eclipse.tycho.p2.impl.publisher.DependencyMetadata;
import org.eclipse.tycho.p2.impl.publisher.SourcesBundleDependencyMetadataGenerator;
import org.eclipse.tycho.p2.impl.test.ArtifactMock;
import org.eclipse.tycho.p2.impl.test.MavenLoggerStub;
import org.eclipse.tycho.p2.resolver.facade.P2ResolutionResult;
import org.eclipse.tycho.p2.resolver.facade.P2ResolutionResult.Entry;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class P2ResolverTest extends P2ResolverTestBase {

    @Before
    public void initDefaultResolver() throws Exception {
        org.eclipse.equinox.internal.p2.core.helpers.Tracing.DEBUG_PLANNER_PROJECTOR = true;
        MavenLogger logger = new MavenLoggerStub();
        context = createTargetPlatformBuilder();
        impl = new P2ResolverImpl(logger);
        impl.setEnvironments(getEnvironments());
    }

    @Test
    public void basic() throws Exception {
        context.addP2Repository(resourceFile("repositories/e342").toURI());

        File bundle = resourceFile("resolver/bundle01");
        String artifactId = "org.eclipse.tycho.p2.impl.resolver.test.bundle01";
        addReactorProject(bundle, TYPE_ECLIPSE_PLUGIN, artifactId);

        List<P2ResolutionResult> results = impl.resolveDependencies(context.buildTargetPlatform(), bundle);

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

        P2ResolutionResult result = impl.collectProjectDependencies(context.buildTargetPlatform(), siteProject);

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
            impl.resolveDependencies(context.buildTargetPlatform(), projectLocation);
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

        List<P2ResolutionResult> results = impl.resolveDependencies(context.buildTargetPlatform(), feature);

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
        DependencyMetadata metadata = new SourcesBundleDependencyMetadataGenerator().generateMetadata(sb,
                getEnvironments(), null);
        sb.setDependencyMetadata(metadata);
        context.addReactorArtifact(sb);

        List<P2ResolutionResult> results = impl.resolveDependencies(context.buildTargetPlatform(), feature);

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
        context.addP2Repository(resourceFile("repositories/launchers").toURI());

        File projectDir = resourceFile("resolver/repository");
        String artifactId = "org.eclipse.tycho.p2.impl.resolver.test.repository";
        addReactorProject(projectDir, TYPE_ECLIPSE_REPOSITORY, artifactId);

        addContextProject(resourceFile("resolver/bundle01"), TYPE_ECLIPSE_PLUGIN);

        List<P2ResolutionResult> results = impl.resolveDependencies(context.buildTargetPlatform(), projectDir);

        Assert.assertEquals(1, results.size());
        P2ResolutionResult result = results.get(0);

        Assert.assertEquals(3, result.getArtifacts().size()); // the product, bundle01, and the one dependency of bundle01
        Assert.assertEquals(2, result.getNonReactorUnits().size());

        assertContainsUnit("org.eclipse.osgi", result.getNonReactorUnits());
        assertContainsUnit("org.eclipse.equinox.executable.feature.group", result.getNonReactorUnits());
    }

    @Test
    public void bundleUsesSWT() throws Exception {
        context.addP2Repository(resourceFile("repositories/e361").toURI());

        File bundle = resourceFile("resolver/bundleUsesSWT");
        String artifactId = "org.eclipse.tycho.p2.impl.resolver.test.bundleUsesSWT";
        addReactorProject(bundle, TYPE_ECLIPSE_PLUGIN, artifactId);

        List<P2ResolutionResult> results = impl.resolveDependencies(context.buildTargetPlatform(), bundle);

        Assert.assertEquals(1, results.size());
        P2ResolutionResult result = results.get(0);

        Assert.assertEquals(3, result.getArtifacts().size());
        Assert.assertEquals(2, result.getNonReactorUnits().size());

        assertContainsUnit("org.eclipse.swt", result.getNonReactorUnits());
        assertContainsUnit("org.eclipse.swt.gtk.linux.x86_64", result.getNonReactorUnits());
    }

    @Test
    public void swt() throws Exception {
        File swt = resourceFile("resolver/swt/org.eclipse.swt");
        addReactorProject(swt, TYPE_ECLIPSE_PLUGIN, "org.eclipse.swt");
        File swtFragment = resourceFile("resolver/swt/swtFragment");
        addReactorProject(swtFragment, TYPE_ECLIPSE_PLUGIN, "org.eclipse.tycho.p2.impl.resolver.test.swtFragment");

        List<P2ResolutionResult> results = impl.resolveDependencies(context.buildTargetPlatform(), swt);

        Assert.assertEquals(1, results.size());
        P2ResolutionResult result = results.get(0);

        Assert.assertEquals(1, result.getArtifacts().size());
        assertContainLocation(result, swt);
    }

    @Test
    public void swtFragment() throws Exception {
        File swt = resourceFile("resolver/swt/org.eclipse.swt");
        addReactorProject(swt, TYPE_ECLIPSE_PLUGIN, "org.eclipse.swt");
        File swtFragment = resourceFile("resolver/swt/swtFragment");
        addReactorProject(swtFragment, TYPE_ECLIPSE_PLUGIN, "org.eclipse.tycho.p2.impl.resolver.test.swtFragment");

        List<P2ResolutionResult> results = impl.resolveDependencies(context.buildTargetPlatform(), swtFragment);

        Assert.assertEquals(1, results.size());
        P2ResolutionResult result = results.get(0);

        Assert.assertEquals(2, result.getArtifacts().size());
        Assert.assertEquals(0, result.getNonReactorUnits().size());

        assertContainLocation(result, swtFragment);
        assertContainLocation(result, swt);
    }

    @Test
    public void swtFragmentWithRemoteSWT() throws Exception {
        context.addP2Repository(resourceFile("repositories/e361").toURI());

        File swtFragment = resourceFile("resolver/swt/swtFragment");
        addReactorProject(swtFragment, TYPE_ECLIPSE_PLUGIN, "org.eclipse.tycho.p2.impl.resolver.test.swtFragment");

        List<P2ResolutionResult> results = impl.resolveDependencies(context.buildTargetPlatform(), swtFragment);

        Assert.assertEquals(1, results.size());
        P2ResolutionResult result = results.get(0);

        Assert.assertEquals(2, result.getArtifacts().size());
        Assert.assertEquals(1, result.getNonReactorUnits().size());

        assertContainLocation(result, swtFragment);
        assertContainsUnit("org.eclipse.swt", result.getNonReactorUnits());
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

        List<P2ResolutionResult> results = impl.resolveDependencies(context.buildTargetPlatform(), featureProject);

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
        context = createTargetPlatformBuilderWithEE("CDC-1.0/Foundation-1.0");

        context.addP2Repository(resourceFile("repositories/javax.xml").toURI());

        File bundle = resourceFile("resolver/bundle.bree");
        String artifactId = "bundle.bree";
        addReactorProject(bundle, TYPE_ECLIPSE_PLUGIN, artifactId);

        List<P2ResolutionResult> results = impl.resolveDependencies(context.buildTargetPlatform(), bundle);

        Assert.assertEquals(1, results.size());
        P2ResolutionResult result = results.get(0);

        Assert.assertEquals(2, result.getArtifacts().size());

        Assert.assertEquals(3, result.getNonReactorUnits().size());
        assertContainsUnit("javax.xml", result.getNonReactorUnits());
        assertContainsUnit("a.jre.cdc", result.getNonReactorUnits());
        assertContainsUnit("config.a.jre.cdc", result.getNonReactorUnits());
    }

    @Test
    public void resolutionEE() throws Exception {
        context = createTargetPlatformBuilderWithEE("J2SE-1.5");

        context.addP2Repository(resourceFile("repositories/javax.xml").toURI());

        File bundle = resourceFile("resolver/bundle.bree");
        String artifactId = "bundle.bree";
        addReactorProject(bundle, TYPE_ECLIPSE_PLUGIN, artifactId);

        List<P2ResolutionResult> results = impl.resolveDependencies(context.buildTargetPlatform(), bundle);

        Assert.assertEquals(1, results.size());
        P2ResolutionResult result = results.get(0);

        Assert.assertEquals(1, result.getArtifacts().size());

        Assert.assertEquals(2, result.getNonReactorUnits().size());
        assertContainsUnit("a.jre.j2se", result.getNonReactorUnits());
        assertContainsUnit("config.a.jre.j2se", result.getNonReactorUnits());
    }

    @Test
    public void resolutionCustomEE() throws Exception {
        context = createTargetPlatformBuilderWithCustomEE("Custom_Profile-2");

        // repository containing both a bundle and the custom profile providing javax.activation;version="1.1.1"
        context.addP2Repository(resourceFile("repositories/custom-profile").toURI());

        // bundle importing javax.activation;version="1.1.1"
        File bundle = resourceFile("resolver/bundleRequiringVersionedJDKPackage");
        addReactorProject(bundle, TYPE_ECLIPSE_PLUGIN, "bundleRequiringVersionedJDKPackage");

        List<P2ResolutionResult> results = impl.resolveDependencies(context.buildTargetPlatform(), bundle);

        assertThat(results.size(), is(1));
        P2ResolutionResult result = results.get(0); // huh?

        assertThat(result.getNonReactorUnits().size(), is(1));
        assertContainsUnit("a.jre.custom.profile", result.getNonReactorUnits());
        // I don't know why we should expect a config.a.jre.custom.profile IU here
    }

    @Test
    public void featureMultienvP2Inf() throws Exception {
        List<TargetEnvironment> environments = new ArrayList<TargetEnvironment>();
        environments.add(new TargetEnvironment("linux", "gtk", "x86_64"));
        environments.add(new TargetEnvironment("macosx", "cocoa", "x86_64"));
        impl.setEnvironments(environments);

        File bundle = resourceFile("resolver/feature.multienv.p2-inf");
        String artifactId = "feature.multienv.p2-inf";
        addReactorProject(bundle, TYPE_ECLIPSE_FEATURE, artifactId);

        List<P2ResolutionResult> results = impl.resolveDependencies(context.buildTargetPlatform(), bundle);

        Assert.assertEquals(2, results.size());

        P2ResolutionResult linux = results.get(0);
        List<Entry> linuxEntries = new ArrayList<Entry>(linux.getArtifacts());
        Assert.assertEquals(1, linuxEntries.size());
        Assert.assertEquals(1, linuxEntries.get(0).getInstallableUnits().size());
        Assert.assertEquals(0, linux.getNonReactorUnits().size());

        P2ResolutionResult macosx = results.get(1);
        List<Entry> macosxEntries = new ArrayList<Entry>(macosx.getArtifacts());
        Assert.assertEquals(1, macosxEntries.size());
        Assert.assertEquals(2, macosxEntries.get(0).getInstallableUnits().size());
        Assert.assertEquals(0, macosx.getNonReactorUnits().size());
    }

    @Test
    public void productMultienvP2Inf() throws Exception {
        List<TargetEnvironment> environments = new ArrayList<TargetEnvironment>();
        environments.add(new TargetEnvironment("linux", "gtk", "x86_64"));
        environments.add(new TargetEnvironment("macosx", "cocoa", "x86_64"));
        impl.setEnvironments(environments);

        File bundle = resourceFile("resolver/product.multienv.p2-inf");
        String artifactId = "product.multienv.p2-inf";
        addReactorProject(bundle, TYPE_ECLIPSE_REPOSITORY, artifactId);

        List<P2ResolutionResult> results = impl.resolveDependencies(context.buildTargetPlatform(), bundle);

        Assert.assertEquals(2, results.size());

        P2ResolutionResult linux = results.get(0);
        List<Entry> linuxEntries = new ArrayList<Entry>(linux.getArtifacts());
        Assert.assertEquals(1, linuxEntries.size());
        Assert.assertEquals(1, linuxEntries.get(0).getInstallableUnits().size());
        Assert.assertEquals(0, linux.getNonReactorUnits().size());

        P2ResolutionResult macosx = results.get(1);
        List<Entry> macosxEntries = new ArrayList<Entry>(macosx.getArtifacts());
        Assert.assertEquals(1, macosxEntries.size());
        Assert.assertEquals(2, macosxEntries.get(0).getInstallableUnits().size());
        Assert.assertEquals(0, macosx.getNonReactorUnits().size());
    }
}
