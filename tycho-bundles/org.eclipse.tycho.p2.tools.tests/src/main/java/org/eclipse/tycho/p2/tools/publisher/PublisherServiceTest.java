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
import static org.eclipse.tycho.test.util.TychoMatchers.endsWithString;
import static org.eclipse.tycho.test.util.TychoMatchers.isFile;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.hasItem;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.tycho.core.facade.MavenContext;
import org.eclipse.tycho.core.facade.MavenContextImpl;
import org.eclipse.tycho.core.facade.MavenLogger;
import org.eclipse.tycho.core.facade.TargetEnvironment;
import org.eclipse.tycho.p2.tools.BuildContext;
import org.eclipse.tycho.p2.tools.RepositoryReferences;
import org.eclipse.tycho.p2.tools.mirroring.MirrorApplicationServiceTest;
import org.eclipse.tycho.p2.tools.publisher.facade.PublisherService;
import org.eclipse.tycho.repository.module.PublishingRepositoryImpl;
import org.eclipse.tycho.repository.publishing.PublishingRepository;
import org.eclipse.tycho.test.util.MemoryLog;
import org.eclipse.tycho.test.util.P2Context;
import org.eclipse.tycho.test.util.ReactorProjectCoordinatesStub;
import org.eclipse.tycho.test.util.StubServiceRegistration;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class PublisherServiceTest {

    private static final String DEFAULT_QUALIFIER = "testqual";
    private static final String DEFAULT_FLAVOR = "tooling";
    private static final List<TargetEnvironment> DEFAULT_ENVIRONMENTS = Collections
            .singletonList(new TargetEnvironment("testos", "testws", "testarch"));

    private MemoryLog mavenLogger = new MemoryLog(true);

    @Rule
    public TemporaryFolder tempManager = new TemporaryFolder();
    @Rule
    public P2Context p2Context = new P2Context();

    @Rule
    public StubServiceRegistration<MavenContext> mavenContextRegistration = new StubServiceRegistration<MavenContext>(
            MavenContext.class, createMavenContext(mavenLogger));

    private PublisherService subject;

    private File outputDirectory;
    private PublishingRepository outputRepository;

    @Before
    public void initSubject() throws Exception {
        outputDirectory = tempManager.newFolder("targetDir");

        BuildContext buildContext = new BuildContext(new ReactorProjectCoordinatesStub(outputDirectory),
                DEFAULT_QUALIFIER, DEFAULT_ENVIRONMENTS);

        // TODO use a "normal" feature (we don't need the patch here...)
        RepositoryReferences contextRepositories = MirrorApplicationServiceTest.sourceRepos("patch");

        outputRepository = new PublishingRepositoryImpl(p2Context.getAgent(), new ReactorProjectCoordinatesStub(
                outputDirectory));
        PublisherInfoTemplate publisherConfiguration = new PublisherInfoTemplate(contextRepositories, buildContext,
                p2Context.getAgent());
        subject = new PublisherServiceImpl(buildContext, publisherConfiguration, outputRepository, mavenLogger);
    }

    @Test
    public void testCategoryPublishing() throws Exception {
        File categoryDefinition = resolveTestResource("resources/publishers/category.xml");

        Collection<?> rootUnits = subject.publishCategories(categoryDefinition);

        assertThat(rootUnits.size(), is(1));
        Object rootUnit = rootUnits.iterator().next();

        Set<Object> publishedUnits = outputRepository.getInstallableUnits();
        assertThat(publishedUnits, hasItem(rootUnit));

//        openFolderAndSleep(outputDirectory);
    }

    @Test
    public void testProductPublishing() throws Exception {
        File productDefinition = resolveTestResource("resources/publishers/test.product");
        File launcherBinaries = resolveTestResource("resources/launchers/");

        Collection<?> rootUnits = subject.publishProduct(productDefinition, launcherBinaries, DEFAULT_FLAVOR);

        assertThat(rootUnits.size(), is(1));
        Object rootUnit = rootUnits.iterator().next();

        Set<Object> publishedUnits = outputRepository.getInstallableUnits();
        assertThat(publishedUnits, hasItem(rootUnit));

        // test for launcher artifact
        Map<String, File> artifactLocations = outputRepository.getArtifactLocations();
        // TODO 348586 drop productUid from classifier
        String executableClassifier = "productUid.executable.testws.testos.testarch";
        assertThat(artifactLocations.keySet(), hasItem(executableClassifier));
        assertThat(artifactLocations.get(executableClassifier), isFile());
        assertThat(artifactLocations.get(executableClassifier).toString(), endsWithString(".zip"));

//        openFolderAndSleep(outputDirectory);
    }

    private static MavenContext createMavenContext(MavenLogger mavenLogger) {
        MavenContext mavenContext = new MavenContextImpl(null, false, mavenLogger, null);
        return mavenContext;
    }

    private static void openFolderAndSleep(File folder) throws IOException {
        Runtime.getRuntime().exec(new String[] { "explorer.exe", "/select," + folder.getAbsolutePath() });
        System.out.println("Press ENTER to continue...");
        new BufferedReader(new InputStreamReader(System.in)).readLine();
    }

}
