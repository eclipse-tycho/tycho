/*******************************************************************************
 * Copyright (c) 2008, 2020 Sonatype Inc. and others.
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
package org.eclipse.tycho.packaging;

import java.io.File;

import org.codehaus.plexus.PlexusContainer;
import org.eclipse.tycho.core.PluginDescription;
import org.eclipse.tycho.core.osgitools.BundleReader;
import org.eclipse.tycho.core.osgitools.OsgiManifest;
import org.eclipse.tycho.core.shared.TargetEnvironment;
import org.eclipse.tycho.model.PluginRef;

public class ProductAssembler extends UpdateSiteAssembler {

    private final TargetEnvironment environment;

    private boolean includeSources;

    private final BundleReader manifestReader;

    public ProductAssembler(PlexusContainer plexus, BundleReader manifestReader, File target,
            TargetEnvironment environment) {
        super(plexus, target);
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
        OsgiManifest mf = manifestReader.loadManifest(plugin.getLocation(true));
        return mf.getValue("Eclipse-SourceBundle") != null;
    }

    @Override
    protected boolean isDirectoryShape(PluginDescription plugin, File location) {
        if (super.isDirectoryShape(plugin, location)) {
            return true;
        }

        OsgiManifest mf = manifestReader.loadManifest(location);

        return mf.isDirectoryShape();
    }

    protected boolean matchEntivonment(PluginDescription plugin) {
        PluginRef ref = plugin.getPluginRef();
        return ref == null || environment == null || environment.match(ref.getOs(), ref.getWs(), ref.getArch());
    }

    public void setIncludeSources(boolean includeSources) {
        this.includeSources = includeSources;
    }

}
