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
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.eclipse.equinox.p2.engine.IPhaseSet;
import org.eclipse.tycho.ArtifactType;
import org.eclipse.tycho.DependencySeed;
import org.eclipse.tycho.TargetEnvironment;
import org.eclipse.tycho.p2.CommandLineArguments;
import org.eclipse.tycho.p2tools.copiedfromp2.PhaseSetFactory;

/**
 * Base class for calling a p2 director via command line arguments.
 */
public abstract class AbstractDirectorApplicationCommand implements DirectorRuntime.Command {

    private StringJoiner metadataSources = new StringJoiner(",");
    private StringJoiner artifactSources = new StringJoiner(",");
    private StringJoiner unitsToInstall = new StringJoiner(",");

    private String profileName;
    private TargetEnvironment environment;
    private boolean installFeatures;
    private Map<String, String> profileProperties = Map.of();
    private boolean verifyOnly;

    private File destination;
    private File bundlePool;
    private IPhaseSet phaseSet;
    private boolean installSources;

    @Override
    public final void addMetadataSources(Iterable<URI> metadataRepositories) {
        for (URI repositoryUrl : metadataRepositories) {
            this.metadataSources.add(repositoryUrl.toString());
        }
    }

    @Override
    public final void addArtifactSources(Iterable<URI> artifactRepositories) {
        for (URI repositoryUrl : artifactRepositories) {
            this.artifactSources.add(repositoryUrl.toString());
        }
    }

    @Override
    public final void addUnitToInstall(String id) {
        this.unitsToInstall.add(id);
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
        this.unitsToInstall.add(uid /* + "/" + dependency.getVersion() */);
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
    public void setInstallSources(boolean installSources) {
        this.installSources = installSources;
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

    @Override
    public void setProfileProperties(Map<String, String> profileProperties) {
        this.profileProperties = profileProperties == null ? Map.of() : profileProperties;
    }

    @Override
    public void setPhaseSet(IPhaseSet phaseSet) {
        this.phaseSet = phaseSet;
    }

    public IPhaseSet getPhaseSet() {
        if (phaseSet == null) {
            return PhaseSetFactory.createDefaultPhaseSet();
        }
        return phaseSet;
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
        Map<String, String> props = getProfileProperties();
        args.add("-profileProperties", props.entrySet().stream().map(entry -> entry.getKey() + '=' + entry.getValue())
                .collect(Collectors.joining(",")));
        args.add("-roaming");
        if (verifyOnly) {
            args.add("-verifyOnly");
        }
        if (bundlePool != null) {
            args.add("-bundlePool", bundlePool.getAbsolutePath());
        }

        if (environment != null) {
            args.add("-p2.os", environment.getOs());
            args.add("-p2.ws", environment.getWs());
            args.add("-p2.arch", environment.getArch());
        }
        return args.asList();
    }

    @Override
    public Map<String, String> getProfileProperties() {
        Map<String, String> props = new TreeMap<>(this.profileProperties);
        props.put("org.eclipse.update.install.features", Boolean.toString(installFeatures));
        if (installSources && props.get("org.eclipse.update.install.sources") == null) {
            props.put("org.eclipse.update.install.sources", "true");
        }
        return props;
    }

}
