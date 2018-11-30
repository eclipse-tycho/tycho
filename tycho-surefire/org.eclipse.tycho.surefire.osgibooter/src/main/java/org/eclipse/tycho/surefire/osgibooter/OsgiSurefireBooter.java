/*******************************************************************************
 * Copyright (c) 2008, 2018 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *    SAP AG        - port to surefire 2.10
 *    Red Hat Inc.  - Lazier logging of resolution error
 *******************************************************************************/
package org.eclipse.tycho.surefire.osgibooter;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.maven.plugin.surefire.StartupReportConfiguration;
import org.apache.maven.plugin.surefire.log.api.PrintStreamLogger;
import org.apache.maven.plugin.surefire.report.DefaultReporterFactory;
import org.apache.maven.surefire.booter.BooterConstants;
import org.apache.maven.surefire.booter.ClassLoaderConfiguration;
import org.apache.maven.surefire.booter.ClasspathConfiguration;
import org.apache.maven.surefire.booter.ForkedBooter;
import org.apache.maven.surefire.booter.PropertiesWrapper;
import org.apache.maven.surefire.booter.ProviderConfiguration;
import org.apache.maven.surefire.booter.ProviderFactory;
import org.apache.maven.surefire.booter.Shutdown;
import org.apache.maven.surefire.booter.StartupConfiguration;
import org.apache.maven.surefire.cli.CommandLineOption;
import org.apache.maven.surefire.report.ReporterConfiguration;
import org.apache.maven.surefire.report.ReporterFactory;
import org.apache.maven.surefire.suite.RunResult;
import org.apache.maven.surefire.testset.DirectoryScannerParameters;
import org.apache.maven.surefire.testset.RunOrderParameters;
import org.apache.maven.surefire.testset.TestListResolver;
import org.apache.maven.surefire.testset.TestRequest;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.internal.framework.EquinoxBundle;
import org.eclipse.osgi.service.resolver.ResolverError;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;

public class OsgiSurefireBooter {
    private static final String XSD = "https://maven.apache.org/surefire/maven-surefire-plugin/xsd/surefire-test-report.xsd";

    public static int run(String[] args) throws Exception {
        Properties testProps = loadProperties(getTestProperties(args));
        boolean failIfNoTests = Boolean.parseBoolean(testProps.getProperty("failifnotests", "false"));
        boolean redirectTestOutputToFile = Boolean
                .parseBoolean(testProps.getProperty("redirectTestOutputToFile", "false"));
        String testPlugin = testProps.getProperty("testpluginname");
        File testClassesDir = new File(testProps.getProperty("testclassesdirectory"));
        File reportsDir = new File(testProps.getProperty("reportsdirectory"));
        String provider = testProps.getProperty("testprovider");
        String runOrder = testProps.getProperty("runOrder");
        boolean trimStackTrace = Boolean.parseBoolean(testProps.getProperty("trimStackTrace", "true"));
        int skipAfterFailureCount = Integer.parseInt(testProps.getProperty("skipAfterFailureCount", "0"));
        int rerunFailingTestsCount = Integer.parseInt(testProps.getProperty("rerunFailingTestsCount", "0"));
        Map<String, String> propertiesMap = new HashMap<String, String>();
        for (String key : testProps.stringPropertyNames()) {
            propertiesMap.put(key, testProps.getProperty(key));
        }
        PropertiesWrapper wrapper = new PropertiesWrapper(propertiesMap);
        List<String> suiteXmlFiles = wrapper.getStringList(BooterConstants.TEST_SUITE_XML_FILES);

        boolean forkRequested = true;
        boolean inForkedVM = true;
        boolean useSystemClassloader = false;
        boolean useManifestOnlyJar = false;
        boolean useFile = true;
        boolean printSummary = true;
        boolean disableXmlReport = false;

        ClasspathConfiguration classPathConfig = new ClasspathConfiguration(false, false);
        StartupConfiguration startupConfiguration = new StartupConfiguration(provider, classPathConfig,
                new ClassLoaderConfiguration(useSystemClassloader, useManifestOnlyJar), forkRequested, inForkedVM);
        // TODO dir scanning with no includes done here (done in TestMojo already) 
        // but without dirScannerParams we get an NPE accessing runOrder
        DirectoryScannerParameters dirScannerParams = new DirectoryScannerParameters(testClassesDir,
                Collections.<String> emptyList(), Collections.<String> emptyList(), Collections.<String> emptyList(),
                failIfNoTests, runOrder);
        ReporterConfiguration reporterConfig = new ReporterConfiguration(reportsDir, trimStackTrace);
        TestRequest testRequest = new TestRequest(suiteXmlFiles, testClassesDir,
                TestListResolver.getEmptyTestListResolver());
        ProviderConfiguration providerConfiguration = new ProviderConfiguration(dirScannerParams,
                new RunOrderParameters(runOrder, null), failIfNoTests, reporterConfig, null, testRequest,
                extractProviderProperties(testProps), null, false, Collections.<CommandLineOption> emptyList(),
                skipAfterFailureCount, Shutdown.DEFAULT, 30);
        StartupReportConfiguration startupReportConfig = new StartupReportConfiguration(useFile, printSummary,
                StartupReportConfiguration.PLAIN_REPORT_FORMAT, redirectTestOutputToFile, disableXmlReport, reportsDir,
                trimStackTrace, null, new File(reportsDir, "TESTHASH"), false, rerunFailingTestsCount, XSD, null);
        ReporterFactory reporterFactory = new DefaultReporterFactory(startupReportConfig,
                new PrintStreamLogger(startupReportConfig.getOriginalSystemOut()));
        // API indicates we should use testClassLoader below but surefire also tries 
        // to load surefire classes using this classloader
        RunResult result = ProviderFactory.invokeProvider(null, createCombinedClassLoader(testPlugin), reporterFactory,
                providerConfiguration, false, startupConfiguration, true);
        // counter-intuitive, but null indicates OK here
        return result.getFailsafeCode() == null ? 0 : result.getFailsafeCode();
    }

