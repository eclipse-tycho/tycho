/*******************************************************************************
 * Copyright (c) 2008, 2011 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *    SAP AG        - port to surefire 2.10
 *******************************************************************************/
package org.eclipse.tycho.surefire.osgibooter;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.maven.surefire.booter.ClassLoaderConfiguration;
import org.apache.maven.surefire.booter.ProviderConfiguration;
import org.apache.maven.surefire.booter.StartupConfiguration;
import org.apache.maven.surefire.booter.StartupReportConfiguration;
import org.apache.maven.surefire.booter.SurefireStarter;
import org.apache.maven.surefire.report.ReporterConfiguration;
import org.apache.maven.surefire.suite.RunResult;
import org.apache.maven.surefire.testset.DirectoryScannerParameters;
import org.apache.maven.surefire.testset.TestRequest;
import org.apache.maven.surefire.util.RunOrder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.service.resolver.ResolverError;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;

public class OsgiSurefireBooter {

    public static int run(String[] args) throws Exception {
        Properties testProps = loadProperties(getTestProperties(args));
        boolean failIfNoTests = Boolean.parseBoolean(testProps.getProperty("failifnotests", "false"));
        boolean redirectTestOutputToFile = Boolean.parseBoolean(testProps.getProperty("redirectTestOutputToFile",
                "false"));
        String plugin = testProps.getProperty("testpluginname");
        File testClassesDir = new File(testProps.getProperty("testclassesdirectory"));
        File reportsDir = new File(testProps.getProperty("reportsdirectory"));
        String provider = testProps.getProperty("testprovider");
        String runOrder = testProps.getProperty("runOrder");
        List<String> includes = getIncludesExcludes(testProps.getProperty("includes"));
        List<String> excludes = getIncludesExcludes(testProps.getProperty("excludes"));

        String forkMode = "never";
        boolean inForkedVM = true;
        boolean trimStacktrace = true;
        boolean useSystemClassloader = false;
        boolean useManifestOnlyJar = false;
        boolean useFile = true;
        boolean printSummary = true;
        boolean disableXmlReport = false;
        ClassLoader testClassLoader = getBundleClassLoader(plugin);
        ClassLoader surefireClassLoader = SurefireStarter.class.getClassLoader();

        TychoClasspathConfiguration classPathConfig = new TychoClasspathConfiguration(testClassLoader,
                surefireClassLoader);
        StartupConfiguration startupConfiguration = new StartupConfiguration(provider, classPathConfig,
                new ClassLoaderConfiguration(useSystemClassloader, useManifestOnlyJar), forkMode, inForkedVM);
        DirectoryScannerParameters dirScannerParams = new DirectoryScannerParameters(testClassesDir, includes,
                excludes, failIfNoTests, RunOrder.valueOf(runOrder));
        ReporterConfiguration reporterConfig = new ReporterConfiguration(reportsDir, trimStacktrace);
        TestRequest testRequest = new TestRequest(null, testClassesDir, null);
        ProviderConfiguration providerConfiguration = new ProviderConfiguration(dirScannerParams, failIfNoTests,
                reporterConfig, null, testRequest, extractProviderProperties(testProps), null);
        StartupReportConfiguration startupReportConfig = new StartupReportConfiguration(useFile, printSummary,
                StartupReportConfiguration.PLAIN_REPORT_FORMAT, redirectTestOutputToFile, disableXmlReport, reportsDir,
                trimStacktrace);
        SurefireStarter surefireStarter = new SurefireStarter(startupConfiguration, providerConfiguration,
                startupReportConfig);

        RunResult result = surefireStarter.runSuitesInProcess();
        return result.getForkedProcessCode();
    }

    /*
     * See TestMojo#mergeProviderProperties
     */
    private static Properties extractProviderProperties(Properties surefireProps) {
        Properties providerProps = new Properties();
        for (Map.Entry entry : surefireProps.entrySet()) {
            String key = (String) entry.getKey();
            if (key.startsWith("__provider.")) {
                providerProps.put(key.substring("__provider.".length()), entry.getValue());
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
        throw new CoreException(
                new Status(
                        IStatus.ERROR,
                        Activator.PLUGIN_ID,
                        0,
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

        Set<ResolverError> errors = Activator.getResolutionErrors(bundle);
        if (errors.size() > 0) {
            System.err.println("Resolution errors for " + bundle.toString());
            for (ResolverError error : errors) {
                System.err.println("\t" + error.toString());
            }
        }

        bundle.start();

        return new BundleClassLoader(bundle);
    }

    private static class BundleClassLoader extends ClassLoader {
        private Bundle bundle;

        public BundleClassLoader(Bundle target) {
            this.bundle = target;
        }

        protected Class findClass(String name) throws ClassNotFoundException {
            return bundle.loadClass(name);
        }

        protected URL findResource(String name) {
            return bundle.getResource(name);
        }

        protected Enumeration findResources(String name) throws IOException {
            return bundle.getResources(name);
        }
    }
}
