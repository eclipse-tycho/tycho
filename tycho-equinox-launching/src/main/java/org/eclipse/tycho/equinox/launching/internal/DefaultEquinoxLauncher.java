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
package org.eclipse.tycho.equinox.launching.internal;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.Commandline;
import org.codehaus.plexus.util.cli.StreamConsumer;
import org.eclipse.tycho.equinox.launching.EquinoxLauncher;
import org.eclipse.tycho.equinox.launching.EquinoxLaunchingException;
import org.eclipse.tycho.launching.LaunchConfiguration;

@Component(role = EquinoxLauncher.class)
public class DefaultEquinoxLauncher implements EquinoxLauncher {
    @Requirement
    private Logger log;

    public int execute(LaunchConfiguration configuration, int forkedProcessTimeoutInSeconds)
            throws EquinoxLaunchingException {
        Commandline cli = new Commandline();

        String executable = configuration.getJvmExecutable();
        if (executable == null || "".equals(executable)) {
            // use the same JVM as the one used to run Maven (the "java.home" one)
            executable = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
            if (File.separatorChar == '\\') {
                executable = executable + ".exe";
            }
        }
        cli.setExecutable(executable);

        cli.setWorkingDirectory(configuration.getWorkingDirectory());

        cli.addArguments(configuration.getVMArguments());

        cli.addArguments(new String[] { "-jar", getCanonicalPath(configuration.getLauncherJar()) });

        cli.addArguments(configuration.getProgramArguments());

        for (Map.Entry<String, String> var : configuration.getEnvironment().entrySet()) {
            cli.addEnvironment(var.getKey(), var.getValue());
        }

        log.info("Command line:\n\t" + cli.toString());

        StreamConsumer out = new StreamConsumer() {
            public void consumeLine(String line) {
                System.out.println(line);
            }
        };
        StreamConsumer err = new StreamConsumer() {
            public void consumeLine(String line) {
                System.err.println(line);
            }
        };
        try {
            return CommandLineUtils.executeCommandLine(cli, out, err, forkedProcessTimeoutInSeconds);
        } catch (CommandLineException e) {
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