    private static ClassLoader createCombinedClassLoader(String testPlugin) throws BundleException {
        ClassLoader testClassLoader = getBundleClassLoader(testPlugin);
        ClassLoader surefireClassLoader = ForkedBooter.class.getClassLoader();
        return new CombinedClassLoader(testClassLoader, surefireClassLoader);
    }

    /*
     * See TestMojo#mergeProviderProperties
     */
    private static Map<String, String> extractProviderProperties(Properties surefireProps) {
        Map<String, String> providerProps = new HashMap<String, String>();
        for (String entry : surefireProps.stringPropertyNames()) {
            if (entry.startsWith("__provider.")) {
                providerProps.put(entry.substring("__provider.".length()), surefireProps.getProperty(entry));
            }
        }
        return providerProps;
    }

    private static File getTestProperties(String[] args) throws CoreException {
        String arg = null;
        for (int i = 0; i < args.length; i++) {
            if ("-testproperties".equals(args[i].toLowerCase())) {
                arg = args[i + 1];
                break;
            }
        }
        if (arg != null) {
            File file = new File(arg);
            if (file.canRead()) {
                return file;
            }
        }
        throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, 0,
                "-testproperties command line parameter is not specified or does not point to an accessible file",
                null));
    }

    private static List<String> getIncludesExcludes(String string) {
        ArrayList<String> list = new ArrayList<String>();
        if (string != null) {
            list.addAll(Arrays.asList(string.split(",")));
        }
        return list;
    }

    private static Properties loadProperties(File file) throws IOException {
        Properties p = new Properties();
        BufferedInputStream in = new BufferedInputStream(new FileInputStream(file));
        try {
            p.load(in);
        } finally {
            in.close();
        }
        return p;
    }

    private static ClassLoader getBundleClassLoader(String symbolicName) throws BundleException {
        Bundle bundle = Activator.getBundle(symbolicName);
        if (bundle == null) {
            throw new RuntimeException("Bundle " + symbolicName + " is not found");
        }
        try {
            bundle.start();
        } catch (BundleException ex) {
            if (ex.getType() == BundleException.RESOLVE_ERROR) {
                System.err.println("Resolution errors for " + bundle.toString());
                Set<ResolverError> errors = Activator.getResolutionErrors(bundle);
                if (errors.size() > 0) {
                    for (ResolverError error : errors) {
                        System.err.println("\t" + error.toString());
                    }
                }
            } else {
                System.err.println("Could not start test bundle: " + bundle.getSymbolicName());
                ex.printStackTrace();
            }
            throw ex;
        }

        return new BundleClassLoader(bundle);
    }

    private static class BundleClassLoader extends ClassLoader {
        private Bundle bundle;

        public BundleClassLoader(Bundle target) {
            this.bundle = target;
        }

        protected Class<?> findClass(String name) throws ClassNotFoundException {
            try {
                return bundle.loadClass(name);
            } catch (ClassNotFoundException cnfe) {
                Bundle[] installedBundles = bundle.getBundleContext().getBundles();
                for (Bundle currentBundle : installedBundles) {
                    if (bundle != currentBundle) {
                        try {
                            return currentBundle.loadClass(name);
                        } catch (ClassNotFoundException e) {
                            // ignore error
                        }
                    }
                }
                throw cnfe;
            }
        }

        protected URL findResource(String name) {
            return bundle.getResource(name);
        }

        protected Enumeration<URL> findResources(String name) throws IOException {
            Enumeration<URL> resources = bundle.getResources(name);
            if ((resources == null || !resources.hasMoreElements()) && bundle instanceof EquinoxBundle) {
                CombinedEnumeration<URL> combiEnum = new CombinedEnumeration<URL>();
                Bundle[] installedBundles = bundle.getBundleContext().getBundles();
                for (Bundle currentBundle : installedBundles) {
                    if (bundle != currentBundle) {
                        try {
                            combiEnum.add(currentBundle.getResources(name));
                        } catch (IOException e) {
                            // ignore error
                        }
                    }
                }
                return combiEnum;
            }
            return resources;
        }

        private static class CombinedEnumeration<T> implements Enumeration<T> {
            private final LinkedList<Enumeration<T>> list = new LinkedList<Enumeration<T>>();
            private Iterator<Enumeration<T>> iter = null;
            private Enumeration<T> cursor = null;

            public void add(Enumeration<T> e) {
                if (e != null && e.hasMoreElements()) {
                    list.add(e);
                }
            }

            public boolean hasMoreElements() {
                if (list.isEmpty()) {
                    return false;
                }
                if (iter == null) {
                    iter = list.iterator();
                }
                while (cursor == null || !cursor.hasMoreElements()) {
                    if (!iter.hasNext()) {
                        return false;
                    }
                    cursor = iter.next();
                }
                return cursor.hasMoreElements();
            }

            public T nextElement() {
                return cursor.nextElement();
            }
        }

    }
}
