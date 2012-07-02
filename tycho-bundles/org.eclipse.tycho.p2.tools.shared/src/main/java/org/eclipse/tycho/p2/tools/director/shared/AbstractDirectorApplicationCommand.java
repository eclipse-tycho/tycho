/*******************************************************************************
 * Copyright (c) 2012 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.tools.director.shared;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.tycho.core.facade.TargetEnvironment;

/**
 * Base class for calling a p2 director via command line arguments.
 */
public abstract class AbstractDirectorApplicationCommand implements DirectorRuntime.Command {

    private StringList metadataSources = new StringList();
    private StringList artifactSources = new StringList();
    private StringList unitsToInstall = new StringList();

    private String profileName;
    private TargetEnvironment environment;
    private boolean installFeatures;

    private File destination;

    public final void addMetadataSources(Iterable<URI> metadataRepositories) {
        for (URI repositoryUrl : metadataRepositories) {
            this.metadataSources.append(repositoryUrl);
        }
    }

    public final void addArtifactSources(Iterable<URI> artifactRepositories) {
        for (URI repositoryUrl : artifactRepositories) {
            this.artifactSources.append(repositoryUrl);
        }
    }

    public final void addUnitToInstall(String id) {
        this.unitsToInstall.append(id);

    }

    public final void setProfileName(String profileName) {
        this.profileName = profileName;
    }

    public final void setEnvironment(TargetEnvironment env) {
        this.environment = env;
    }

    public final void setInstallFeatures(boolean installFeatures) {
        this.installFeatures = installFeatures;
    }

    public final void setDestination(File path) {
        this.destination = path;
    }

    /**
     * Returns the command line arguments for the p2 director application (not including the
     * <code>-application</code> argument).
     */
    protected String[] getDirectorApplicationArguments() {
        CommandLineArguments args = new CommandLineArguments();
        args.addUnlessEmpty("-metadataRepository", metadataSources);
        args.addUnlessEmpty("-artifactRepository", artifactSources);
        args.addUnlessEmpty("-installIU", unitsToInstall);
        args.add("-destination", destination.getAbsolutePath());
        args.add("-profile", profileName);
        args.add("-profileProperties", "org.eclipse.update.install.features=" + String.valueOf(installFeatures));
        args.add("-roaming");
        args.add("-p2.os", environment.getOs());
        args.add("-p2.ws", environment.getWs());
        args.add("-p2.arch", environment.getArch());
        return args.toArray();
    }

    private static class StringList {
        private static final String VALUE_SEPARATOR = ",";

        StringBuilder list = new StringBuilder();

        void append(Object valueToAppend) {
            list.append(VALUE_SEPARATOR);
            list.append(valueToAppend);
        }

        @Override
        public String toString() {
            if (list.length() == 0) {
                return "";
            } else {
                return list.substring(VALUE_SEPARATOR.length());
            }
        }
    }

    private static class CommandLineArguments {
        List<String> arguments = new ArrayList<String>();

        void add(String flag) {
            arguments.add(flag);
        }

        void add(String parameterName, String parameterValue) {
            arguments.add(parameterName);
            arguments.add(parameterValue);
        }

        void addUnlessEmpty(String parameterName, StringList list) {
            String parameterValue = list.toString();
            if (parameterValue.length() > 0) {
                add(parameterName, parameterValue);
            }
        }

        public String[] toArray() {
            return arguments.toArray(new String[arguments.size()]);
        }
    }
}
