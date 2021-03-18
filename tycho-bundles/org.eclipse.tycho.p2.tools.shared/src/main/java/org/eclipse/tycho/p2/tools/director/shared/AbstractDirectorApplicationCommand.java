/*******************************************************************************
 * Copyright (c) 2012, 2014 SAP SE and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    SAP SE - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.tools.director.shared;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.tycho.ArtifactType;
import org.eclipse.tycho.core.resolver.shared.DependencySeed;
import org.eclipse.tycho.core.shared.TargetEnvironment;

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
    private boolean verifyOnly;

    private File destination;
    private File bundlePool;

    @Override
    public final void addMetadataSources(Iterable<URI> metadataRepositories) {
        for (URI repositoryUrl : metadataRepositories) {
            this.metadataSources.append(repositoryUrl);
        }
    }

    @Override
    public final void addArtifactSources(Iterable<URI> artifactRepositories) {
        for (URI repositoryUrl : artifactRepositories) {
            this.artifactSources.append(repositoryUrl);
        }
    }

    @Override
    public final void addUnitToInstall(String id) {
        this.unitsToInstall.append(id);
    }

    @Override
    public final void addUnitToInstall(DependencySeed dependency) {
        final String uid;
        if (ArtifactType.TYPE_ECLIPSE_FEATURE.equals(dependency.getType())) {
            uid = dependency.getId() + ".feature.group";
        } else {
            uid = dependency.getId();
        }
        // format understood by VersionedId.parse(String)
        // TODO 372780 once installing from the TP, we need to explicitly pick a version here
        this.unitsToInstall.append(uid /* + "/" + dependency.getVersion() */);
    }

    @Override
    public final void setProfileName(String profileName) {
        this.profileName = profileName;
    }

    @Override
    public final void setEnvironment(TargetEnvironment env) {
        this.environment = env;
    }

    @Override
    public final void setInstallFeatures(boolean installFeatures) {
        this.installFeatures = installFeatures;
    }

    @Override
    public final void setVerifyOnly(boolean verifyOnly) {
        this.verifyOnly = verifyOnly;
    }

    @Override
    public final void setDestination(File path) {
        this.destination = path;
    }

    @Override
    public void setBundlePool(File path) {
        this.bundlePool = path;
    }

    /**
     * Returns the command line arguments for the p2 director application (not including the
     * <code>-application</code> argument).
     */
    protected List<String> getDirectorApplicationArguments() {
        CommandLineArguments args = new CommandLineArguments();
        args.addUnlessEmpty("-metadataRepository", metadataSources);
        args.addUnlessEmpty("-artifactRepository", artifactSources);
        args.addUnlessEmpty("-installIU", unitsToInstall);
        args.add("-destination", destination.getAbsolutePath());
        args.add("-profile", profileName);
        args.add("-profileProperties", "org.eclipse.update.install.features=" + String.valueOf(installFeatures));
        args.add("-roaming");
        if (verifyOnly) {
            args.add("-verifyOnly");
        }
        if (bundlePool != null) {
            args.add("-bundlePool " + bundlePool.getAbsolutePath());
        }

        if (environment != null) {
            args.add("-p2.os", environment.getOs());
            args.add("-p2.ws", environment.getWs());
            args.add("-p2.arch", environment.getArch());
        }

        return args.asList();
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
        List<String> arguments = new ArrayList<>();

        void add(String flag) {
            arguments.add(flag);
        }

        void add(String parameterName, String parameterValue) {
            arguments.add(parameterName);
            arguments.add(parameterValue);
        }

        void addUnlessEmpty(String parameterName, StringList list) {
            String parameterValue = list.toString();
            if (!parameterValue.isEmpty()) {
                add(parameterName, parameterValue);
            }
        }

        public List<String> asList() {
            return new ArrayList<>(arguments);
        }
    }
}
