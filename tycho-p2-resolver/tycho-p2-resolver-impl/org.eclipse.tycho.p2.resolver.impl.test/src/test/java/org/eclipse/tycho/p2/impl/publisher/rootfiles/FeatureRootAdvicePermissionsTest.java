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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;

import org.eclipse.equinox.p2.publisher.actions.IFeatureRootAdvice;
import org.junit.Ignore;
import org.junit.Test;

@SuppressWarnings("restriction")
public class FeatureRootAdvicePermissionsTest {

    @Test
    public void testNoPermisions() {
        Properties buildProperties = createBuildPropertiesWithDefaultRootFiles();
        List<String[]> actualPermissions = createAdviceAndGetPermissions(buildProperties, GLOBAL_SPEC);
        assertEquals(0, actualPermissions.size());
    }

    @Test
    public void testGlobalPermissions() {
        Properties buildProperties = createBuildPropertiesWithDefaultRootFiles();
        buildProperties.put("root.permissions.755", "file1.txt,dir/file3.txt");
        buildProperties.put("root.permissions.555", "file2.txt");

        List<String[]> actualPermissions = createAdviceAndGetPermissions(buildProperties, GLOBAL_SPEC);

        assertEquals(3, actualPermissions.size());
        assertPermissionEntry("dir/file3.txt", "755", actualPermissions.get(0));
        assertPermissionEntry("file1.txt", "755", actualPermissions.get(1));
        assertPermissionEntry("file2.txt", "555", actualPermissions.get(2));
    }

    @Test
    public void testSpecificPermissions() {
        Properties buildProperties = createBuildPropertiesWithDefaultRootFiles();
        buildProperties.put("root.permissions.755", "file1.txt");
        buildProperties.put("root." + LINUX_SPEC_FOR_PROPERTIES_KEY, "file:rootfiles/file2.txt");
        buildProperties.put("root." + LINUX_SPEC_FOR_PROPERTIES_KEY + ".permissions.755", "file2.txt");
        IFeatureRootAdvice advice = createAdvice(buildProperties);

        List<String[]> globalPermissions = getSortedPermissions(advice, GLOBAL_SPEC);
        assertEquals(1, globalPermissions.size());

        List<String[]> specificPermissions = getSortedPermissions(advice, LINUX_SPEC_FOR_ADVICE);
        assertEquals(1, specificPermissions.size());
    }

    @Test
    public void testWhitespaceAroundSeparatorsInPermissions() throws Exception {
        Properties buildProperties = createBuildPropertiesWithDefaultRootFiles();
        buildProperties.put("root.permissions.755", " file1.txt ,  dir/file3.txt, \n\tfile2.txt ");
        List<String[]> list = createAdviceAndGetPermissions(buildProperties, GLOBAL_SPEC);

        assertEquals(3, list.size());
        assertPermissionEntry("dir/file3.txt", "755", list.get(0));
        assertPermissionEntry("file1.txt", "755", list.get(1));
        assertPermissionEntry("file2.txt", "755", list.get(2));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGlobalPermissionsChmodMissing() throws Exception {
        Properties buildProperties = createBuildPropertiesWithDefaultRootFiles();
        buildProperties.put("root.permissions", "file1.txt");
        createAdvice(buildProperties).getDescriptor(GLOBAL_SPEC);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSpecificPermissionsChmodMissing() throws Exception {
        Properties buildProperties = createBuildPropertiesWithDefaultRootFiles();
        buildProperties.put("root." + WINDOWS_SPEC_FOR_PROPERTIES_KEY + ".permissions", "rootfiles/file1.txt");
        createAdvice(buildProperties).getDescriptor(GLOBAL_SPEC);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGlobalPermissionsButNoFiles() throws Exception {
        Properties buildProperties = createBuildPropertiesWithoutRootKeys();
        buildProperties.put("root.permissions.644", "file2.txt");

        IFeatureRootAdvice advice = createAdvice(buildProperties);
        callGetDescriptorsForAllConfigurations(advice);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSpecificPermissionsButNoFiles() throws Exception {
        Properties buildProperties = createBuildPropertiesWithDefaultRootFiles();
        buildProperties.put("root." + LINUX_SPEC_FOR_PROPERTIES_KEY + ".permissions.644", "file2.txt");

        IFeatureRootAdvice advice = createAdvice(buildProperties);
        callGetDescriptorsForAllConfigurations(advice);
    }

    @Ignore
    @Test(expected = IllegalArgumentException.class)
    public void testPermissionsChmodInvalidValue() throws Exception {
        Properties buildProperties = createBuildPropertiesWithDefaultRootFiles();
        buildProperties.put("root.permissions.og-rwx", "file1.txt");
        createAdvice(buildProperties).getDescriptor(GLOBAL_SPEC);
    }

    private static List<String[]> createAdviceAndGetPermissions(Properties buildProperties, String configSpec) {
        IFeatureRootAdvice advice = createAdvice(buildProperties);
        return getSortedPermissions(advice, configSpec);
    }

    private static List<String[]> getSortedPermissions(IFeatureRootAdvice advice, String configSpec) {
        String[][] permissionsArray = advice.getDescriptor(configSpec).getPermissions();
        ArrayList<String[]> permissionsList = new ArrayList<String[]>();
        permissionsList.addAll(Arrays.asList(permissionsArray));
        Collections.sort(permissionsList, new PermissionEntryComparator());
        return permissionsList;
    }

    private static void assertPermissionEntry(String expectedFile, String expectedChmod, String[] descriptorPermission) {
        assertEquals(expectedChmod, descriptorPermission[0]);
        assertEquals(expectedFile, descriptorPermission[1]);
    }

    private static class PermissionEntryComparator implements Comparator<String[]> {
        public int compare(String[] o1, String[] o2) {
            // compare files
            return o1[1].compareTo(o2[1]);
        }
    }
}
