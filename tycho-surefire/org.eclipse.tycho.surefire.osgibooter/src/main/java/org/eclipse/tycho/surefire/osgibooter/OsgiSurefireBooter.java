/*******************************************************************************
 * Copyright (c) 2008, 2018 Sonatype Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
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
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.maven.plugin.failsafe.util.FailsafeSummaryXmlUtils;
import org.apache.maven.plugin.surefire.StartupReportConfiguration;
import org.apache.maven.plugin.surefire.log.api.PrintStreamLogger;
import org.apache.maven.plugin.surefire.report.ConsoleReporter;
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
import org.eclipse.osgi.service.resolver.ResolverError;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;

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

        String timeoutParameter = getArgumentValue(args, "-timeout");
        if (timeoutParameter != null) {
            DumpStackTracesTimer.startStackDumpTimeoutTimer(timeoutParameter);
        }

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
                ConsoleReporter.PLAIN, redirectTestOutputToFile, disableXmlReport, reportsDir, trimStackTrace, null,
                new File(reportsDir, "TESTHASH"), false, rerunFailingTestsCount, XSD, null, false);
        ReporterFactory reporterFactory = new DefaultReporterFactory(startupReportConfig,
                new PrintStreamLogger(startupReportConfig.getOriginalSystemOut()));
        // API indicates we should use testClassLoader below but surefire also tries 
        // to load surefire classes using this classloader
        List<URL> testClasspath = new ArrayList<URL>();
        for (int i = 0; i < args.length; i++) {
            String key = args[i];
            if ("-rtcp".equals(key)) {
                i++;
                testClasspath.add(new File(args[i]).toURI().toURL());
            }
        }
        RunResult result = ProviderFactory.invokeProvider(null, createCombinedClassLoader(testPlugin, testClasspath),
                reporterFactory, providerConfiguration, false, startupConfiguration, true);
        String failsafe = getArgumentValue(args, "-failsafe");
        if (failsafe != null) {
            FailsafeSummaryXmlUtils.writeSummary(result, new File(failsafe), false);
        }
        // counter-intuitive, but null indicates OK here
        return result.getFailsafeCode() == null ? 0 : result.getFailsafeCode();
    }

    private static ClassLoader createCombinedClassLoader(String testPlugin, List<URL> testClasspath)
            throws BundleException {
        Bundle testBundle = getBundle(testPlugin);
        Bundle[] bundles = getBundle(Constants.SYSTEM_BUNDLE_SYMBOLICNAME).getBundleContext().getBundles();
        List<ClassLoader> otherBundles = new ArrayList<ClassLoader>();
        for (Bundle bundle : bundles) {
            if (bundle != testBundle) {
                otherBundles.add(new BundleClassLoader(bundle));
            }
        }
        ClassLoader mavenClasspath = new CombinedClassLoader(otherBundles.toArray(new ClassLoader[0]));
        ClassLoader testClassLoader = getBundleClassLoader(testBundle);
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        ClassLoader surefireClassLoader = ForkedBooter.class.getClassLoader();
        return new CombinedClassLoader(testClassLoader,
                new URLClassLoader(testClasspath.toArray(new URL[0]), mavenClasspath), surefireClassLoader,
                // Not used contextClassLoader directly because it's a ContextFinder
                // which not work with tycho sufire osgibooster bundle
                new ContextFinderWithoutTychoBundle(contextClassLoader.getParent()));
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
        String arg = getArgumentValue(args, "-testproperties");
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

    private static String getArgumentValue(String[] args, String argumentName) {
        String arg = null;
        for (int i = 0; i < args.length; i++) {
            if (argumentName.equalsIgnoreCase(args[i]) && args.length >= i + 1) {
                arg = args[i + 1];
                break;
            }
        }
        return arg;
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

    private static ClassLoader getBundleClassLoader(Bundle bundle) throws BundleException {
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

    protected static Bundle getBundle(String symbolicName) {
        Bundle bundle = Activator.getBundle(symbolicName);
        if (bundle == null) {
            throw new RuntimeException("Bundle " + symbolicName + " is not found");
        }
        return bundle;
    }

    private static class BundleClassLoader extends ClassLoader {
        private Bundle bundle;

        public BundleClassLoader(Bundle target) {
            this.bundle = target;
        }

        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            return bundle.loadClass(name);
        }

        @Override
        protected URL findResource(String name) {
            return bundle.getResource(name);
        }

        @Override
        protected Enumeration<URL> findResources(String name) throws IOException {
            return bundle.getResources(name);
        }
    }

}
