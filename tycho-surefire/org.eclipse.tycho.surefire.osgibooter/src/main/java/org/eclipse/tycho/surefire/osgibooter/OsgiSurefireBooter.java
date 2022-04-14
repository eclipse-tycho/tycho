/*******************************************************************************
 * Copyright (c) 2008, 2022 Sonatype Inc. and others.
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
 *    Christoph Läubrich - [Issue 790] Support printing of bundle wirings in tycho-surefire-plugin
 *******************************************************************************/
package org.eclipse.tycho.surefire.osgibooter;

import static org.osgi.framework.namespace.PackageNamespace.PACKAGE_NAMESPACE;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.maven.plugin.failsafe.util.FailsafeSummaryXmlUtils;
import org.apache.maven.plugin.surefire.StartupReportConfiguration;
import org.apache.maven.plugin.surefire.extensions.SurefireConsoleOutputReporter;
import org.apache.maven.plugin.surefire.extensions.SurefireStatelessReporter;
import org.apache.maven.plugin.surefire.extensions.SurefireStatelessTestsetInfoReporter;
import org.apache.maven.plugin.surefire.log.api.PrintStreamLogger;
import org.apache.maven.plugin.surefire.report.ConsoleReporter;
import org.apache.maven.plugin.surefire.report.DefaultReporterFactory;
import org.apache.maven.surefire.api.booter.Shutdown;
import org.apache.maven.surefire.api.cli.CommandLineOption;
import org.apache.maven.surefire.api.report.ReporterConfiguration;
import org.apache.maven.surefire.api.report.ReporterFactory;
import org.apache.maven.surefire.api.suite.RunResult;
import org.apache.maven.surefire.api.testset.DirectoryScannerParameters;
import org.apache.maven.surefire.api.testset.RunOrderParameters;
import org.apache.maven.surefire.api.testset.TestListResolver;
import org.apache.maven.surefire.api.testset.TestRequest;
import org.apache.maven.surefire.booter.BooterConstants;
import org.apache.maven.surefire.booter.ClassLoaderConfiguration;
import org.apache.maven.surefire.booter.ClasspathConfiguration;
import org.apache.maven.surefire.booter.ForkedBooter;
import org.apache.maven.surefire.booter.ProcessCheckerType;
import org.apache.maven.surefire.booter.PropertiesWrapper;
import org.apache.maven.surefire.booter.ProviderConfiguration;
import org.apache.maven.surefire.booter.ProviderFactory;
import org.apache.maven.surefire.booter.StartupConfiguration;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.service.resolver.ResolverError;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;

public class OsgiSurefireBooter {
    private static final String XSD = "https://maven.apache.org/surefire/maven-surefire-plugin/xsd/surefire-test-report.xsd";

    public static int run(String[] args, Properties testProps) throws Exception {
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

        boolean useSystemClassloader = false;
        boolean useManifestOnlyJar = false;
        boolean useFile = true;
        boolean printSummary = true;
        boolean disableXmlReport = false;

        ClasspathConfiguration classPathConfig = new ClasspathConfiguration(false, false);
        StartupConfiguration startupConfiguration = new StartupConfiguration(provider, classPathConfig,
                new ClassLoaderConfiguration(useSystemClassloader, useManifestOnlyJar), ProcessCheckerType.ALL,
                new LinkedList<String[]>());
        // TODO dir scanning with no includes done here (done in TestMojo already)
        // but without dirScannerParams we get an NPE accessing runOrder
        DirectoryScannerParameters dirScannerParams = new DirectoryScannerParameters(testClassesDir,
                Collections.<String> emptyList(), Collections.<String> emptyList(), Collections.<String> emptyList(),
                failIfNoTests, runOrder);
        ReporterConfiguration reporterConfig = new ReporterConfiguration(reportsDir, trimStackTrace);
        TestRequest testRequest = new TestRequest(suiteXmlFiles, testClassesDir,
                TestListResolver.getEmptyTestListResolver(), rerunFailingTestsCount);
        ProviderConfiguration providerConfiguration = new ProviderConfiguration(dirScannerParams,
                new RunOrderParameters(runOrder, null), failIfNoTests, reporterConfig, null, testRequest,
                extractProviderProperties(testProps), null, false, Collections.<CommandLineOption> emptyList(),
                skipAfterFailureCount, Shutdown.DEFAULT, 30);
        SurefireConsoleOutputReporter consoleOutputReporter = new SurefireConsoleOutputReporter();
        //consoleOutputReporter.setDisable(true); // storing console output causes OOM, see https://github.com/eclipse/tycho/issues/879 & https://issues.apache.org/jira/browse/SUREFIRE-1845
        StartupReportConfiguration startupReportConfig = new StartupReportConfiguration(useFile, printSummary,
                ConsoleReporter.PLAIN, false, reportsDir, trimStackTrace, null, new File(reportsDir, "TESTHASH"), false,
                rerunFailingTestsCount, XSD, StandardCharsets.UTF_8.toString(), false,
                new SurefireStatelessReporter(disableXmlReport, null), consoleOutputReporter,
                new SurefireStatelessTestsetInfoReporter());
        ReporterFactory reporterFactory = new DefaultReporterFactory(startupReportConfig,
                new PrintStreamLogger(System.out));
        // API indicates we should use testClassLoader below but surefire also tries
        // to load surefire classes using this classloader
        RunResult result = ProviderFactory.invokeProvider(null, createCombinedClassLoader(testPlugin), reporterFactory,
                providerConfiguration, false, startupConfiguration, true);
        String failsafe = testProps.getProperty("failsafe");
        if (failsafe != null && !failsafe.isBlank()) {
            FailsafeSummaryXmlUtils.writeSummary(result, new File(failsafe), false);
        }
        // counter-intuitive, but null indicates OK here
        return result.getFailsafeCode() == null ? 0 : result.getFailsafeCode();
    }

