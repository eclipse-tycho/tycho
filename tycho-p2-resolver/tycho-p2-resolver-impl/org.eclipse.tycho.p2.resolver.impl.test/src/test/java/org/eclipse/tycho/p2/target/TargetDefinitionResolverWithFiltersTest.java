/*******************************************************************************
 * Copyright (c) 2011 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.target;

import static org.eclipse.tycho.p2.target.TargetDefinitionResolverTest.definitionWith;
import static org.eclipse.tycho.p2.target.TargetDefinitionResolverTest.versionedIdSet;
import static org.eclipse.tycho.p2.target.TargetDefinitionResolverTest.versionedIdsOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IVersionedId;
import org.eclipse.equinox.p2.metadata.VersionedId;
import org.eclipse.tycho.p2.impl.test.MavenLoggerStub;
import org.eclipse.tycho.p2.target.TargetDefinitionResolverTest.LocationStub;
import org.eclipse.tycho.p2.target.TargetDefinitionResolverTest.TestRepositories;
import org.eclipse.tycho.p2.target.facade.TargetDefinition;
import org.eclipse.tycho.test.util.P2Context;
import org.junit.Rule;
import org.junit.Test;

public class TargetDefinitionResolverWithFiltersTest {
    private static final IVersionedId LAUNCHER_FEATURE = new VersionedId(
            "org.eclipse.equinox.executable.feature.group", "3.3.101.R34x_v20081125-7H-ELfE8hXnkE15Wh9Tnyu");
    private static final IVersionedId LAUNCHER_BUNDLE = new VersionedId("org.eclipse.equinox.launcher",
            "1.0.101.R34x_v20081125");
    private static final IVersionedId LAUNCHER_BUNDLE_LINUX = new VersionedId(
            "org.eclipse.equinox.launcher.gtk.linux.x86_64", "1.0.101.R34x_v20080731");
    private static final IVersionedId LAUNCHER_BUNDLE_WINDOWS = new VersionedId(
            "org.eclipse.equinox.launcher.win32.win32.x86", "1.0.101.R34x_v20080731");
    private static final IVersionedId LAUNCHER_BUNDLE_MAC = new VersionedId(
            "org.eclipse.equinox.launcher.carbon.macosx", "1.0.101.R34x_v20080731");

    @Rule
    public P2Context p2Context = new P2Context();

    private MavenLoggerStub logger = new MavenLoggerStub();
    private TargetDefinitionResolver subject;

    @Test
    public void testResolutionWithoutPlatform() throws Exception {
        subject = new TargetDefinitionResolver(Collections.singletonList(newMap()), p2Context.getAgent(), logger);

        TargetDefinition definition = definitionWith(new LocationStub(TestRepositories.WITH_FILTERS, LAUNCHER_FEATURE));
        TargetPlatformContent content = subject.resolveContent(definition);
        Collection<? extends IInstallableUnit> units = content.getUnits();
        assertThat(versionedIdsOf(units), is(versionedIdSet(LAUNCHER_FEATURE, LAUNCHER_BUNDLE)));
    }

    @Test
    public void testResolutionWithOnePlatform() throws Exception {
        Map<String, String> environment = createEnvironment("gtk", "linux", "x86_64");
        subject = new TargetDefinitionResolver(Collections.singletonList(environment), p2Context.getAgent(), logger);

        TargetDefinition definition = definitionWith(new LocationStub(TestRepositories.WITH_FILTERS, LAUNCHER_FEATURE));
        TargetPlatformContent content = subject.resolveContent(definition);
        Collection<? extends IInstallableUnit> units = content.getUnits();
        assertThat(versionedIdsOf(units), is(versionedIdSet(LAUNCHER_FEATURE, LAUNCHER_BUNDLE, LAUNCHER_BUNDLE_LINUX)));
    }

    @Test
    public void testResolutionWithMultiplePlatforms() throws Exception {
        @SuppressWarnings("unchecked")
        List<Map<String, String>> environments = Arrays.asList(createEnvironment("gtk", "linux", "x86_64"),
                createEnvironment("win32", "win32", "x86"), createEnvironment("carbon", "macosx", "x86"));
        subject = new TargetDefinitionResolver(environments, p2Context.getAgent(), logger);

        TargetDefinition definition = definitionWith(new LocationStub(TestRepositories.WITH_FILTERS, LAUNCHER_FEATURE));
        TargetPlatformContent content = subject.resolveContent(definition);
        Collection<? extends IInstallableUnit> units = content.getUnits();
        assertThat(
                versionedIdsOf(units),
                is(versionedIdSet(LAUNCHER_FEATURE, LAUNCHER_BUNDLE, LAUNCHER_BUNDLE_LINUX, LAUNCHER_BUNDLE_WINDOWS,
                        LAUNCHER_BUNDLE_MAC)));
    }

    private Map<String, String> createEnvironment(String ws, String os, String arch) {
        Map<String, String> environment = newMap();
        environment.put("osgi.ws", ws);
        environment.put("osgi.os", os);
        environment.put("osgi.arch", arch);
        return environment;
    }

    private Map<String, String> newMap() {
        return new HashMap<String, String>();
    }
}
