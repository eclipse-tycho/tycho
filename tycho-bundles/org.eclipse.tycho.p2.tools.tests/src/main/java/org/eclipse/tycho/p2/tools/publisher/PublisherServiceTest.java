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
package org.eclipse.tycho.p2.tools.publisher;

import static org.eclipse.tycho.p2.tools.test.util.ResourceUtil.resolveTestResource;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.matchers.JUnitMatchers.hasItem;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.eclipse.tycho.core.facade.MavenContext;
import org.eclipse.tycho.core.facade.MavenContextImpl;
import org.eclipse.tycho.p2.tools.BuildContext;
import org.eclipse.tycho.p2.tools.RepositoryReferences;
import org.eclipse.tycho.p2.tools.TargetEnvironment;
import org.eclipse.tycho.p2.tools.impl.Activator;
import org.eclipse.tycho.p2.tools.mirroring.MirrorApplicationServiceTest;
import org.eclipse.tycho.p2.tools.publisher.facade.PublisherService;
import org.eclipse.tycho.p2.tools.publisher.facade.PublisherServiceFactory;
import org.eclipse.tycho.test.util.MemoryLog;
import org.eclipse.tycho.test.util.ProjectIdStub;
import org.eclipse.tycho.test.util.StubServiceRegistration;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.osgi.util.tracker.ServiceTracker;

public class PublisherServiceTest {

    private static final String DEFAULT_QUALIFIER = "testqual";
    private static final String DEFAULT_FLAVOR = "tooling";
    private static final List<TargetEnvironment> DEFAULT_ENVIRONMENTS = Collections
            .singletonList(new TargetEnvironment("testws", "testos", "testarch"));

    private ServiceTracker<PublisherServiceFactory, PublisherServiceFactory> tracker;
    private PublisherServiceFactory publisherServiceFactory;

    @Rule
    public StubServiceRegistration<MavenContext> mavenContextRegistration = new StubServiceRegistration<MavenContext>(
            MavenContext.class, createMavenContext());

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private PublisherService subject;

    private File outputDirectory;
    private BuildContext buildContext;
    private RepositoryReferences contextRepositories;

    @Before
    public void initSubject() throws Exception {
        tracker = new ServiceTracker<PublisherServiceFactory, PublisherServiceFactory>(Activator.getContext(),
                PublisherServiceFactory.class, null);
        tracker.open();
        publisherServiceFactory = tracker.waitForService(2000);
        if (publisherServiceFactory == null)
            throw new IllegalStateException("PublisherServiceFactory did not show up");

        outputDirectory = tempFolder.newFolder("targetFolder");
        buildContext = new BuildContext(new ProjectIdStub(outputDirectory), DEFAULT_QUALIFIER,
                DEFAULT_ENVIRONMENTS);

        // TODO use a "normal" feature (we don't need the patch here...)
        contextRepositories = MirrorApplicationServiceTest.sourceRepos("patch");

        subject = publisherServiceFactory.createPublisher(contextRepositories, buildContext);
    }

    @After
    public void closeTracker() {
        // TODO this could be done by a rule
        tracker.close();
    }

    @Test
    public void testCategoryPublishing() throws Exception {
        File categoryDefinition = resolveTestResource("resources/publishers/category.xml");

        Collection<?> publishCategories = subject.publishCategories(categoryDefinition);

        assertEquals(1, publishCategories.size());

        List<String> outputDirectoryFiles = Arrays.asList(outputDirectory.list());
        assertThat(outputDirectoryFiles, hasItem("p2content.xml"));
        // also expect artifact repository (although it is empty)
        assertThat(outputDirectoryFiles, hasItem("p2artifacts.xml"));
        assertThat(outputDirectoryFiles, hasItem("local-artifacts.properties"));

        // TODO assert published unit

//        openFolderAndSleep(outputDirectory);
    }

    @Test
    public void testProductPublishing() throws Exception {
        File productDefinition = resolveTestResource("resources/publishers/test.product");
        File launcherBinaries = resolveTestResource("resources/launchers/");

        Collection<?> publishProduct = subject.publishProduct(productDefinition, launcherBinaries, DEFAULT_FLAVOR);

        assertTrue(publishProduct.size() > 0);

        List<String> outputDirectoryFiles = Arrays.asList(outputDirectory.list());
        assertThat(outputDirectoryFiles, hasItem("p2content.xml"));
        assertThat(outputDirectoryFiles, hasItem("p2artifacts.xml"));
        assertThat(outputDirectoryFiles, hasItem("local-artifacts.properties"));

        // TODO assert published unit
        // TODO assert binary artifact

//        openFolderAndSleep(outputDirectory);
    }

    private static MavenContextImpl createMavenContext() {
        MavenContextImpl mavenContext = new MavenContextImpl();
        mavenContext.setLogger(new MemoryLog(true));
        return mavenContext;
    }

    private static void openFolderAndSleep(File folder) throws IOException {
        Runtime.getRuntime().exec(new String[] { "explorer.exe", "/select," + folder.getAbsolutePath() });
        System.out.println("Press ENTER to continue...");
        new BufferedReader(new InputStreamReader(System.in)).readLine();
    }
}
