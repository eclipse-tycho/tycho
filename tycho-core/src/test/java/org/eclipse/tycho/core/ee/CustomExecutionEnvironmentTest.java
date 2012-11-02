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
package org.eclipse.tycho.core.ee;

import static org.hamcrest.CoreMatchers.any;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.hasItem;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.tycho.core.ee.shared.SystemCapability;
import org.eclipse.tycho.core.ee.shared.SystemCapability.Type;
import org.junit.Test;
import org.osgi.framework.Constants;

@SuppressWarnings("deprecation")
public class CustomExecutionEnvironmentTest {

    private static final SystemCapability PACKAGE_JAVA_LANG = new SystemCapability(Type.JAVA_PACKAGE, "java.lang", null);
    private static final SystemCapability PACKAGE_JAVAX_ACTIVATION_1_1 = new SystemCapability(Type.JAVA_PACKAGE,
            "javax.activation", "1.1");
    private static final SystemCapability OSGI_JAVASE_1_6 = new SystemCapability(Type.OSGI_EE, "JavaSE", "1.6.0");
    private static final SystemCapability OSGI_JAVASE_1_7 = new SystemCapability(Type.OSGI_EE, "JavaSE", "1.7.0");
    private static final SystemCapability OSGI_OSGIMINIMUM_1_0 = new SystemCapability(Type.OSGI_EE, "OSGi/Minimum",
            "1.0.0");

    private CustomExecutionEnvironment customExecutionEnvironment;

    @Test
    public void testEmptyExecutionEnvironment() {
        createExecutionEnvironment();

        assertThat(customExecutionEnvironment.getProfileName(), is("name"));
        assertThat(customExecutionEnvironment.getCompilerSourceLevelDefault(), is(nullValue()));
        assertThat(customExecutionEnvironment.getCompilerTargetLevelDefault(), is(nullValue()));
        assertThat(customExecutionEnvironment.getSystemPackages(), not(hasItem(any(String.class))));
        assertProperty(org.eclipse.osgi.framework.internal.core.Constants.OSGI_JAVA_PROFILE_NAME, "name");
    }

    @Test
    public void testProvidedSystemPackageNoVersion() throws Exception {
        createExecutionEnvironment(PACKAGE_JAVA_LANG);

        assertThat(customExecutionEnvironment.getSystemPackages(), hasItem("java.lang"));
        assertThat(customExecutionEnvironment.getProfileProperties().size(), is(2));
        assertProperty(Constants.FRAMEWORK_SYSTEMPACKAGES, "java.lang");
    }

    @Test
    public void testProvidedSystemPackageWithVersion() throws Exception {
        createExecutionEnvironment(PACKAGE_JAVAX_ACTIVATION_1_1);

        assertThat(customExecutionEnvironment.getSystemPackages(), hasItem("javax.activation"));
        assertThat(customExecutionEnvironment.getProfileProperties().size(), is(2));
        assertProperty(Constants.FRAMEWORK_SYSTEMPACKAGES, "javax.activation;version=\"1.1\"");
    }

    @Test
    public void testTwoProvidedSystemPackages() throws Exception {
        createExecutionEnvironment(PACKAGE_JAVA_LANG, PACKAGE_JAVAX_ACTIVATION_1_1);

        assertThat(customExecutionEnvironment.getSystemPackages(), hasItem("java.lang"));
        assertThat(customExecutionEnvironment.getSystemPackages(), hasItem("javax.activation"));
        assertThat(customExecutionEnvironment.getProfileProperties().size(), is(2));
        assertProperty(Constants.FRAMEWORK_SYSTEMPACKAGES, "java.lang,javax.activation;version=\"1.1\"");
    }

    @Test
    public void testOsgiEeCapability() throws Exception {
        createExecutionEnvironment(OSGI_JAVASE_1_6);

        assertThat(customExecutionEnvironment.getSystemPackages(), not(hasItem(any(String.class))));
        assertThat(customExecutionEnvironment.getProfileProperties().size(), is(3));
        assertProperty(Constants.FRAMEWORK_SYSTEMCAPABILITIES, "osgi.ee; osgi.ee=\"JavaSE\"; version:Version=\"1.6\"");
        assertProperty(Constants.FRAMEWORK_EXECUTIONENVIRONMENT, "JavaSE-1.6");
    }

