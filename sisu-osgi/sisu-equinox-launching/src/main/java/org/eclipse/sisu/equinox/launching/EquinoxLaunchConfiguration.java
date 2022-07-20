/*******************************************************************************
 * Copyright (c) 2008, 2015 Sonatype Inc. and others.
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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.plexus.util.cli.Commandline.Argument;
import org.eclipse.sisu.osgi.launching.FrameworkInstallation;
import org.eclipse.sisu.osgi.launching.LaunchConfiguration;

public class EquinoxLaunchConfiguration implements LaunchConfiguration {
    private String jvmExecutable;

    private File workingDirectory;

    private final Map<String, String> env = new LinkedHashMap<>();

    private final List<Argument> args = new ArrayList<>();

    private final List<Argument> vmargs = new ArrayList<>();

    private final FrameworkInstallation installation;

    public EquinoxLaunchConfiguration(FrameworkInstallation installation) {
        this.installation = installation;
    }

    public void addEnvironmentVariables(Map<String, String> variables) {
        for (String key : variables.keySet()) {
            String value = variables.get(key);
            env.put(key, (value != null) ? value : "");
        }
    }

    @Override
    public Map<String, String> getEnvironment() {
        return env;
    }

    public void setJvmExecutable(String jvmExecutable) {
        this.jvmExecutable = jvmExecutable;
    }

    @Override
    public String getJvmExecutable() {
        return jvmExecutable;
    }

    public void setWorkingDirectory(File workingDirectory) {
        this.workingDirectory = workingDirectory;
    }

    @Override
    public File getWorkingDirectory() {
        return workingDirectory;
    }

    public void addProgramArguments(String... args) {
        addArguments(this.args, args);
    }

    private void addArguments(List<Argument> to, String... args) {
        for (String str : args) {
            Argument arg = new Argument();
            arg.setValue(str);
            to.add(arg);
        }
    }

    @Override
    public String[] getProgramArguments() {
        return toStringArray(args);
    }

    private static String[] toStringArray(List<Argument> args) {
        ArrayList<String> result = new ArrayList<>();
        for (Argument arg : args) {
            for (String str : arg.getParts()) {
                result.add(str);
            }
        }
        return result.toArray(new String[result.size()]);
    }

    public void addVMArguments(String... vmargs) {
        addArguments(this.vmargs, vmargs);
    }

    @Override
    public String[] getVMArguments() {
        return toStringArray(vmargs);
    }

    @Override
    public File getLauncherJar() {
        return installation.getLauncherJar();
    }

}
