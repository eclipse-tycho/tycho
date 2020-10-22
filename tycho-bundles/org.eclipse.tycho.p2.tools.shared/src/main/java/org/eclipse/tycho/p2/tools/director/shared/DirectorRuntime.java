/*******************************************************************************
 * Copyright (c) 2012, 2016 SAP SE and others.
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

import org.eclipse.tycho.core.resolver.shared.DependencySeed;
import org.eclipse.tycho.core.shared.TargetEnvironment;

/**
 * A runtime environment of the p2 director application.
 */
public interface DirectorRuntime {

    /**
     * A command for a p2 director application.
     */
    public interface Command {

        void addMetadataSources(Iterable<URI> metadataRepositories);

        void addArtifactSources(Iterable<URI> artifactRepositories);

        void addUnitToInstall(String id);

        void addUnitToInstall(DependencySeed seed);

        void setProfileName(String profileName);

        void setEnvironment(TargetEnvironment env);

        void setInstallFeatures(boolean installFeatures);

        void setDestination(File path);

        void setVerifyOnly(boolean verifyOnly);

        void execute() throws DirectorCommandException;
    }

    /**
     * Returns a new {@link Command} instance that can be used to execute a command with this
     * director runtime.
     */
    public Command newInstallCommand();
}
