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
package org.eclipse.tycho.p2.impl.publisher.rootfiles;

import static org.eclipse.tycho.p2.impl.publisher.rootfiles.FeatureRootAdviceTest.GLOBAL_SPEC;
import static org.eclipse.tycho.p2.impl.publisher.rootfiles.FeatureRootAdviceTest.LINUX_SPEC_FOR_ADVICE;
import static org.eclipse.tycho.p2.impl.publisher.rootfiles.FeatureRootAdviceTest.LINUX_SPEC_FOR_PROPERTIES_KEY;
import static org.eclipse.tycho.p2.impl.publisher.rootfiles.FeatureRootAdviceTest.WINDOWS_SPEC_FOR_PROPERTIES_KEY;
import static org.eclipse.tycho.p2.impl.publisher.rootfiles.FeatureRootAdviceTest.callGetDescriptorsForAllConfigurations;
import static org.eclipse.tycho.p2.impl.publisher.rootfiles.FeatureRootAdviceTest.createAdvice;
import static org.eclipse.tycho.p2.impl.publisher.rootfiles.FeatureRootAdviceTest.createBuildPropertiesWithDefaultRootFiles;
import static org.eclipse.tycho.p2.impl.publisher.rootfiles.FeatureRootAdviceTest.createBuildPropertiesWithoutRootKeys;
import static org.junit.Assert.assertEquals;

import java.util.Properties;

import org.eclipse.equinox.p2.publisher.actions.IFeatureRootAdvice;
import org.junit.Test;

@SuppressWarnings("restriction")
public class FeatureRootAdviceLinksTest {
    @Test
    public void testNoLinks() {
        Properties buildProperties = createBuildPropertiesWithDefaultRootFiles();

        IFeatureRootAdvice advice = createAdvice(buildProperties);

        String globalLinks = advice.getDescriptor(GLOBAL_SPEC).getLinks();
        assertEquals("", globalLinks);
    }

    @Test
    public void testGlobalLinks() {
        Properties buildProperties = createBuildPropertiesWithDefaultRootFiles();
        buildProperties.put("root.link", "dir/file3.txt,alias1.txt,file1.txt,alias2.txt");

        IFeatureRootAdvice advice = createAdvice(buildProperties);

        String actualLink = advice.getDescriptor(GLOBAL_SPEC).getLinks();
        assertEquals("dir/file3.txt,alias1.txt,file1.txt,alias2.txt", actualLink);
    }

    @Test
    public void testSpecificLinks() {
        Properties buildProperties = createBuildPropertiesWithDefaultRootFiles();

        buildProperties.put("root." + LINUX_SPEC_FOR_PROPERTIES_KEY, "file:rootfiles/dir/file3.txt");
        buildProperties.put("root." + LINUX_SPEC_FOR_PROPERTIES_KEY + ".link", "file3.txt,alias.txt");

        IFeatureRootAdvice advice = createAdvice(buildProperties);

        String globalLink = advice.getDescriptor(GLOBAL_SPEC).getLinks();
        assertEquals("", globalLink);

        String specificLink = advice.getDescriptor(LINUX_SPEC_FOR_ADVICE).getLinks();
        assertEquals("file3.txt,alias.txt", specificLink);
    }

    @Test
    public void testWhitespaceAroundSeparatorsInLinks() throws Exception {
        Properties buildProperties = createBuildPropertiesWithDefaultRootFiles();
        buildProperties.put("root.link",
                " file1.txt , alias1.txt ,  dir/file3.txt,alias2.txt , \n\tfile2.txt , alias3.txt \n\t");

        IFeatureRootAdvice advice = createAdvice(buildProperties);

        String actualLinks = advice.getDescriptor(GLOBAL_SPEC).getLinks();
        assertEquals("file1.txt,alias1.txt,dir/file3.txt,alias2.txt,file2.txt,alias3.txt", actualLinks);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWrongRootfilesLinksKey() throws Exception {
        Properties buildProperties = createBuildPropertiesWithDefaultRootFiles();

        buildProperties.put("root.link.addedTooMuch", "file1.txt,alias.txt");
        createAdvice(buildProperties);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGlobalLinkButNoFiles() throws Exception {
        Properties buildProperties = createBuildPropertiesWithoutRootKeys();
        buildProperties.put("root.link", "file1.txt,alias.txt");

        IFeatureRootAdvice advice = createAdvice(buildProperties);
        callGetDescriptorsForAllConfigurations(advice);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSpecificLinkButNoFiles() throws Exception {
        Properties buildProperties = createBuildPropertiesWithDefaultRootFiles();
        buildProperties.put("root." + WINDOWS_SPEC_FOR_PROPERTIES_KEY + ".link", "file1.txt,alias.txt");

        IFeatureRootAdvice advice = createAdvice(buildProperties);
        callGetDescriptorsForAllConfigurations(advice);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testLinkValueNotInPairs() throws Exception {
        Properties buildProperties = createBuildPropertiesWithDefaultRootFiles();
        buildProperties.put("root.link", "file1.txt,alias1.txt,file2.txt");

        IFeatureRootAdvice advice = createAdvice(buildProperties);
        callGetDescriptorsForAllConfigurations(advice);
    }

}
