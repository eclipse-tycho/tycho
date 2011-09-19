/*******************************************************************************
 * Copyright (c) 2008, 2011 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
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
import java.util.Properties;
import java.util.Set;

import org.apache.maven.surefire.Surefire;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.service.resolver.ResolverError;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;

public class OsgiSurefireBooter {

    public static int run(String[] args) throws Exception {

        Properties p = loadProperties(getTestProperties(args));

        String plugin = p.getProperty("testpluginname");
        File testDir = new File(p.getProperty("testclassesdirectory"));
        File reportsDir = new File(p.getProperty("reportsdirectory"));

        String runner = p.getProperty("testrunner");

        ArrayList<String> includes = getIncludesExcludes(p.getProperty("includes"));
        ArrayList<String> excludes = getIncludesExcludes(p.getProperty("excludes"));

        ClassLoader testClassLoader = getBundleClassLoader(plugin);
        ClassLoader surefireClassLoader = Surefire.class.getClassLoader();

        Surefire surefire = new Surefire();

        List reports = new ArrayList();
        reports.add(new Object[] { "org.apache.maven.surefire.report.BriefConsoleReporter", new Object[] { Boolean.TRUE /* trimStackTrace */
        } });
        reports.add(new Object[] { "org.apache.maven.surefire.report.FileReporter",
                new Object[] { reportsDir, Boolean.TRUE /* trimStackTrace */
                } });
        reports.add(new Object[] { "org.apache.maven.surefire.report.XMLReporter",
                new Object[] { reportsDir, Boolean.TRUE /* trimStackTrace */
                } });

        List tests = new ArrayList();
        tests.add(new Object[] { runner, new Object[] { testDir, includes, excludes } });

        Boolean failIfNoTests;
        if ("false".equalsIgnoreCase(p.getProperty("failifnotests"))) {
            failIfNoTests = Boolean.FALSE;
        } else {
            failIfNoTests = Boolean.TRUE;
        }
        return surefire.run(reports, tests, surefireClassLoader, testClassLoader, failIfNoTests);
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

    private static ArrayList<String> getIncludesExcludes(String string) {
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
