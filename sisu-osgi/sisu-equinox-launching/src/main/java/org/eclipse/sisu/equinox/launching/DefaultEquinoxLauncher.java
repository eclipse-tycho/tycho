/*******************************************************************************
 * Copyright (c) 2008, 2019 Sonatype Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.sisu.equinox.launching;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.ShutdownHookProcessDestroyer;
import org.apache.commons.exec.environment.EnvironmentUtils;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.sisu.osgi.launching.FrameworkLauncher;
import org.eclipse.sisu.osgi.launching.LaunchConfiguration;

@Component(role = FrameworkLauncher.class)
public class DefaultEquinoxLauncher implements FrameworkLauncher {
    @Requirement
    private Logger log;

    @Override
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

        final boolean handleQuotes = false;
        cli.addArguments(configuration.getVMArguments(), handleQuotes);

        cli.addArguments(new String[] { "-jar", getCanonicalPath(configuration.getLauncherJar()) }, handleQuotes);

        cli.addArguments(configuration.getProgramArguments(), handleQuotes);

        DefaultExecutor executor = new DefaultExecutor();
        ExecuteWatchdog watchdog = null;
        if (forkedProcessTimeoutInSeconds > 0) {
            long timeoutInMilliseconds = forkedProcessTimeoutInSeconds * 1000L;
            cli.addArguments(new String[] { "-timeout ", String.valueOf(timeoutInMilliseconds) });
            watchdog = new ExecuteWatchdog(timeoutInMilliseconds);
            executor.setWatchdog(watchdog);
        }

        log.info("Command line:\n\t" + cli.toString());

        // best effort to avoid orphaned child process
        executor.setProcessDestroyer(new ShutdownHookProcessDestroyer());
        executor.setWorkingDirectory(configuration.getWorkingDirectory());
        try {
            return executor.execute(cli, getMergedEnvironment(configuration));
        } catch (ExecuteException e) {
            if (watchdog != null && watchdog.killedProcess()) {
                log.error("Timeout " + forkedProcessTimeoutInSeconds + " s exceeded. Process was killed.");
            }
            return e.getExitValue();
        } catch (IOException e) {
            throw new EquinoxLaunchingException(e);
        }
    }

    private static Map<String, String> getMergedEnvironment(LaunchConfiguration configuration) throws IOException {
        Map<String, String> currentEnv = EnvironmentUtils.getProcEnvironment();
        currentEnv.putAll(configuration.getEnvironment());
        return currentEnv;
    }

    private String getCanonicalPath(File file) throws EquinoxLaunchingException {
        try {
            return file.getCanonicalPath();
        } catch (IOException e) {
            throw new EquinoxLaunchingException(e);
        }
    }
}
