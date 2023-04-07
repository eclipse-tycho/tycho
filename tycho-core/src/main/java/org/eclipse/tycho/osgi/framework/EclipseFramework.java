/*******************************************************************************
 * Copyright (c) 2023 Christoph Läubrich and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.osgi.framework;

import java.util.Arrays;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.internal.adaptor.EclipseAppLauncher;
import org.eclipse.osgi.internal.framework.EquinoxConfiguration;
import org.eclipse.osgi.service.runnable.ApplicationLauncher;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.launch.Framework;

public class EclipseFramework implements AutoCloseable {

    private Framework framework;
    private EquinoxConfiguration configuration;
    private EclipseApplication application;

    EclipseFramework(Framework framework, EquinoxConfiguration configuration, EclipseApplication application) {
        this.framework = framework;
        this.configuration = configuration;
        this.application = application;
    }

    @Override
    public void close() {
        try {
            framework.stop();
            framework.waitForStop(0);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (BundleException e) {
            // not interesting...
        }
    }

    public void start() throws Exception {
        framework.start();
        String[] args = configuration.getNonFrameworkArgs();
        for (String arg : args) {
            if (EclipseApplication.ARG_APPLICATION.equals(arg)) {
                int exitCode = launchApplication(framework.getBundleContext(), configuration);
                if (exitCode != 0) {
                    throw new Exception("Application returned exit code " + exitCode);
                }
                return;
            }
        }
    }

    private int launchApplication(BundleContext systemBundleContext, EquinoxConfiguration configuration)
            throws Exception {
        EclipseAppLauncher appLauncher = new EclipseAppLauncher(systemBundleContext, false, true, null, configuration);
        systemBundleContext.registerService(ApplicationLauncher.class, appLauncher, null);
        Object returnValue;
        try {
            returnValue = appLauncher.start(null);
        } catch (Exception e) {
            throw applicationStartupError(systemBundleContext, e);
        }
        if (returnValue instanceof Integer retCode) {
            return retCode.intValue();
        }
        throw applicationStartupError(systemBundleContext, null);
    }

    private Exception applicationStartupError(BundleContext systemBundleContext, Exception e) {
        String bundleState = Arrays.stream(systemBundleContext.getBundles())
                .map(b -> toBundleState(b.getState()) + " | " + b.getSymbolicName())
                .collect(Collectors.joining(System.lineSeparator()));
        application.getLogger().error(String.format("Internal error execute the " + application.getName()
                + " application, the current framework state is:\r\n%s", bundleState), e);
        return e;
    }

    private static String toBundleState(int state) {
        return switch (state) {
        case Bundle.ACTIVE -> "ACTIVE   ";
        case Bundle.INSTALLED -> "INSTALLED";
        case Bundle.RESOLVED -> "RESOLVED ";
        case Bundle.STARTING -> "STARTING ";
        case Bundle.STOPPING -> "STOPPING ";
        default -> String.valueOf(state);
        };
    }

}
