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
package org.eclipse.tycho.packaging;

import java.io.File;
import java.util.jar.Manifest;

import org.apache.maven.execution.MavenSession;
import org.eclipse.tycho.core.PluginDescription;
import org.eclipse.tycho.core.TargetEnvironment;
import org.eclipse.tycho.core.osgitools.BundleReader;
import org.eclipse.tycho.model.PluginRef;

public class ProductAssembler extends UpdateSiteAssembler {

    private final TargetEnvironment environment;

    private boolean includeSources;

    private final BundleReader manifestReader;

    public ProductAssembler(MavenSession session, BundleReader manifestReader, File target,
            TargetEnvironment environment) {
        super(session, target);
        this.manifestReader = manifestReader;
        setUnpackPlugins(true);
        setUnpackFeatures(true);
        this.environment = environment;
    }

    @Override
    public void visitPlugin(PluginDescription plugin) {
        if (!matchEntivonment(plugin)) {
            return;
        }

        if (!includeSources && isSourceBundle(plugin)) {
            return;
        }

        super.visitPlugin(plugin);
    }

    private boolean isSourceBundle(PluginDescription plugin) {
        Manifest mf = manifestReader.loadManifest(plugin.getLocation());
        return manifestReader.parseHeader("Eclipse-SourceBundle", mf) != null;
    }

    @Override
    protected boolean isDirectoryShape(PluginDescription plugin, File location) {
        if (super.isDirectoryShape(plugin, location)) {
            return true;
        }

        Manifest mf = manifestReader.loadManifest(location);

        return manifestReader.isDirectoryShape(mf);
    }

    protected boolean matchEntivonment(PluginDescription plugin) {
        PluginRef ref = plugin.getPluginRef();
        return ref == null || environment == null || environment.match(ref.getOs(), ref.getWs(), ref.getArch());
    }

    public void setIncludeSources(boolean includeSources) {
        this.includeSources = includeSources;
    }

}
