/*******************************************************************************
 * Copyright (c) 2012 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.tools.director;

import static org.eclipse.tycho.test.util.TychoMatchers.hasSequence;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.hasItem;

import java.io.File;
import java.net.URI;
import java.util.Arrays;
import java.util.List;

import org.eclipse.tycho.core.facade.TargetEnvironment;
import org.eclipse.tycho.p2.tools.director.shared.AbstractDirectorApplicationCommand;
import org.eclipse.tycho.p2.tools.director.shared.DirectorCommandException;
import org.junit.Before;
import org.junit.Test;

public class DirectorApplicationCommandTest {

    private static final String PARAM_OS = "-p2.os";
    private static final String PARAM_WS = "-p2.ws";
    private static final String PARAM_ARCH = "-p2.arch";

    private static final URI REPO_1 = URI.create("http://meta1/");
    private static final URI REPO_2 = URI.create("file:/meta2/");
    private static final URI REPO_3 = URI.create("http://example.org/artifactrepo");
    private static final String UNIT_1 = "product.uid";
    private static final String UNIT_2 = "org.example.feature.feature.group";
    private static final String PROFILE_NAME = "SDKProfile";
    private static final String OS = "some.os";
    private static final String WS = "some.ws";
    private static final String ARCH = "some.arch";

    private AbstractDirectorApplicationCommandForTesting subject;

    @Before
    public void setUp() throws Exception {
        subject = new AbstractDirectorApplicationCommandForTesting();
    }

    @Test
    public void testAllDirectorApplicationArguments() {
        subject.addMetadataSources(Arrays.asList(REPO_1, REPO_2));
        subject.addArtifactSources(Arrays.asList(REPO_3));
        subject.addUnitToInstall(UNIT_1);
        subject.addUnitToInstall(UNIT_2);
        subject.setEnvironment(new TargetEnvironment(OS, WS, ARCH));
        subject.setProfileName(PROFILE_NAME);
        subject.setInstallFeatures(true);

        File dest = new File(".").getAbsoluteFile();
        subject.setDestination(dest);

        List<String> result = subject.getDirectorApplicationArguments();

        assertThat(result, hasSequence("-metadataRepository", REPO_1 + "," + REPO_2));
        assertThat(result, hasSequence("-artifactRepository", REPO_3.toString()));
        assertThat(result, hasSequence("-installIU", UNIT_1 + "," + UNIT_2));
        assertThat(result, hasSequence("-profile", PROFILE_NAME));
        assertThat(result, hasSequence("-profileProperties", "org.eclipse.update.install.features=true"));

        assertThat(result, hasSequence(PARAM_OS, OS));
        assertThat(result, hasSequence(PARAM_WS, WS));
        assertThat(result, hasSequence(PARAM_ARCH, ARCH));

        assertThat(result, hasSequence("-destination", dest.toString()));

        assertThat(result, hasItem("-roaming"));
    }

    @Test
    public void testNoOsWsArchArguments() {
        addRequiredArguments();

        List<String> result = subject.getDirectorApplicationArguments();

        assertThat(result, not(hasItem(PARAM_OS)));
        assertThat(result, not(hasItem(PARAM_WS)));
        assertThat(result, not(hasItem(PARAM_ARCH)));
    }

    private void addRequiredArguments() {
        subject.setDestination(new File("."));
    }

    private static class AbstractDirectorApplicationCommandForTesting extends AbstractDirectorApplicationCommand {

        // increase method visibility
        @Override
        public List<String> getDirectorApplicationArguments() {
            return super.getDirectorApplicationArguments();
        }

        public void execute() throws DirectorCommandException {
            // not needed
        }
    }
}
