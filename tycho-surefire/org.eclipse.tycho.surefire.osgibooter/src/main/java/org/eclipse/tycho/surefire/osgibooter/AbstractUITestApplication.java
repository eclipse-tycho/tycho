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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.Platform;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.testing.ITestHarness;
import org.eclipse.ui.testing.TestableObject;

public abstract class AbstractUITestApplication implements ITestHarness {

    private static final String DEFAULT_APP_3_0 = "org.eclipse.ui.ide.workbench"; //$NON-NLS-1$

    public static final int SUREFIRE_COULD_NOT_PERFORM_TESTS = 111; // arbitrary value

    private int fTestRunnerResult = -1;
    private String[] fArgs = new String[0];
    private TestableObject fTestableObject;

    public void runTests() {
        fTestableObject.testingStarting();
        if (useUIThread(fArgs)) {
            fTestableObject.runTest(new Runnable() {
                public void run() {
                    try {
                        fTestRunnerResult = OsgiSurefireBooter.run(fArgs);
                    } catch (Exception e) {
                        e.printStackTrace();
                        fTestRunnerResult = SUREFIRE_COULD_NOT_PERFORM_TESTS;
                    }
                }
            });
        } else {
            try {
                fTestRunnerResult = OsgiSurefireBooter.run(fArgs);
            } catch (Exception e) {
                e.printStackTrace();
                fTestRunnerResult = SUREFIRE_COULD_NOT_PERFORM_TESTS;
            }
        }
        fTestableObject.testingFinished();
    }

    private boolean useUIThread(String[] args) {
        if (args != null) {
            for (int i = 0; i < args.length; i++) {
                if ("-nouithread".equals(args[i])) {
                    return false;
                }
            }
        }
        return true;
    }

    /*
     * return the application to run, or null if not even the default application is found.
     */
    private Object getApplication(String[] args) throws CoreException {
        // Assume we are in 3.0 mode.
        // Find the name of the application as specified by the PDE JUnit launcher.
        // If no application is specified, the 3.0 default workbench application
        // is returned.
        IExtension extension = Platform.getExtensionRegistry().getExtension(Platform.PI_RUNTIME,
                Platform.PT_APPLICATIONS, getApplicationToRun(args));

        // If no 3.0 extension can be found, search the registry
        // for the pre-3.0 default workbench application, i.e. org.eclipse ui.workbench
        // Set the deprecated flag to true
        if (extension == null) {
            return null;
        }

        // If the extension does not have the correct grammar, return null.
        // Otherwise, return the application object.
        IConfigurationElement[] elements = extension.getConfigurationElements();
        if (elements.length > 0) {
            IConfigurationElement[] runs = elements[0].getChildren("run"); //$NON-NLS-1$
            if (runs.length > 0) {
                return runs[0].createExecutableExtension("class"); //$NON-NLS-1$
            }
        }
        return null;
    }

    /**
     * The -testApplication argument specifies the application to be run. If the PDE JUnit launcher
     * did not set this argument, then return the name of the default application. In 3.0, the
     * default is the "org.eclipse.ui.ide.worbench" application.
     * 
     */
    private String getApplicationToRun(String[] args) {
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-testApplication") && i < args.length - 1) //$NON-NLS-1$
                return args[i + 1];
        }
        return DEFAULT_APP_3_0;
    }

    protected Object run(String[] args) throws Exception {
        if (args != null)
            fArgs = args;
        fTestableObject = PlatformUI.getTestableObject();
        fTestableObject.setTestHarness(this);
        Object application = getApplication(args);
        try {
            runApplication(application, args);
            return new Integer(fTestRunnerResult);
        } catch (Throwable ex) {
            // Throwable to catch also SWTError.
            // A throwable at this point make JVM return Exit code 13
            // as implemented in org.eclipse.equinox.launcher.Main
            throw new Exception("Error while starting testApplication", ex);
        }
    }

    protected abstract void runApplication(Object application, String[] args) throws Exception;
}