    @Test
    public void testTwoOsgiEeCapabilities() throws Exception {
        createExecutionEnvironment(OSGI_JAVASE_1_6, OSGI_OSGIMINIMUM_1_0);

        assertThat(customExecutionEnvironment.getProfileProperties().size(), is(3));
        assertProperty(Constants.FRAMEWORK_SYSTEMCAPABILITIES, "osgi.ee; osgi.ee=\"JavaSE\"; version:Version=\"1.6\","
                + "osgi.ee; osgi.ee=\"OSGi/Minimum\"; version:Version=\"1.0\"");
        assertProperty(Constants.FRAMEWORK_EXECUTIONENVIRONMENT, "JavaSE-1.6,OSGi/Minimum-1.0");
    }

    @Test
    public void testOsgiEeCapabilityInTwoVersions() throws Exception {
        createExecutionEnvironment(OSGI_JAVASE_1_6, OSGI_JAVASE_1_7);

        assertThat(customExecutionEnvironment.getProfileProperties().size(), is(3));
        assertProperty(Constants.FRAMEWORK_EXECUTIONENVIRONMENT, "JavaSE-1.6,JavaSE-1.7");
        assertProperty(Constants.FRAMEWORK_SYSTEMCAPABILITIES,
                "osgi.ee; osgi.ee=\"JavaSE\"; version:List<Version>=\"1.6, 1.7\"");
    }

    @Test
    public void testOsgiEeCapabilityStrangeVersion() throws Exception {
        createExecutionEnvironment(new SystemCapability(Type.OSGI_EE, "JavaSE", "1.6.1"));

        assertThat(customExecutionEnvironment.getProfileProperties().size(), is(3));
        assertProperty(Constants.FRAMEWORK_EXECUTIONENVIRONMENT, "JavaSE-1.6.1");
        assertProperty(Constants.FRAMEWORK_SYSTEMCAPABILITIES, "osgi.ee; osgi.ee=\"JavaSE\"; version:Version=\"1.6.1\"");
    }

    @Test
    public void testOsgiEeCapabilityAlphanumericVersion() throws Exception {
        createExecutionEnvironment(new SystemCapability(Type.OSGI_EE, "JavaSE", "AlphaRelease.beta-245.0"));

        assertThat(customExecutionEnvironment.getProfileProperties().size(), is(3));
        assertProperty(Constants.FRAMEWORK_EXECUTIONENVIRONMENT, "JavaSE-AlphaRelease.beta-245.0");
        assertProperty(Constants.FRAMEWORK_SYSTEMCAPABILITIES,
                "osgi.ee; osgi.ee=\"JavaSE\"; version:Version=\"AlphaRelease.beta-245.0\"");
    }

    @Test
    public void testJavaSEVersionNameMappings() throws Exception {
        List<SystemCapability> javaSeVersions = new ArrayList<SystemCapability>();
        for (String version : new String[] { "1.2.0", "1.3.0", "1.4.0", "1.5.0", "1.6.0", "1.7.0" }) {
            SystemCapability capability = new SystemCapability(Type.OSGI_EE, "JavaSE", version);
            javaSeVersions.add(capability);
        }
        customExecutionEnvironment = new CustomExecutionEnvironment("name", javaSeVersions);

        assertThat(customExecutionEnvironment.getProfileProperties().size(), is(3));
        assertProperty(Constants.FRAMEWORK_EXECUTIONENVIRONMENT,
                "J2SE-1.2,J2SE-1.3,J2SE-1.4,J2SE-1.5,JavaSE-1.6,JavaSE-1.7");
    }

    @Test
    public void testCDCNameMapping() throws Exception {
        createExecutionEnvironment(new SystemCapability(Type.OSGI_EE, "CDC/Foundation", "1.0"), new SystemCapability(
                Type.OSGI_EE, "CDC/Foundation", "1.1.0"));

        assertProperty(Constants.FRAMEWORK_EXECUTIONENVIRONMENT, "CDC-1.0/Foundation-1.0,CDC-1.1/Foundation-1.1");
        assertProperty(Constants.FRAMEWORK_SYSTEMCAPABILITIES,
                "osgi.ee; osgi.ee=\"CDC/Foundation\"; version:List<Version>=\"1.0, 1.1\"");
    }

    private void createExecutionEnvironment(SystemCapability... capabilities) {
        customExecutionEnvironment = new CustomExecutionEnvironment("name", Arrays.asList(capabilities));
    }

    private void assertProperty(String key, String value) {
        assertThat(customExecutionEnvironment.getProfileProperties().getProperty(key), is(value));
    }
}
