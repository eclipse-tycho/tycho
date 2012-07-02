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
    public void testDirectorApplicationArguments() {
        subject.addMetadataSources(Arrays.asList(REPO_1, REPO_2));
        subject.addArtifactSources(Arrays.asList(REPO_3));
        subject.addUnitToInstall(UNIT_1);
        subject.addUnitToInstall(UNIT_2);
        subject.setEnvironment(new TargetEnvironment(OS, WS, ARCH));
        subject.setProfileName(PROFILE_NAME);
        subject.setInstallFeatures(true);

        File dest = new File(".").getAbsoluteFile();
        subject.setDestination(dest);

        List<String> result = Arrays.asList(subject.getDirectorApplicationArguments());

        assertThat(result, hasSequence("-metadataRepository", REPO_1 + "," + REPO_2));
        assertThat(result, hasSequence("-artifactRepository", REPO_3.toString()));
        assertThat(result, hasSequence("-installIU", UNIT_1 + "," + UNIT_2));
        assertThat(result, hasSequence("-profile", PROFILE_NAME));
        assertThat(result, hasSequence("-profileProperties", "org.eclipse.update.install.features=true"));

        assertThat(result, hasSequence("-p2.os", OS));
        assertThat(result, hasSequence("-p2.ws", WS));
        assertThat(result, hasSequence("-p2.arch", ARCH));

        assertThat(result, hasSequence("-destination", dest.toString()));

        // unconditional arguments;
        assertThat(result, hasItem("-roaming"));
    }

    private static class AbstractDirectorApplicationCommandForTesting extends AbstractDirectorApplicationCommand {

        @Override
        public String[] getDirectorApplicationArguments() {
            return super.getDirectorApplicationArguments();
        }

        public void execute() throws DirectorCommandException {
            // not needed
        }
    }
}