    protected static void printBundleInfos(Properties testProps) {
        boolean printBundles = Boolean.parseBoolean(testProps.getProperty("printBundles"));
        boolean printWires = Boolean.parseBoolean(testProps.getProperty("printWires"));
        if (printBundles || printWires) {
            System.out.println("====== Installed Bundles ========");
            Bundle fwbundle = getBundle(Constants.SYSTEM_BUNDLE_SYMBOLICNAME);
            Bundle[] bundles = fwbundle.getBundleContext().getBundles();
            for (Bundle bundle : bundles) {
                System.out.println("[" + bundle.getBundleId() + "][" + getBundleState(bundle) + "] "
                        + bundle.getSymbolicName() + " (" + bundle.getVersion() + ")");
                if (printWires) {
                    printImports(bundle);
                }
            }
            System.out.println("=================================");
        }
    }

    private static String getBundleState(Bundle bundle) {
        int state = bundle.getState();
        switch (state) {
        case Bundle.UNINSTALLED:
            return "UNINSTALLED";
        case Bundle.INSTALLED:
            return "INSTALLED";
        case Bundle.RESOLVED:
            return "RESOLVED";
        case Bundle.STARTING:
            return "STARTING";
        case Bundle.STOPPING:
            return "STOPPING";
        case Bundle.ACTIVE:
            return "ACTIVE";
        default:
            return "UNKOWN";
        }
    }

    private static void printImports(Bundle source) {
        BundleWiring bundleWiring = source.adapt(BundleWiring.class);
        if (bundleWiring == null) {
            return;
        }
        List<BundleWire> wires = bundleWiring.getRequiredWires(PACKAGE_NAMESPACE);
        if (wires.isEmpty()) {
            return;
        }
        System.out.println(" Imported-Packages:");
        for (BundleWire wire : wires) {
            String pack = (String) wire.getCapability().getAttributes().get(PACKAGE_NAMESPACE);
            Bundle bundle = wire.getProviderWiring().getBundle();
            System.out.println("   " + pack + " <--> " + bundle.getSymbolicName() + " (" + bundle.getVersion() + ") @ "
                    + bundle.getLocation());
        }
    }

    private static ClassLoader createCombinedClassLoader(String testPlugin) throws BundleException {
        ClassLoader testClassLoader = getBundleClassLoader(testPlugin);
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        ClassLoader surefireClassLoader = ForkedBooter.class.getClassLoader();
        return new CombinedClassLoader(testClassLoader, surefireClassLoader,
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

    private static ClassLoader getBundleClassLoader(String symbolicName) throws BundleException {
        Bundle bundle = getBundle(symbolicName);
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

        @Override
        public String toString() {
            return bundle.getSymbolicName() + " [" + bundle.getVersion() + "]";
        }
    }

    public static Properties loadProperties(String[] args) throws IOException, CoreException {
        return loadProperties(getTestProperties(args));
    }

}
