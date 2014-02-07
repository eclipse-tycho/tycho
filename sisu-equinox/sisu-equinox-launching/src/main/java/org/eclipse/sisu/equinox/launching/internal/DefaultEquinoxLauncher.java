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
package org.eclipse.sisu.equinox.launching.internal;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.ExecuteWatchdog;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.sisu.equinox.launching.EquinoxLauncher;
import org.eclipse.sisu.equinox.launching.EquinoxLaunchingException;
import org.eclipse.tycho.launching.LaunchConfiguration;

@Component(role = EquinoxLauncher.class)
public class DefaultEquinoxLauncher implements EquinoxLauncher {
    @Requirement
    private Logger log;

    public int execute(LaunchConfiguration configuration, int forkedProcessTimeoutInSeconds)
            throws EquinoxLaunchingException {

        String executable = configuration.getJvmExecutable();
        if (executable == null || "".equals(executable)) {
            // use the same JVM as the one used to run Maven (the "java.home" one)
            executable = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
            if (File.separatorChar == '\\') {
                executable = executable + ".exe";
            }
        }
        CommandLine cli = new CommandLine(executable);

        cli.addArguments(configuration.getVMArguments(), false);

        cli.addArguments(new String[] { "-jar", getCanonicalPath(configuration.getLauncherJar()) }, false);

        cli.addArguments(configuration.getProgramArguments(), false);

        log.info("Command line:\n\t" + cli.toString());

        DefaultExecutor executor = new DefaultExecutor();
        ExecuteWatchdog watchdog = null;
        if (forkedProcessTimeoutInSeconds > 0) {
            watchdog = new ExecuteWatchdog(forkedProcessTimeoutInSeconds * 1000L);
            executor.setWatchdog(watchdog);
        }
        executor.setWorkingDirectory(configuration.getWorkingDirectory());
        try {
            Map<String, String> environment = configuration.getEnvironment();
            if (environment.isEmpty()) {
                // null means use the current environment
                environment = null;
            }
            return executor.execute(cli, environment);
        } catch (ExecuteException e) {
            if (watchdog != null && watchdog.killedProcess()) {
                log.error("Timeout " + forkedProcessTimeoutInSeconds + " s exceeded. Process was killed.");
            }
            return e.getExitValue();
        } catch (IOException e) {
            throw new EquinoxLaunchingException(e);
        }
    }

    private String getCanonicalPath(File file) throws EquinoxLaunchingException {
        try {
            return file.getCanonicalPath();
        } catch (IOException e) {
            throw new EquinoxLaunchingException(e);
        }
    }
}
