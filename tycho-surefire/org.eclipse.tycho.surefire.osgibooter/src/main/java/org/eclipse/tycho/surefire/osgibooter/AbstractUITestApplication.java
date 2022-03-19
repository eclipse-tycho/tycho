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
 *    Christoph Läubrich - [Issue 790] Support printing of bundle wirings in tycho-surefire-plugin
 *******************************************************************************/
package org.eclipse.tycho.surefire.osgibooter;

import java.util.Properties;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.Platform;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.testing.ITestHarness;
import org.eclipse.ui.testing.TestableObject;

public abstract class AbstractUITestApplication implements ITestHarness {

    private static final String DEFAULT_APP_3_0 = "org.eclipse.ui.ide.workbench"; //$NON-NLS-1$

    private int fTestRunnerResult = -1;
    private String[] fArgs = new String[0];
    private TestableObject fTestableObject;

    public void runTests() {
        fTestableObject.testingStarting();
        if (useUIThread(fArgs)) {
            fTestableObject.runTest(new Runnable() {
                public void run() {
                    try {
                        fTestRunnerResult = OsgiSurefireBooter.run(fArgs, getTestProperties());
                    } catch (Exception e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            });
        } else {
            try {
                fTestRunnerResult = OsgiSurefireBooter.run(fArgs, getTestProperties());
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        fTestableObject.testingFinished();
    }

    protected abstract Properties getTestProperties();

    private boolean useUIThread(String[] args) {
        if (args != null) {
            for (String arg : args) {
                if ("-nouithread".equals(arg)) {
                    return false;
                }
            }
        }
        return true;
    }

    /*
     * return the application to run, or null if not even the default application is found.
     */
    private Object getApplicationToRun(String[] args) throws CoreException {
        String configuredApplication = getConfiguredApplication(args);
        if (configuredApplication == null) {
            configuredApplication = DEFAULT_APP_3_0;
        } else {
            System.out.println("Launching application " + configuredApplication + "...");
        }

        // Assume we are in 3.0 mode.
        // Find the name of the application as specified by the PDE JUnit launcher.
        // If no application is specified, the 3.0 default workbench application
        // is returned.
        IExtension extension = Platform.getExtensionRegistry().getExtension(Platform.PI_RUNTIME,
                Platform.PT_APPLICATIONS, configuredApplication);

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

    private String getConfiguredApplication(String[] args) {
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-testApplication") && i < args.length - 1) //$NON-NLS-1$
                return args[i + 1];
        }
        return null;
    }

    protected Object run(String[] args) throws Exception {
        if (args != null)
            fArgs = args;
        fTestableObject = PlatformUI.getTestableObject();
        fTestableObject.setTestHarness(this);
        try {
            Object application = getApplicationToRun(args);

            if (application == null) {
                return Integer.valueOf(200);
            }
            runApplication(application, args, getTestProperties());
        } catch (Exception e) {
            if (fTestRunnerResult == -1) {
                throw e;
            }
            // the exception was thrown after test runner returned. this is most likely a bug in Eclipse Platform
            // see for example, https://bugs.eclipse.org/bugs/show_bug.cgi?id=436159
            // there is no point to fail the build because of this, just log and ignore
            System.err.println("Caught unexpected exception during test framework shutdown");
            e.printStackTrace();
            // TODO funnel exceptions to LogService
        }
        return Integer.valueOf(fTestRunnerResult);
    }

    protected abstract void runApplication(Object application, String[] args, Properties testProps) throws Exception;
}
