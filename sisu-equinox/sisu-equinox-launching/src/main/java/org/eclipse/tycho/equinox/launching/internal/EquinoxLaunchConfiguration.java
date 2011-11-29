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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.plexus.util.cli.Commandline.Argument;
import org.eclipse.tycho.equinox.launching.EquinoxInstallation;
import org.eclipse.tycho.launching.LaunchConfiguration;

public class EquinoxLaunchConfiguration implements LaunchConfiguration {
    private String jvmExecutable;

    private File workingDirectory;

    private final Map<String, String> env = new LinkedHashMap<String, String>();

    private final List<Argument> args = new ArrayList<Argument>();

    private final List<Argument> vmargs = new ArrayList<Argument>();

    private final EquinoxInstallation installation;

    public EquinoxLaunchConfiguration(EquinoxInstallation installation) {
        this.installation = installation;
    }

    public void addEnvironmentVariables(Map<String, String> variables) {
        env.putAll(variables);
    }

    public Map<String, String> getEnvironment() {
        return env;
    }

    public void setJvmExecutable(String jvmExecutable) {
        this.jvmExecutable = jvmExecutable;
    }

    public String getJvmExecutable() {
        return jvmExecutable;
    }

    public void setWorkingDirectory(File workingDirectory) {
        this.workingDirectory = workingDirectory;
    }

    public File getWorkingDirectory() {
        return workingDirectory;
    }

    /**
     * Fully equivalent to <code>addProgramArguments(false, vmargs)</code>
     */
    public void addProgramArguments(String... args) {
        addProgramArguments(false, args);
    }

    public void addProgramArguments(boolean escape, String... args) {
        addArguments(this.args, escape, args);
    }

    private void addArguments(List<Argument> to, boolean escape, String... args) {
        for (String str : args) {
            Argument arg = new Argument();
            if (escape) {
                arg.setValue(str);
            } else {
                arg.setLine(str);
            }
            to.add(arg);
        }
    }

    public String[] getProgramArguments() {
        return toStringArray(args);
    }

    private static String[] toStringArray(List<Argument> args) {
        ArrayList<String> result = new ArrayList<String>();
        for (Argument arg : args) {
            for (String str : arg.getParts()) {
                result.add(str);
            }
        }
        return result.toArray(new String[result.size()]);
    }

    /**
     * Fully equivalent to <code>addVMArguments(false, vmargs)</code>
     */
    public void addVMArguments(String... vmargs) {
        this.addVMArguments(false, vmargs);
    }

    public void addVMArguments(boolean escape, String... vmargs) {
        addArguments(this.vmargs, escape, vmargs);
    }

    public String[] getVMArguments() {
        return toStringArray(vmargs);
    }

    public File getLauncherJar() {
        return installation.getLauncherJar();
    }

}
